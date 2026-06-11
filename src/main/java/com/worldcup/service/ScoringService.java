package com.worldcup.service;

import com.worldcup.model.Match;
import com.worldcup.model.Prediction;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Evaluates prediction accuracy and returns earned points.
 *
 * Scoring rules:
 *   2 pts – exact score match
 *   1 pt  – correct outcome (winner / draw), wrong score
 *   0 pts – neither score nor outcome correct
 */
@ApplicationScoped
public class ScoringService {

    public static final int EXACT_SCORE_POINTS      = 2;
    public static final int CORRECT_OUTCOME_POINTS  = 1;
    public static final int INCORRECT_POINTS        = 0;

    /**
     * Calculates points for a prediction against a finished match.
     *
     * @throws IllegalArgumentException if the match is not yet finished
     */
    public int calculatePoints(Prediction prediction, Match match) {
        if (!match.isFinished())
            throw new IllegalArgumentException(
                "Cannot score a match that is not finished. ID: " + match.getId());

        if (isExactScore(prediction, match)) return EXACT_SCORE_POINTS;
        if (isCorrectOutcome(prediction, match)) return CORRECT_OUTCOME_POINTS;
        return INCORRECT_POINTS;
    }

    private boolean isExactScore(Prediction p, Match m) {
        return p.getPredictedHomeScore() == m.getHomeScore()
            && p.getPredictedAwayScore() == m.getAwayScore();
    }

    private boolean isCorrectOutcome(Prediction p, Match m) {
        return p.getPredictedOutcome() == m.getOutcome();
    }
}
