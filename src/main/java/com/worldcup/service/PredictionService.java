package com.worldcup.service;

import com.worldcup.model.Match;
import com.worldcup.model.Prediction;
import com.worldcup.model.User;
import com.worldcup.repository.PredictionRepository;
import com.worldcup.repository.UserRepository;
import com.worldcup.service.ActivityLogService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Business logic for prediction submission, retrieval, and score recalculation.
 *
 * CIRCULAR DEPENDENCY FIX:
 *   PredictionService <-> MatchService form a cycle.
 *   Both beans are @ApplicationScoped, so Weld resolves the cycle via client proxies.
 *   Requirements for proxy support:
 *     1. Fields must NOT be final (proxy subclass cannot set parent's final fields).
 *     2. A protected no-args constructor must exist for proxy instantiation.
 */
@ApplicationScoped
public class PredictionService {

    private PredictionRepository predictionRepository;
    private UserRepository userRepository;
    private MatchService matchService;
    private ScoringService scoringService;
    private ActivityLogService activityLogService;

    /** Required by CDI / Weld for proxy creation. */
    protected PredictionService() {}

    @Inject
    public PredictionService(PredictionRepository predictionRepository,
                             UserRepository userRepository,
                             MatchService matchService,
                             ScoringService scoringService,
                             ActivityLogService activityLogService) {
        this.predictionRepository = predictionRepository;
        this.userRepository       = userRepository;
        this.matchService         = matchService;
        this.scoringService       = scoringService;
        this.activityLogService   = activityLogService;
    }

    /**
     * Submits and persists a new prediction after validating all business rules.
     * Emits a PREDICTION_CREATED or PREDICTION_UPDATED audit record.
     *
     * @param userId              the user making the prediction
     * @param matchId             the match being predicted
     * @param predictedHomeScore  home score prediction
     * @param predictedAwayScore  away score prediction
     * @param sessionId           HTTP session ID (for audit log, may be null)
     * @param ipAddress           client IP (for audit log, may be null)
     * @param userAgent           browser UA (for audit log, may be null)
     */
    public Prediction submitPrediction(Long userId, Long matchId,
                                       int predictedHomeScore, int predictedAwayScore,
                                       String sessionId, String ipAddress, String userAgent) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        Match match = matchService.getMatch(matchId);

        if (match.isFinished())
            throw new IllegalArgumentException("Cannot predict a finished match.");

        if (match.getRound() != null && !match.getRound().isPredictionsAllowed())
            throw new IllegalArgumentException("Predictions are locked for this round.");

        if (match.getPredictionDeadline() != null
                && java.time.LocalDateTime.now().isAfter(match.getPredictionDeadline()))
            throw new IllegalArgumentException("Prediction deadline has passed for this match.");

        if (match.isPredictionLocked())
            throw new IllegalArgumentException(
                "Predictions are locked — less than "
                + com.worldcup.config.GameConstants.PREDICTION_LOCK_MINUTES
                + " minutes remain before kick-off.");

        if (predictedHomeScore < 0 || predictedAwayScore < 0)
            throw new IllegalArgumentException("Predicted scores cannot be negative.");

        java.util.Optional<Prediction> existing = predictionRepository.findByUserAndMatch(userId, matchId);
        String newValue = predictedHomeScore + "-" + predictedAwayScore;

        if (existing.isPresent()) {
            Prediction prediction = existing.get();
            String oldValue = prediction.getPredictedHomeScore() + "-" + prediction.getPredictedAwayScore();
            prediction.setPredictedHomeScore(predictedHomeScore);
            prediction.setPredictedAwayScore(predictedAwayScore);
            Prediction saved = predictionRepository.save(prediction);
            // Audit: PREDICTION_UPDATED
            activityLogService.log("PREDICTION_UPDATED",
                    "PREDICTION_UPDATED | user=" + user.getUsername()
                    + " | matchId=" + matchId
                    + " | " + match.getHomeTeam() + " vs " + match.getAwayTeam()
                    + " | old=" + oldValue + " new=" + newValue,
                    user.getUsername(),
                    userId, sessionId, ipAddress, userAgent,
                    matchId, oldValue, newValue);
            return saved;
        }

        Prediction saved = predictionRepository.save(
            new Prediction(null, userId, matchId, predictedHomeScore, predictedAwayScore));
        // Audit: PREDICTION_CREATED
        activityLogService.log("PREDICTION_CREATED",
                "PREDICTION_CREATED | user=" + user.getUsername()
                + " | matchId=" + matchId
                + " | " + match.getHomeTeam() + " vs " + match.getAwayTeam()
                + " | score=" + newValue,
                user.getUsername(),
                userId, sessionId, ipAddress, userAgent,
                matchId, null, newValue);
        return saved;
    }

    /**
     * Backward-compatible overload — callers that don't have session context.
     * Delegates to the full method with nulls for session fields.
     */
    public Prediction submitPrediction(Long userId, Long matchId,
                                       int predictedHomeScore, int predictedAwayScore) {
        return submitPrediction(userId, matchId, predictedHomeScore, predictedAwayScore,
                null, null, null);
    }

    public boolean hasPredictionForMatch(Long userId, Long matchId) {
        if (userId == null || matchId == null) {
            return false;
        }
        return predictionRepository.findByUserAndMatch(userId, matchId).isPresent();
    }

    public Prediction getPrediction(Long id) {
        return predictionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Prediction not found: " + id));
    }

    public List<Prediction> getPredictionsByUser(Long userId) {
        return predictionRepository.findAll().stream()
                .filter(p -> p.getUserId().equals(userId))
                .toList();
    }

    public List<Prediction> getPredictionsByMatch(Long matchId) {
        return predictionRepository.findByMatch(matchId);
    }

    public List<Prediction> getAllPredictions() {
        return predictionRepository.findAll();
    }

    /**
     * Re-scores every prediction for a match and updates user totals.
     * Called automatically by MatchService after a result is recorded.
     */
    public void recalculateForMatch(Long matchId) {
        Match match = matchService.getMatch(matchId);
        if (!match.isFinished()) return;

        for (Prediction prediction : predictionRepository.findByMatch(matchId)) {
            prediction.setEarnedPoints(scoringService.calculatePoints(prediction, match));
            predictionRepository.save(prediction);
            recalculateUserTotalPoints(prediction.getUserId());
        }
    }

    private void recalculateUserTotalPoints(Long userId) {
        int total = getPredictionsByUser(userId).stream()
                .mapToInt(Prediction::getEarnedPoints).sum();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.setTotalPoints(total);
        userRepository.update(user);
    }
}
