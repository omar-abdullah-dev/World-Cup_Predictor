package com.worldcup.config;

/**
 * Central place for all tunable game-rule constants.
 *
 * Change a value here and the entire application picks it up on next deploy.
 */
public final class GameConstants {

    private GameConstants() {}

    // -----------------------------------------------------------------------
    // Prediction window
    // -----------------------------------------------------------------------

    /**
     * How many minutes before kickoff the prediction window closes.
     * A user can update their prediction any number of times until this
     * many minutes remain before the match starts.
     *
     * Default: 5 minutes.
     */
    public static final int PREDICTION_LOCK_MINUTES = 5;

    // -----------------------------------------------------------------------
    // Result editing window
    // -----------------------------------------------------------------------

    /**
     * How many hours after a result is first entered that the admin may
     * still edit it. After this window the result is permanently locked.
     *
     * Default: 4 hours  (mirrors Match.RESULT_WINDOW_HOURS).
     */
    public static final int RESULT_EDIT_WINDOW_HOURS = 4;

    // -----------------------------------------------------------------------
    // Scoring
    // -----------------------------------------------------------------------

    /** Points awarded for predicting the exact final score. */
    public static final int POINTS_EXACT_SCORE   = 2;

    /** Points awarded for predicting the correct outcome (win/draw/loss) but wrong score. */
    public static final int POINTS_CORRECT_OUTCOME = 1;

    /** Points awarded when neither score nor outcome is correct. */
    public static final int POINTS_INCORRECT     = 0;
}
