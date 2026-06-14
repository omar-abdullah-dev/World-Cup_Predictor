package com.worldcup.security;

public interface AuthenticationProvider {
    /**
     * Authenticates a user against an external provider (e.g. Active Directory).
     * @param username The username to authenticate
     * @param password The password to authenticate
     * @return AdUserDetails if authentication is successful, null otherwise.
     */
    AdUserDetails authenticate(String username, String password);
}
