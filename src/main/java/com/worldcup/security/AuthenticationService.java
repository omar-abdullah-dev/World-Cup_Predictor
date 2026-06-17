package com.worldcup.security;

import com.worldcup.model.User;
import com.worldcup.service.UserService;
import com.worldcup.service.WhitelistService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Service for user authentication: credential validation and session verification.
 *
 * Domain policy: Only usernames ending with @QNB.COM.EG (case-insensitive) are
 * permitted to log in.  The check is performed BEFORE the AD/Mock authentication
 * call so that an invalid domain is rejected immediately with a clear message.
 *
 * Java 8 compatible — no String.isBlank(), no var, no switch expressions.
 */
@ApplicationScoped
public class AuthenticationService {

    /** Required corporate email domain — compared case-insensitively. */
    private static final String REQUIRED_DOMAIN = "@QNB.COM.EG";

    private UserService            userService;
    private AuthenticationProvider authenticationProvider;
    private WhitelistService       whitelistService;

    protected AuthenticationService() {}

    @Inject
    public AuthenticationService(UserService userService,
                                  AuthenticationProvider authenticationProvider,
                                  WhitelistService whitelistService) {
        this.userService            = userService;
        this.authenticationProvider = authenticationProvider;
        this.whitelistService       = whitelistService;
    }

    /**
     * Authenticates a user.
     *
     * Steps:
     *  1. Basic null / empty checks.
     *  2. Enforce @QNB.COM.EG domain (case-insensitive).
     *  3. Check whitelist — user must be registered and enabled.
     *  4. Delegate to AD / Mock provider for credential verification.
     *  5. Sync AD details into the local users table.
     *
     * @param username     the username / AD login (must end with @QNB.COM.EG)
     * @param plainPassword the plain-text password
     * @return authenticated User object
     * @throws SecurityException if any check fails
     */
    public User authenticate(String username, String plainPassword) {
        if (username == null || username.trim().isEmpty()) {
            throw SecurityException.authenticationFailed("Username cannot be empty.");
        }
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw SecurityException.authenticationFailed("Password cannot be empty.");
        }

        String trimmed = username.trim();

        // ── 1. Domain enforcement ──────────────────────────────────────────
        if (!trimmed.toUpperCase().endsWith(REQUIRED_DOMAIN)) {
            throw SecurityException.authenticationFailed(
                    "Access is restricted to QNB corporate accounts (@QNB.COM.EG).");
        }

        // ── 2. Whitelist check ────────────────────────────────────────────
        if (!whitelistService.isUserWhitelisted(trimmed)) {
            throw SecurityException.authenticationFailed(
                    "Your account is not registered. Please contact the system administrator.");
        }

        // ── 3. Credential verification via AD / Mock provider ─────────────
        AdUserDetails adUser = authenticationProvider.authenticate(trimmed, plainPassword);
        if (adUser == null) {
            throw SecurityException.authenticationFailed("Invalid username or password.");
        }

        // ── 4. Sync AD info with local database ───────────────────────────
        return userService.syncAdUser(
                adUser.getAdUsername(),
                adUser.getDisplayName(),
                adUser.getEmail(),
                adUser.getEmployeeId());
    }

    /**
     * Validates that an authenticated user is still valid.
     * Called on every page request via AuthBean.isLoggedIn().
     *
     * @param userId the user ID to validate
     * @return User if found, null otherwise
     */
    public User validateActiveSession(Long userId) {
        try {
            return userService.getUser(userId);
        } catch (Exception e) {
            return null;
        }
    }
}
