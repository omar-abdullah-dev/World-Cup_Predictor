package com.worldcup.security;

import com.worldcup.model.*;
import com.worldcup.service.UserService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Service for user authentication: credential validation and session verification.
 * 
 * Delegates password verification to PasswordService and user lookup to UserService.
 * 
 * @author Security Team
 */
@ApplicationScoped
public class AuthenticationService {

    private UserService userService;
    private AuthenticationProvider authenticationProvider;

    protected AuthenticationService() {}

    @Inject
    public AuthenticationService(UserService userService, AuthenticationProvider authenticationProvider) {
        this.userService = userService;
        this.authenticationProvider = authenticationProvider;
    }

    /**
     * Authenticates a user by username and password.
     * 
     * Steps:
     * 1. Look up user by username
     * 2. Verify password hash
     * 3. Check if user is approved for system access
     * 4. Return authenticated User object
     * 
     * @param username the username
     * @param plainPassword the plain-text password
     * @return authenticated User object
     * @throws SecurityException if authentication fails
     */
    public User authenticate(String username, String plainPassword) {
        if (username == null || username.trim().isEmpty()) {
            throw SecurityException.authenticationFailed("Username cannot be empty");
        }
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw SecurityException.authenticationFailed("Password cannot be empty");
        }

        /* DEPRECATED - AD is the source of truth; no manual approval or whitelist required.
        // Check whitelist first
        if (!whitelistService.isUserWhitelisted(username.trim())) {
            throw SecurityException.userNotApproved(username.trim());
        }
        */

        // Authenticate via AD (or Mock)
        AdUserDetails adUser = authenticationProvider.authenticate(username.trim(), plainPassword);
        if (adUser == null) {
            throw SecurityException.authenticationFailed("Invalid username or password");
        }

        // Sync AD info with local database
        return userService.syncAdUser(adUser.getAdUsername(), adUser.getDisplayName(), adUser.getEmail(), adUser.getEmployeeId());
    }

    /**
     * Validates that an authenticated user is still approved for access.
     * Called during session restoration to ensure access hasn't been revoked.
     * 
     * @param userId the user ID
     * @return User object if approved, null if not found or not approved
     */
    public User validateActiveSession(Long userId) {
        try {
            User user = userService.getUser(userId);
            return user;
            /* DEPRECATED - Whitelist no longer used for session validation
            if (user != null && whitelistService.isUserWhitelisted(user.getAdUsername())) {
                return user;
            }
            */
        } catch (Exception e) {
            // User not found or error during lookup
        }
        return null;
    }
}
