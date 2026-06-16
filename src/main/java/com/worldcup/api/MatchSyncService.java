package com.worldcup.api;

import com.worldcup.api.dto.FixtureDto;
import com.worldcup.api.dto.LiveScoreDto;
import com.worldcup.api.dto.ScoresDto;
import com.worldcup.api.dto.SyncResultDto;
import com.worldcup.model.Match;
import com.worldcup.model.MatchStatus;
import com.worldcup.repository.MatchRepository;
import com.worldcup.service.ActivityLogService;
import com.worldcup.service.PredictionService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Synchronises data from WorldCupAPI into the local database.
 *
 * Rules:
 *  - If external_match_id already exists → UPDATE
 *  - If not found → INSERT (only for group-stage matches with real team names)
 *  - Never deletes existing records
 *  - When a match transitions to FINISHED → triggers prediction recalculation
 */
@ApplicationScoped
public class MatchSyncService {

    private static final Logger LOG = Logger.getLogger(MatchSyncService.class.getName());
    private static final DateTimeFormatter DATE_TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME_FMT2 =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Inject private WorldCupApiClient    apiClient;
    @Inject private MatchRepository      matchRepository;
    @Inject private PredictionService    predictionService;
    @Inject private ActivityLogService   activityLogService;

    // -----------------------------------------------------------------------
    // Full fixture sync (run at startup and every few hours)
    // -----------------------------------------------------------------------

    @Transactional
    public SyncResultDto syncAllFixtures() {
        SyncResultDto result = new SyncResultDto("FIXTURE_SYNC");
        LOG.info("[MatchSyncService] Starting full fixture sync...");
        try {
            List<FixtureDto> fixtures = apiClient.getFixtures(null);
            LOG.info("[MatchSyncService] Received " + fixtures.size() + " fixtures from API");
            for (FixtureDto f : fixtures) {
                try {
                    processFixture(f, result);
                } catch (Exception e) {
                    result.incrementErrors("[fixture " + f.getId() + "] " + e.getMessage());
                    LOG.log(Level.WARNING, "[MatchSyncService] Error processing fixture " + f.getId(), e);
                }
            }
        } catch (Exception e) {
            result.incrementErrors(e.getMessage());
            LOG.log(Level.SEVERE, "[MatchSyncService] Fixture sync failed", e);
        }
        logSync(result);
        return result;
    }

    // -----------------------------------------------------------------------
    // Live score sync (run every 5 min during tournament)
    // -----------------------------------------------------------------------

