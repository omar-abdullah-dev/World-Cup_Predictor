package com.worldcup.service;

import com.worldcup.model.Match;
import com.worldcup.model.MatchStatus;
import com.worldcup.model.User;
import com.worldcup.repository.MatchRepository;
import com.worldcup.security.SecurityService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for match creation, retrieval, and result recording.
 * 
 * Enhanced with RBAC enforcement:
 * - createMatch() requires ADMIN role
 * - updateResult() requires ADMIN role
 * - deleteMatch() requires ADMIN role
 * - Read operations available to all authenticated users
 *
 * CIRCULAR DEPENDENCY FIX: see PredictionService javadoc.
 */
@ApplicationScoped
public class MatchService {

    private MatchRepository matchRepository;
    private PredictionService predictionService;

    /** Required by CDI / Weld for proxy creation. */
    protected MatchService() {}

    @Inject
    public MatchService(MatchRepository matchRepository, PredictionService predictionService) {
        this.matchRepository = matchRepository;
        this.predictionService = predictionService;
    }

    /**
     * Creates a new match (ADMIN-ONLY).
     * 
     * @param adminUser the admin user creating the match
     * @param homeTeam home team name
     * @param awayTeam away team name
     * @param kickoffDate match kickoff date/time
     * @return created Match
     * @throws SecurityException if caller is not an admin
     */
    public Match createMatch(User adminUser, String homeTeam, String awayTeam, LocalDateTime kickoffDate) {
        SecurityService.assertAdmin(adminUser, "create match");
        
        if (homeTeam == null || homeTeam.trim().isEmpty())
            throw new IllegalArgumentException("Home team cannot be empty.");
        if (awayTeam == null || awayTeam.trim().isEmpty())
            throw new IllegalArgumentException("Away team cannot be empty.");
        if (kickoffDate == null)
            throw new IllegalArgumentException("Kickoff date cannot be null.");
        if (homeTeam.trim().equalsIgnoreCase(awayTeam.trim()))
            throw new IllegalArgumentException("Home team and away team cannot be the same.");

        return matchRepository.save(new Match(null, homeTeam.trim(), awayTeam.trim(), kickoffDate));
    }

    /**
     * Retrieves a match by ID (no restriction).
     * 
     * @param id the match ID
     * @return the Match
     * @throws IllegalArgumentException if match not found
     */
    public Match getMatch(Long id) {
        return matchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Match not found with ID: " + id));
    }

    /**
     * Retrieves all matches in the system (no restriction).
     * 
     * @return list of all matches
     */
    public List<Match> getAllMatches() { 
        return matchRepository.findAll(); 
    }

    /**
     * Records the result of a match (ADMIN-ONLY).
     * Marks match as FINISHED and triggers prediction recalculation (scoring + user totals).
     * 
     * @param adminUser the admin user recording the result
     * @param matchId the match ID
     * @param homeScore home team final score
     * @param awayScore away team final score
     * @return updated Match
     * @throws SecurityException if caller is not an admin
     * @throws IllegalArgumentException if business rules are violated
     */
    public Match updateResult(User adminUser, Long matchId, int homeScore, int awayScore) {
        SecurityService.assertAdmin(adminUser, "record match result");
        
        if (homeScore < 0 || awayScore < 0)
            throw new IllegalArgumentException("Scores cannot be negative.");

        Match match = getMatch(matchId);
        if (match.isFinished())
            throw new IllegalArgumentException(
                "Result already recorded for match ID: " + matchId);
        if (!match.hasStarted())
            throw new IllegalArgumentException(
                "Cannot record result before the match has started.");
        if (!match.isWithinResultWindow())
            throw new IllegalArgumentException(
                "Result can only be recorded within " + Match.RESULT_WINDOW_HOURS
                        + " hours after kickoff.");

        match.finish(homeScore, awayScore);
        matchRepository.update(match);
        predictionService.recalculateForMatch(matchId);
        return match;
    }

    /**
     * Deletes a match (ADMIN-ONLY).
     * Match cannot have started, been finished, or have existing predictions.
     * 
     * @param adminUser the admin user deleting the match
     * @param matchId the match ID
     * @throws SecurityException if caller is not an admin
     * @throws IllegalArgumentException if match cannot be deleted
     */
    public void deleteMatch(User adminUser, Long matchId) {
        SecurityService.assertAdmin(adminUser, "delete match");
        
        Match match = getMatch(matchId);
        if (match.isFinished())
            throw new IllegalArgumentException("Cannot delete a finished match.");
        if (match.hasStarted())
            throw new IllegalArgumentException("Cannot delete a match that has already started.");
        if (!predictionService.getPredictionsByMatch(matchId).isEmpty())
            throw new IllegalArgumentException("Cannot delete a match that has predictions.");

        matchRepository.deleteById(matchId);
    }

    /**
     * Retrieves all scheduled matches (no restriction).
     * 
     * @return list of scheduled matches
     */
    /**
     * Force-records a match result (ADMIN-ONLY), bypassing the time window restriction.
     * Useful for testing or correcting results outside the normal 4-hour window.
     */
    public Match forceUpdateResult(User adminUser, Long matchId, int homeScore, int awayScore) {
        SecurityService.assertAdmin(adminUser, "force record match result");

        if (homeScore < 0 || awayScore < 0)
            throw new IllegalArgumentException("Scores cannot be negative.");

        Match match = getMatch(matchId);
        if (match.isFinished())
            throw new IllegalArgumentException("Result already recorded for match ID: " + matchId);

        match.finish(homeScore, awayScore);
        matchRepository.update(match);
        predictionService.recalculateForMatch(matchId);
        return match;
    }

    public List<Match> getScheduledMatches() {
        return matchRepository.findAll().stream()
                .filter(m -> m.getStatus() == MatchStatus.SCHEDULED)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves scheduled matches that still accept predictions (before kickoff).
     * 
     * @return list of predictable matches
     */
    public List<Match> getPredictableMatches() {
        return matchRepository.findAll().stream()
                .filter(Match::isPredictionOpen)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves scheduled matches currently inside the 4-hour result recording window.
     * Used by admin dashboard to show which matches can be reported.
     * 
     * @return list of matches eligible for result recording
     */
    public List<Match> getMatchesEligibleForResult() {
        return matchRepository.findAll().stream()
                .filter(Match::canRecordResult)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all finished matches (no restriction).
     * 
     * @return list of finished matches
     */
    public List<Match> getFinishedMatches() {
        return matchRepository.findAll().stream()
                .filter(m -> m.getStatus() == MatchStatus.FINISHED)
                .collect(Collectors.toList());
    }
}
