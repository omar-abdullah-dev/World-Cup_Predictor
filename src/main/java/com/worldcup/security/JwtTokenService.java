package com.worldcup.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.enterprise.context.ApplicationScoped;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.logging.Logger;

/**
 * JWT Token Service for generating and validating JWT tokens.
 * Compatible with JJWT 0.12.x API.
 */
@ApplicationScoped
public class JwtTokenService {
    private static final Logger LOGGER = Logger.getLogger(JwtTokenService.class.getName());

    private static final String JWT_SECRET = "your-secret-key-change-this-in-production-minimum-256-bits-long!!";
    private static final long JWT_EXPIRATION_MS = 86400000L; // 24 hours

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(JWT_SECRET.getBytes());
    }

    public String generateToken(String username, String role) {
        try {
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + JWT_EXPIRATION_MS);

            return Jwts.builder()
                    .subject(username)
                    .claim("role", role)
                    .issuedAt(now)
                    .expiration(expiryDate)
                    .signWith(getSigningKey())
                    .compact();
        } catch (Exception e) {
            LOGGER.warning("Error generating JWT token: " + e.getMessage());
            throw new SecurityException("Could not generate authentication token",
                SecurityException.SecurityErrorCode.INVALID_TOKEN);
        }
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            LOGGER.warning("Invalid JWT token: " + e.getMessage());
            return false;
        }
    }

    private Claims extractClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new SecurityException("Invalid JWT token: " + e.getMessage(),
                SecurityException.SecurityErrorCode.INVALID_TOKEN);
        }
    }
}
