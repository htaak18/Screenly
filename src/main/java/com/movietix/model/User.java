package com.movietix.model;

/**
 * Represents a registered user of the system.
 *
 * Stored in users.xml. Passwords are NEVER stored in plain text;
 * only the SHA-256 hash and the random salt are persisted.
 *
 * Role is either "USER" or "ADMIN".
 */
public class User {

    private String id;           // unique UUID or sequential ID
    private String username;
    private String email;
    private String passwordHash; // SHA-256(salt + password) as hex string
    private String salt;         // random 16-byte salt as hex string
    private String role;         // "USER" or "ADMIN"

    /** Required by XML deserialization / bean patterns. */
    public User() {}

    public User(String id, String username, String email,
                String passwordHash, String salt, String role) {
        this.id           = id;
        this.username     = username;
        this.email        = email;
        this.passwordHash = passwordHash;
        this.salt         = salt;
        this.role         = role;
    }

    // ---- Getters & Setters ----

    public String getId()           { return id; }
    public void   setId(String id)  { this.id = id; }

    public String getUsername()                { return username; }
    public void   setUsername(String username) { this.username = username; }

    public String getEmail()               { return email; }
    public void   setEmail(String email)   { this.email = email; }

    public String getPasswordHash()                      { return passwordHash; }
    public void   setPasswordHash(String passwordHash)   { this.passwordHash = passwordHash; }

    public String getSalt()              { return salt; }
    public void   setSalt(String salt)   { this.salt = salt; }

    public String getRole()              { return role; }
    public void   setRole(String role)   { this.role = role; }

    @Override
    public String toString() {
        return "User{id='" + id + "', username='" + username + "', role='" + role + "'}";
    }
}
