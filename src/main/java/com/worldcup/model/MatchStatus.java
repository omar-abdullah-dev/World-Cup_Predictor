package com.worldcup.model;

/**
 * Represents the lifecycle status of a football match.
 */
public enum MatchStatus {
    /** Match has not been played yet; predictions are accepted. */
    SCHEDULED,
    /** Match is currently being played. */
    IN_PROGRESS,
    /** Match has been played and result is recorded; no more predictions. */
    FINISHED
}
