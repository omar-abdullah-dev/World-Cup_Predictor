package com.worldcup.model;

/**
 * Represents the stages of the knockout tournament.
 */
public enum TournamentStage {
    GROUP_STAGE(0, "Group Stage", 0), // Kept for group stage tracking
    ROUND_OF_32(1, "Round of 32", 16),
    ROUND_OF_16(2, "Round of 16", 8),
    QUARTER_FINAL(3, "Quarter Finals", 4),
    SEMI_FINAL(4, "Semi Finals", 2),
    THIRD_PLACE(5, "Third Place Match", 1),
    FINAL(6, "Final", 1);

    private final int order;
    private final String displayName;
    private final int matchCount;

    TournamentStage(int order, String displayName, int matchCount) {
        this.order = order;
        this.displayName = displayName;
        this.matchCount = matchCount;
    }

    public int getOrder() {
        return order;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMatchCount() {
        return matchCount;
    }

    public TournamentStage nextStage() {
        switch (this) {
            case GROUP_STAGE:   return ROUND_OF_32;
            case ROUND_OF_32:   return ROUND_OF_16;
            case ROUND_OF_16:   return QUARTER_FINAL;
            case QUARTER_FINAL: return SEMI_FINAL;
            case SEMI_FINAL:    return FINAL;
            case THIRD_PLACE:
            case FINAL:         return null;
            default:            return null;
        }
    }
}
