package com.worldcup.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

/**
 * Mock authentication provider for local development.
 * Allows login with any username as long as the password is "password".
 */
@Alternative
@ApplicationScoped
public class MockAuthenticationProvider implements AuthenticationProvider {

    @Override
    public AdUserDetails authenticate(String username, String password) {
        if ("password".equals(password) || "admin".equals(password)) {
            return new AdUserDetails(
                    username,
                    "Mock User " + username,
                    username + "@company.com",
                    "EMP-" + Math.abs(username.hashCode() % 10000)
            );
        }
        return null;
    }
}
