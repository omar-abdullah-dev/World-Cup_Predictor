package com.worldcup.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

/**
 * Mock authentication provider for local development.
 *
 * Accepts any username that ends with @QNB.COM.EG (case-insensitive)
 * as long as the password is "password" or "admin".
 *
 * This mirrors the domain restriction enforced by AuthenticationService
 * so that the full login flow behaves consistently in dev environments.
 */
@Alternative
@ApplicationScoped
public class MockAuthenticationProvider implements AuthenticationProvider {

    private static final String REQUIRED_DOMAIN = "@QNB.COM.EG";

    @Override
    public AdUserDetails authenticate(String username, String password) {
        if (username == null || password == null) return null;

        // Only accept QNB corporate accounts
        if (!username.trim().toUpperCase().endsWith(REQUIRED_DOMAIN)) {
            return null;
        }

        if ("password".equals(password) || "admin".equals(password)) {
            return new AdUserDetails(
                    username.trim(),
                    "Mock User " + username.trim(),
                    username.trim().toLowerCase(),
                    "EMP-" + Math.abs(username.hashCode() % 10000)
            );
        }
        return null;
    }
}
