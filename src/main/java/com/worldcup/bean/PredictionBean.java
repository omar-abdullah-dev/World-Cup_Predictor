package com.worldcup.bean;

import com.worldcup.config.GameConstants;
import com.worldcup.model.Match;
import com.worldcup.model.Prediction;
import com.worldcup.service.ActivityLogService;
import com.worldcup.service.MatchService;
import com.worldcup.service.PredictionService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Backing bean for the Predictions page.
 *
 * Responsibilities:
 *  - Show open matches where the logged-in user can submit / update a prediction
 *    (prediction window closes {@link GameConstants#PREDICTION_LOCK_MINUTES} min before kickoff)
 *  - Show in-progress matches (locked, no editing)
 *  - Show finished matches with the result and the user's earned points
 *
 * No dev-user-selector is present; the page is always scoped to the session user.
 */
@Named
@ViewScoped
public class PredictionBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(PredictionBean.class.getName());
    private static final ZoneId EGYPT = ZoneId.of("Africa/Cairo");
    private static final DateTimeFormatter EGYPT_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy \u00b7 HH:mm");

    private String errorMessage;
    private String successMessage;

    /** Rows for matches that are still open for prediction. */
    private List<MatchPredictionRow> openRows = new ArrayList<>();

    @Inject private PredictionService predictionService;
    @Inject private MatchService matchService;
    @Inject private AuthBean authBean;
    @Inject private ActivityLogService activityLogService;

    @PostConstruct
    public void init() {
        refreshOpenRows();
    }

    // -----------------------------------------------------------------------
    // Submit prediction
    // -----------------------------------------------------------------------

    public String submitPredictionFromLoop(MatchPredictionRow row) {
        errorMessage = null;
        successMessage = null;

        if (!authBean.isLoggedIn()) {
            errorMessage = "You must be logged in to submit a prediction.";
            return null;
        }
        if (row == null || row.getMatchId() == null) {
            errorMessage = "Match not found.";
            return null;
        }
        if (row.getHomeScore() == null || row.getAwayScore() == null) {
            errorMessage = "Please enter both scores before submitting.";
            return null;
        }

        Long userId = authBean.getCurrentUserId();
        try {
            boolean updating = predictionService.hasPredictionForMatch(userId, row.getMatchId());

            // Collect session context for audit
            String sessionId = authBean.getHttpSessionId();
            String ipAddress = null;
            String userAgent = null;
            try {
                jakarta.servlet.http.HttpServletRequest req =
                    (jakarta.servlet.http.HttpServletRequest)
                    jakarta.faces.context.FacesContext.getCurrentInstance()
                        .getExternalContext().getRequest();
                String xff = req.getHeader("X-Forwarded-For");
                ipAddress = (xff != null && !xff.trim().isEmpty()) ? xff.split(",")[0].trim() : req.getRemoteAddr();
                userAgent = req.getHeader("User-Agent");
            } catch (Exception ignored) {}

            predictionService.submitPrediction(userId, row.getMatchId(),
                    row.getHomeScore(), row.getAwayScore(),
                    sessionId, ipAddress, userAgent);

            Match match = matchService.getMatch(row.getMatchId());
            String action = updating ? "Updated" : "Saved";
            successMessage = action + " prediction: "
                    + match.getHomeTeam() + " "
                    + row.getHomeScore() + " \u2013 " + row.getAwayScore()
                    + " " + match.getAwayTeam();
            refreshOpenRows();
        } catch (IllegalArgumentException e) {
            errorMessage = e.getMessage();
        } catch (Exception e) {
            LOG.warning("[PredictionBean] Unexpected error: " + e.getMessage());
            errorMessage = "An unexpected error occurred. Please try again.";
        }
        return null;
    }

    // -----------------------------------------------------------------------
    // Data queries used by the view
    // -----------------------------------------------------------------------

    public List<MatchPredictionRow> getUpcomingRows() {
        return openRows;
    }

    /** Matches that have started but no result yet (locked for prediction). */
    public List<Match> getStartedMatches() {
        return deduplicateById(
            matchService.getAllMatches().stream()
                .filter(m -> m.hasStarted() && !m.isFinished())
                .collect(java.util.stream.Collectors.toList()));
    }

    /** Finished matches — result + points. */
    public List<Match> getFinishedMatches() {
        return deduplicateById(matchService.getFinishedMatches());
    }

    public boolean hasPredictionForMatch(Long matchId) {
        Long uid = currentUserId();
        return uid != null && predictionService.hasPredictionForMatch(uid, matchId);
    }

    public Prediction getExistingPrediction(Long matchId) {
        Long uid = currentUserId();
        if (uid == null || matchId == null) return null;
        return predictionService.getAllPredictions().stream()
                .filter(p -> p.getUserId().equals(uid) && p.getMatchId().equals(matchId))
                .findFirst()
                .orElse(null);
    }

    public String getPointsBadgeClass(int earnedPoints) {
        if (earnedPoints == 2) return "points-exact";
        if (earnedPoints == 1) return "points-outcome";
        return "points-zero";
    }

    // -----------------------------------------------------------------------
    // Formatting helpers
    // -----------------------------------------------------------------------

    public String formatPredictionKickoff(LocalDateTime kickoff) {
        if (kickoff == null) return "";
        ZonedDateTime egypt = kickoff.atZone(ZoneId.of("UTC")).withZoneSameInstant(EGYPT);
        return egypt.format(EGYPT_FMT) + " (Cairo)";
    }

    /** Minutes remaining before the prediction window closes, or 0 if already locked. */
    public long minutesUntilLock(Match match) {
        if (match == null || match.getKickoffDate() == null) return 0;
        LocalDateTime lockTime = match.getKickoffDate()
                .minusMinutes(GameConstants.PREDICTION_LOCK_MINUTES);
        long mins = java.time.Duration.between(LocalDateTime.now(), lockTime).toMinutes();
        return Math.max(mins, 0);
    }

    public String getTeamInitials(String teamName) {
        if (teamName == null || teamName.trim().isEmpty()) return "??";
        String[] parts = teamName.trim().split("\\s+");
        if (parts.length >= 2)
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        String n = teamName.trim();
        return n.length() >= 2 ? n.substring(0, 2).toUpperCase() : n.toUpperCase();
    }

    public String getFlagForTeam(String teamName) {
        if (teamName == null) return "\uD83C\uDFF3";
        String t = teamName.trim().toLowerCase();
        if ("brazil".equals(t))        return "\uD83C\uDDE7\uD83C\uDDF7";
        if ("argentina".equals(t))     return "\uD83C\uDDE6\uD83C\uDDF7";
        if ("france".equals(t))        return "\uD83C\uDDEB\uD83C\uDDF7";
        if ("germany".equals(t))       return "\uD83C\uDDE9\uD83C\uDDEA";
        if ("spain".equals(t))         return "\uD83C\uDDEA\uD83C\uDDF8";
        if ("portugal".equals(t))      return "\uD83C\uDDF5\uD83C\uDDF9";
        if ("netherlands".equals(t))   return "\uD83C\uDDF3\uD83C\uDDF1";
        if ("italy".equals(t))         return "\uD83C\uDDEE\uD83C\uDDF9";
        if ("croatia".equals(t))       return "\uD83C\uDDED\uD83C\uDDF7";
        if ("morocco".equals(t))       return "\uD83C\uDDF2\uD83C\uDDE6";
        if ("senegal".equals(t))       return "\uD83C\uDDF8\uD83C\uDDF3";
        if ("qatar".equals(t))         return "\uD83C\uDDF6\uD83C\uDDE6";
        if ("ecuador".equals(t))       return "\uD83C\uDDEA\uD83C\uDDE8";
        if ("usa".equals(t) || "united states".equals(t)) return "\uD83C\uDDFA\uD83C\uDDF8";
        if ("japan".equals(t))         return "\uD83C\uDDEF\uD83C\uDDF5";
        if ("south korea".equals(t))   return "\uD83C\uDDF0\uD83C\uDDF7";
        if ("australia".equals(t))     return "\uD83C\uDDE6\uD83C\uDDFA";
        if ("mexico".equals(t))        return "\uD83C\uDDF2\uD83C\uDDFD";
        if ("nigeria".equals(t))       return "\uD83C\uDDF3\uD83C\uDDEC";
        if ("cameroon".equals(t))      return "\uD83C\uDDE8\uD83C\uDDF2";
        if ("egypt".equals(t))         return "\uD83C\uDDEA\uD83C\uDDEC";
        if ("saudi arabia".equals(t))  return "\uD83C\uDDF8\uD83C\uDDE6";
        if ("ghana".equals(t))         return "\uD83C\uDDEC\uD83C\uDDED";
        if ("switzerland".equals(t))   return "\uD83C\uDDE8\uD83C\uDDED";
        if ("belgium".equals(t))       return "\uD83C\uDDE7\uD83C\uDDEA";
        if ("poland".equals(t))        return "\uD83C\uDDF5\uD83C\uDDF1";
        if ("uruguay".equals(t))       return "\uD83C\uDDFA\uD83C\uDDFE";
        if ("canada".equals(t))        return "\uD83C\uDDE8\uD83C\uDDE6";
        if ("iran".equals(t))          return "\uD83C\uDDEE\uD83C\uDDF7";
        return "\uD83C\uDFF3";
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    public String getErrorMessage()   { return errorMessage; }
    public void setErrorMessage(String m) { this.errorMessage = m; }
    public String getSuccessMessage() { return successMessage; }
    public void setSuccessMessage(String m) { this.successMessage = m; }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private Long currentUserId() {
        return (authBean != null && authBean.isLoggedIn()) ? authBean.getCurrentUserId() : null;
    }

    private void refreshOpenRows() {
        openRows = new ArrayList<>();
        Long uid = currentUserId();
        Set<Long> seen = new LinkedHashSet<>();
        for (Match match : matchService.getPredictableMatches()) {
            if (!seen.add(match.getId())) continue;  // deduplicate
            MatchPredictionRow row = new MatchPredictionRow(match);
            if (uid != null) {
                Prediction existing = predictionService.getAllPredictions().stream()
                        .filter(p -> p.getUserId().equals(uid) && p.getMatchId().equals(match.getId()))
                        .findFirst().orElse(null);
                if (existing != null) {
                    row.setHomeScore(existing.getPredictedHomeScore());
                    row.setAwayScore(existing.getPredictedAwayScore());
                }
            }
            openRows.add(row);
        }
    }

    private static List<Match> deduplicateById(List<Match> matches) {
        Set<Long> seen = new LinkedHashSet<>();
        List<Match> result = new ArrayList<>();
        for (Match m : matches) {
            if (seen.add(m.getId())) result.add(m);
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // Inner row class
    // -----------------------------------------------------------------------

    public static class MatchPredictionRow implements Serializable {
        private static final long serialVersionUID = 1L;

        private final Match match;
        private Integer homeScore;
        private Integer awayScore;

        public MatchPredictionRow(Match match) { this.match = match; }

        public Long getMatchId()   { return match.getId(); }
        public Match getMatch()    { return match; }

        public Integer getHomeScore() { return homeScore; }
        public void setHomeScore(Integer s) { this.homeScore = s; }

        public Integer getAwayScore() { return awayScore; }
        public void setAwayScore(Integer s) { this.awayScore = s; }
    }
}
