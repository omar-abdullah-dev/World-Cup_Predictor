package com.worldcup.service;

import com.worldcup.model.Match;
import com.worldcup.model.Prediction;
import com.worldcup.model.User;
import com.worldcup.repository.PredictionRepository;
import com.worldcup.repository.UserRepository;
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

    /** Required by CDI / Weld for proxy creation. */
    protected PredictionService() {}

    @Inject
    public PredictionService(PredictionRepository predictionRepository,
                             UserRepository userRepository,
                             MatchService matchService,
                             ScoringService scoringService) {
        this.predictionRepository = predictionRepository;
        this.userRepository = userRepository;
        this.matchService = matchService;
        this.scoringService = scoringService;
    }

    /**
     * Submits and persists a new prediction after validating all business rules.
     */
    public Prediction submitPrediction(Long userId, Long matchId,
                                       int predictedHomeScore, int predictedAwayScore) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        Match match = matchService.getMatch(matchId);

        if (match.isFinished())
            throw new IllegalArgumentException(
                "Cannot predict a finished match.");

        if (!match.isPredictionOpen())
            throw new IllegalArgumentException(
                "Cannot submit or change a prediction after the match has started.");

        if (predictedHomeScore < 0 || predictedAwayScore < 0)
            throw new IllegalArgumentException("Predicted scores cannot be negative.");

        java.util.Optional<Prediction> existing = predictionRepository.findByUserAndMatch(userId, matchId);
        if (existing.isPresent()) {
            Prediction prediction = existing.get();
            prediction.setPredictedHomeScore(predictedHomeScore);
            prediction.setPredictedAwayScore(predictedAwayScore);
            return predictionRepository.save(prediction);
        }

        return predictionRepository.save(
            new Prediction(null, userId, matchId, predictedHomeScore, predictedAwayScore));
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
