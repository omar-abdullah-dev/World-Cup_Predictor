package com.worldcup.security;

import com.worldcup.model.User;

/**
 * Service for security-related checks: role validation, permission verification.
 * 
 * This is the central point for authorization logic. All role-based access checks
 * should be delegated to this service.
 * 
 * @author Security Team
 */
public class SecurityService {

    /**
     * Asserts that a user has the ADMIN role. Throws SecurityException if not.
     * 
     * @param user the user to check (must not be null)
     * @param operation description of operation being guarded (e.g., "create match")
     * @throws SecurityException if user is not an admin
     */
    public static void assertAdmin(User user, String operation) {
        if (user == null) {
            throw SecurityException.notAuthenticated();
        }
        if (!user.isAdmin()) {
            throw SecurityException.insufficientPermission(operation, Role.ADMIN);
        }
    }

    /**
     * Asserts that a user is approved for system access. Throws SecurityException if not.
     * 
     * @param user the user to check (must not be null)
     * @throws SecurityException if user is not approved
     */
    public static void assertApproved(User user) {
        if (user == null) {
            throw SecurityException.notAuthenticated();
        }
        if (!user.isApproved()) {
            throw SecurityException.userNotApproved(user.getUsername());
        }
    }

    /**
     * Asserts that a user is authenticated and approved. Throws SecurityException if not.
     * 
     * @param user the user to check
     * @throws SecurityException if user is null or not approved
     */
    public static void assertAuthenticated(User user) {
        if (user == null) {
            throw SecurityException.notAuthenticated();
        }
        assertApproved(user);
    }

    /**
     * Checks if user has admin role.
     * 
     * @param user the user to check (can be null)
     * @return true if user is not null and is admin, false otherwise
     */
    public static boolean isAdmin(User user) {
        return user != null && user.isAdmin();
    }

    /**
     * Checks if user is authenticated and approved.
     * 
     * @param user the user to check (can be null)
     * @return true if user is not null and approved, false otherwise
     */
    public static boolean isAuthenticated(User user) {
        return user != null && user.isApproved();
    }

    /**
     * Checks if user has sufficient permission for an operation.
     * Returns true if user is authenticated; additionally for admin operations, checks role.
     * 
     * @param user the user to check
     * @param requireAdmin true if operation requires admin role
     * @return true if user can perform operation, false otherwise
     */
    public static boolean hasPermission(User user, boolean requireAdmin) {
        if (user == null || !user.isApproved()) {
            return false;
        }
        if (requireAdmin) {
            return user.isAdmin();
        }
        return true;
    }
}