    @Transactional
    public SyncResultDto syncLiveScores() {
        SyncResultDto result = new SyncResultDto("LIVE_SCORE_SYNC");
        try {
            List<LiveScoreDto> liveScores = apiClient.getLiveScores();
            for (LiveScoreDto ls : liveScores) {
                try {
                    processLiveScore(ls, result);
                } catch (Exception e) {
                    result.incrementErrors("[live " + ls.getFixtureId() + "] " + e.getMessage());
                }
            }
        } catch (Exception e) {
            result.incrementErrors(e.getMessage());
            LOG.log(Level.WARNING, "[MatchSyncService] Live score sync failed", e);
        }
        if (result.getUpdated() > 0 || result.getErrors() > 0) logSync(result);
        return result;
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private void processFixture(FixtureDto f, SyncResultDto result) {
        // Skip TBD/placeholder matches (knockout rounds before teams are known)
        if (f.getHome() == null || f.getAway() == null) { result.incrementSkipped(); return; }
        if (isTbd(f.getHome().getName()) || isTbd(f.getAway().getName())) {
            result.incrementSkipped(); return;
        }

        Match match = matchRepository.findByExternalMatchId(f.getId()).orElse(null);
        boolean isNew = (match == null);

        if (isNew) {
            // Also try matching by home+away team name (for pre-seeded data)
            match = matchRepository.findAll().stream()
                    .filter(m -> m.getExternalMatchId() == null
                            && m.getHomeTeam().equalsIgnoreCase(f.getHome().getName())
                            && m.getAwayTeam().equalsIgnoreCase(f.getAway().getName()))
                    .findFirst().orElse(null);
            if (match == null) {
                match = new Match();
                match.setStatus(MatchStatus.SCHEDULED);
            }
        }

        // Always update these fields from API
        match.setExternalMatchId(f.getId());
        match.setHomeTeam(f.getHome().getName());
        match.setAwayTeam(f.getAway().getName());
        match.setHomeTeamApiId(f.getHome().getId());
        match.setAwayTeamApiId(f.getAway().getId());
        match.setHomeTeamLogo(f.getHome().getLogo());
        match.setAwayTeamLogo(f.getAway().getLogo());
        if (f.getLocation() != null) match.setVenue(f.getLocation());

        LocalDateTime kickoff = parseKickoff(f.getDate(), f.getTime());
        if (kickoff != null && match.getKickoffDate() == null) {
            match.setKickoffDate(kickoff);
        }

        matchRepository.save(match);
        if (isNew) result.incrementCreated(); else result.incrementUpdated();
    }

    private void processLiveScore(LiveScoreDto ls, SyncResultDto result) {
        matchRepository.findByExternalMatchId(ls.getFixtureId()).ifPresent(match -> {
            MatchStatus newStatus = mapApiStatus(ls.getStatus());
            boolean wasFinished = match.isFinished();

            match.setStatus(newStatus);

            if (newStatus == MatchStatus.FINISHED) {
                ScoresDto scores = ls.getScores();
                String ftScore = scores != null ? scores.getFtScore() : "";
                if (!ftScore.isBlank() && ftScore.contains("-")) {
                    int[] parsed = parseScoreString(ftScore);
                    if (parsed != null) {
                        match.setHomeScore(parsed[0]);
                        match.setAwayScore(parsed[1]);
                        if (match.getResultEnteredAt() == null) {
                            match.setResultEnteredAt(LocalDateTime.now());
                        }
                    }
                }
                matchRepository.update(match);

                // Trigger prediction recalculation only once when newly finished
                if (!wasFinished && match.getHomeScore() != null && match.getAwayScore() != null) {
                    LOG.info("[MatchSyncService] Match " + match.getId()
                            + " finished via API — recalculating predictions");
                    predictionService.recalculateForMatch(match.getId());
                    activityLogService.log("RES-API",
                            "RES-API | screen=scheduler | user=system | detail=matchId="
                            + match.getId() + " " + match.getHomeTeam()
                            + " " + match.getHomeScore() + "-" + match.getAwayScore()
                            + " " + match.getAwayTeam(), "system");
                }
            } else {
                matchRepository.update(match);
            }
            result.incrementUpdated();
        });
    }

    private MatchStatus mapApiStatus(String apiStatus) {
        if (apiStatus == null) return MatchStatus.SCHEDULED;
        return switch (apiStatus.toUpperCase().trim()) {
            case "IN PLAY", "HALF TIME", "EXTRA TIME", "BREAK TIME", "IN_PLAY" -> MatchStatus.SCHEDULED;
            case "FINISHED", "FT", "AET", "PEN", "FULL_TIME" -> MatchStatus.FINISHED;
            default -> MatchStatus.SCHEDULED;
        };
    }

    private int[] parseScoreString(String score) {
        try {
            String[] parts = score.split("-");
            if (parts.length == 2) {
                return new int[]{ Integer.parseInt(parts[0].trim()),
                                  Integer.parseInt(parts[1].trim()) };
            }
        } catch (NumberFormatException ignored) {}
        return null;
    }

    private LocalDateTime parseKickoff(String date, String time) {
        if (date == null) return null;
        try {
            String t = (time != null && time.length() >= 5) ? time.substring(0, 5) : "00:00";
            return LocalDateTime.parse(date + " " + t, DATE_TIME_FMT2);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private boolean isTbd(String name) {
        if (name == null) return true;
        String n = name.trim().toUpperCase();
        return n.startsWith("TBD") || n.startsWith("W") || n.startsWith("L")
                || n.matches("\\d[A-L]") || n.contains("/");
    }

    private void logSync(SyncResultDto r) {
        LOG.info("[MatchSyncService] " + r);
        activityLogService.log("SYNC", r.toString(), "system");
    }
}
