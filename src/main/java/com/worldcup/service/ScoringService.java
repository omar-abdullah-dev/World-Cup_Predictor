package com.worldcup.service;

import com.worldcup.config.GameConstants;
import com.worldcup.model.Match;
import com.worldcup.model.Prediction;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Evaluates prediction accuracy and returns earned points.
 *
 * Scoring rules for GROUP_STAGE matches (matchDecidedBy == "90" or null):
 *   2 pts – exact 90-minute score match
 *   1 pt  – correct outcome (winner / draw), wrong score
 *   0 pts – neither score nor outcome correct
 *
 * Scoring rules for KNOCKOUT matches decided by ET or PEN (matchDecidedBy == "ET" | "PEN"):
 *   The 90-minute score will always be a draw in ET/PEN scenarios.
 *   2 pts – predicted the exact 90-minute draw score
 *   1 pt  – predicted any draw at 90 minutes (correct that it would not be decided in normal time)
 *   0 pts – predicted a winner in 90 minutes
 *
 * Java 8 compatible — no switch expressions.
 */
@ApplicationScoped
public class ScoringService {

    public static final int EXACT_SCORE_POINTS     = GameConstants.POINTS_EXACT_SCORE;
    public static final int CORRECT_OUTCOME_POINTS = GameConstants.POINTS_CORRECT_OUTCOME;
    public static final int INCORRECT_POINTS       = GameConstants.POINTS_INCORRECT;

    /**
     * Calculates points for a prediction against a finished match.
     *
     * @throws IllegalArgumentException if the match is not yet finished
     */
    public int calculatePoints(Prediction prediction, Match match) {
        if (!match.isFinished()) {
            throw new IllegalArgumentException(
                    "Cannot score a match that is not finished. ID: " + match.getId());
        }

        String decidedBy = match.getMatchDecidedBy();

        if ("ET".equals(decidedBy) || "PEN".equals(decidedBy)) {
            return calculateKnockoutPoints(prediction, match);
        }

        // Default: 90-minute / group stage scoring
        if (isExactScore(prediction, match)) return EXACT_SCORE_POINTS;
        if (isCorrectOutcome(prediction, match)) return CORRECT_OUTCOME_POINTS;
        return INCORRECT_POINTS;
    }

    /**
     * Knockout scoring when the match was decided beyond 90 minutes.
     * The 90-minute score stored in homeScore/awayScore is always a draw.
     */
    private int calculateKnockoutPoints(Prediction prediction, Match match) {
        // 2 pts: predicted the exact 90-minute score (the draw)
        if (isExactScore(prediction, match)) {
            return EXACT_SCORE_POINTS;
        }
        // 1 pt: predicted any draw (correct that normal time would not settle it)
        if (prediction.getPredictedOutcome() == 0) {
            return CORRECT_OUTCOME_POINTS;
        }
        // 0 pts: predicted a winner in 90 minutes
        return INCORRECT_POINTS;
    }

    private boolean isExactScore(Prediction p, Match m) {
        return p.getPredictedHomeScore() == m.getHomeScore()
                && p.getPredictedAwayScore() == m.getAwayScore();
    }

    private boolean isCorrectOutcome(Prediction p, Match m) {
        Integer matchOutcome = m.getOutcome();
        if (matchOutcome == null) return false;
        return p.getPredictedOutcome() == matchOutcome;
    }
}
