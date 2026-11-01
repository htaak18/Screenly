package com.movietix.service;

import com.movietix.model.User;
import com.movietix.store.UserStore;
import com.movietix.util.IdGenerator;
import com.movietix.util.PasswordUtil;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Handles user registration and login/logout logic.
 *
 * Stateless – all per-request data comes from parameters; nothing is stored
 * in instance fields (servlet-safety rule).
 */
public class AuthService {

    private static final Logger LOG = Logger.getLogger(AuthService.class.getName());

    private final UserStore userStore;

    public AuthService(UserStore userStore) {
        this.userStore = userStore;
    }

    /**
     * Result of a login or register attempt.
     */
    public record AuthResult(boolean success, String message, User user) {
        public AuthResult(boolean success, String message) { this(success, message, null); }
    }

    // -------------------------------------------------------------------------

    /**
     * Registers a new user with role "USER".
     *
     * Validation:
     *  - username must be 3–20 alphanumeric characters
     *  - email must contain "@"
     *  - password must be at least 6 characters
     *  - username must not already exist
     */
    public AuthResult register(String username, String email, String password) {
        // Input validation
        if (username == null || !username.matches("[A-Za-z0-9_]{3,20}")) {
            return new AuthResult(false, "Username must be 3-20 alphanumeric/underscore characters.");
        }
        if (email == null || !email.contains("@")) {
            return new AuthResult(false, "Please enter a valid email address.");
        }
        if (password == null || password.length() < 6) {
            return new AuthResult(false, "Password must be at least 6 characters.");
        }

        // Check uniqueness
        if (userStore.findByUsername(username).isPresent()) {
            return new AuthResult(false, "Username '" + username + "' is already taken.");
        }

        // Hash password
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hash(password, salt);

        // Assign a unique ID (UUID keeps it simple for users)
        String id = "u-" + UUID.randomUUID().toString().substring(0, 8);

        User newUser = new User(id, username, email, hash, salt, "USER");
        userStore.save(newUser);

        LOG.info("Registered new user: " + username + " (id=" + id + ")");
        return new AuthResult(true, "Registration successful! Please log in.", newUser);
    }

    /**
     * Authenticates a user by username + password.
     *
     * @return AuthResult with the User object on success
     */
    public AuthResult login(String username, String password) {
        if (username == null || password == null) {
            return new AuthResult(false, "Username and password are required.");
        }

        Optional<User> maybeUser = userStore.findByUsername(username);
        if (maybeUser.isEmpty()) {
            return new AuthResult(false, "Invalid username or password.");
        }

        User user = maybeUser.get();
        if (!PasswordUtil.verify(password, user.getPasswordHash(), user.getSalt())) {
            return new AuthResult(false, "Invalid username or password.");
        }

        LOG.info("User logged in: " + username + " (role=" + user.getRole() + ")");
        return new AuthResult(true, "Login successful.", user);
    }
}
