package com.movietix.util;

import java.security.SecureRandom;
import java.util.HexFormat;

import jakarta.servlet.http.HttpSession;

/**
 * Utility for generating and validating CSRF tokens.
 *
 * A CSRF (Cross-Site Request Forgery) token is a unique, unpredictable value
 * tied to the user's session.  Every form that mutates state (hold, pay,
 * cancel) includes a hidden _csrf field.  The server rejects requests where
 * the submitted token does not match the session token.
 *
 * ATTACK PREVENTED: without this, an attacker's website could submit a form
 * on behalf of a logged-in user (e.g. cancel their bookings).
 */
public final class CsrfUtil {

    private static final String SESSION_KEY  = "csrfToken";
    private static final SecureRandom RNG    = new SecureRandom();

    private CsrfUtil() {}

    // -------------------------------------------------------------------------

    /**
     * Returns the existing CSRF token from the session, or generates and stores
     * a new one if none exists yet.
     */
    public static String getOrCreateToken(HttpSession session) {
        String token = (String) session.getAttribute(SESSION_KEY);
        if (token == null) {
            byte[] bytes = new byte[24];
            RNG.nextBytes(bytes);
            token = HexFormat.of().formatHex(bytes);
            session.setAttribute(SESSION_KEY, token);
        }
        return token;
    }

    /**
     * Validates the token submitted by the client against the session token.
     *
     * @param session       the current HTTP session
     * @param submittedToken the _csrf field value from the request
     * @return true if valid
     */
    public static boolean isValid(HttpSession session, String submittedToken) {
        if (session == null || submittedToken == null) return false;
        String stored = (String) session.getAttribute(SESSION_KEY);
        if (stored == null) return false;
        // Constant-time comparison to prevent timing attacks
        return java.security.MessageDigest.isEqual(
            stored.getBytes(),
            submittedToken.getBytes()
        );
    }
}
