package com.movietix.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Utility class for password hashing and verification.
 *
 * Algorithm: SHA-256(salt + plainText)
 *   1. Generate a random 16-byte salt per user (stored alongside the hash).
 *   2. Compute SHA-256 of (salt_hex + plaintext_password).
 *   3. Store salt and hash as hex strings in users.xml.
 *
 * This is NOT bcrypt/Argon2, but is sufficient for a university project.
 * A comment below explains what a production system would use.
 */
public final class PasswordUtil {

    // SecureRandom is thread-safe; a single shared instance is fine.
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** Prevent instantiation – all methods are static. */
    private PasswordUtil() {}

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Generates a random 16-byte salt encoded as a 32-character hex string.
     * A unique salt per user ensures two users with the same password get
     * different hashes, preventing rainbow-table attacks.
     */
    public static String generateSalt() {
        byte[] salt = new byte[16];
        SECURE_RANDOM.nextBytes(salt);
        return HexFormat.of().formatHex(salt);
    }

    /**
     * Hashes a plain-text password with the given salt.
     *
     * @param plainPassword  the raw password typed by the user
     * @param salt           hex-encoded salt returned by {@link #generateSalt()}
     * @return 64-character hex SHA-256 hash
     */
    public static String hash(String plainPassword, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // Concatenate salt + password, then hash
            String combined = salt + plainPassword;
            byte[] hashBytes = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed by the JVM spec – this should never happen.
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Verifies a plain-text password against a stored hash+salt pair.
     *
     * @param plainPassword  the password the user just typed
     * @param storedHash     the hash read from users.xml
     * @param salt           the salt read from users.xml
     * @return true if the password is correct
     */
    public static boolean verify(String plainPassword, String storedHash, String salt) {
        String computed = hash(plainPassword, salt);
        // MessageDigest.isEqual does a constant-time compare to avoid timing attacks.
        return MessageDigest.isEqual(
            computed.getBytes(StandardCharsets.UTF_8),
            storedHash.getBytes(StandardCharsets.UTF_8)
        );
    }

    /*
     * PRODUCTION NOTE (for viva):
     * SHA-256 with a single salt is not the ideal choice for storing passwords
     * in production because modern GPUs can compute billions of SHA-256 hashes
     * per second.  A production system should use a memory-hard algorithm such
     * as bcrypt (cost factor ≥ 12), Argon2id, or scrypt.  These algorithms are
     * deliberately slow so that brute-force attacks take much longer.
     * For this university project SHA-256 + salt is sufficient to demonstrate
     * the concept of salted hashing.
     */
}
