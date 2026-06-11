package com.worldcup.security;

/**
 * Enumeration of user roles in the World Cup Predictor system.
 * Designed for easy migration to JPA database schema.
 */
public enum Role {
    /**
     * Standard predictor - can view matches, make predictions, view leaderboard.
     * All read-only and prediction submission operations.
     */
    NORMAL_USER("NORMAL_USER", "Standard prediction participant"),
    
    /**
     * System administrator - has all NORMAL_USER privileges plus administrative
     * capabilities: match creation, result recording, group management, user access control.
     */
    ADMIN("ADMIN", "System administrator with full capabilities");

    private final String code;
    private final String description;

    Role(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    @Override
    public String toString() { return code; }

    /**
     * Parse role from string (case-insensitive).
     * @param name role name (NORMAL_USER or ADMIN)
     * @return Role enum value, or NORMAL_USER if not recognized
     */
    public static Role fromString(String name) {
        if (name == null) return NORMAL_USER;
        try {
            return Role.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NORMAL_USER;
        }
    }
}
