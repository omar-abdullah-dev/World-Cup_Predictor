package com.worldcup.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service for secure password hashing and verification.
 * 
 * Uses PBKDF2 (Password-Based Key Derivation Function 2) with SHA-256.
 * - 65536 iterations
 * - 32-byte (256-bit) derived key
 * - 16-byte (128-bit) random salt
 * 
 * This is a simple, JDK-only implementation suitable for Jakarta EE.
 * For production, consider bcrypt (via Spring Security) or Argon2.
 * 
 * @author Security Team
 */
public class PasswordService {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 128;

    /**
     * Hashes a plain-text password using PBKDF2-SHA256 with a random salt.
     * 
     * @param plainPassword the plain-text password to hash
     * @return a Base64-encoded string in format: "iterations:salt:hash"
     * @throws SecurityException if hashing fails
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        try {
            // Generate random salt
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH / 8];
            random.nextBytes(salt);

            // Derive key using PBKDF2
            byte[] hash = deriveKey(plainPassword, salt, ITERATIONS, KEY_LENGTH);

            // Encode as: iterations:salt:hash
            String saltBase64 = Base64.getEncoder().encodeToString(salt);
            String hashBase64 = Base64.getEncoder().encodeToString(hash);

            return ITERATIONS + ":" + saltBase64 + ":" + hashBase64;
        } catch (Exception e) {
            throw new SecurityException("Password hashing failed", e);
        }
    }

    /**
     * Verifies a plain-text password against a stored hash.
     * 
     * @param plainPassword the plain-text password to verify
     * @param storedHash the stored hash (format: "iterations:salt:hash")
     * @return true if password matches stored hash, false otherwise
     * @throws SecurityException if verification fails unexpectedly
     */
    public static boolean verifyPassword(String plainPassword, String storedHash) {
        if (plainPassword == null || plainPassword.isEmpty() || storedHash == null) {
            return false;
        }

        try {
            String[] parts = storedHash.split(":");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid hash format");
            }

            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] storedDerivedKey = Base64.getDecoder().decode(parts[2]);

            // Derive key from provided password using same salt and iterations
            byte[] derivedKey = deriveKey(plainPassword, salt, iterations, storedDerivedKey.length * 8);

            // Constant-time comparison to prevent timing attacks
            return constantTimeEquals(derivedKey, storedDerivedKey);
        } catch (Exception e) {
            throw new SecurityException("Password verification failed", e);
        }
    }

    /**
     * Derives a cryptographic key from a password using PBKDF2.
     * 
     * @param password the password
     * @param salt the salt bytes
     * @param iterations number of iterations
     * @param keyLength desired key length in bits
     * @return derived key bytes
     */
    private static byte[] deriveKey(String password, byte[] salt, int iterations, int keyLength) {
        try {
            javax.crypto.spec.PBEKeySpec spec =
                new javax.crypto.spec.PBEKeySpec(
                    password.toCharArray(),
                    salt,
                    iterations,
                    keyLength
                );

            javax.crypto.SecretKeyFactory factory =
                javax.crypto.SecretKeyFactory.getInstance(ALGORITHM);

            return factory.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("Key derivation failed", e);
        }
    }

    /**
     * Constant-time byte array comparison to prevent timing attacks.
     * 
     * @param a first byte array
     * @param b second byte array
     * @return true if arrays are equal, false otherwise
     */
    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null) {
            return a == b;
        }

        int mismatch = 0;
        if (a.length != b.length) {
            mismatch = 1;
        }

        int length = Math.min(a.length, b.length);
        for (int i = 0; i < length; i++) {
            mismatch |= a[i] ^ b[i];
        }

        return mismatch == 0 && a.length == b.length;
    }
}
