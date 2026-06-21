package com.worldcup.model;

import com.worldcup.security.Role;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Plain Java model: Represents a participant in the World Cup Predictor competition.
 *
 * JPA/Hibernate annotations have been removed as part of the oracle-jdbc-migration.
 * All persistence is handled by {@link com.worldcup.repository.JpaUserRepository}
 * using pure JDBC / PreparedStatement.
 *
 * Field names and method signatures are unchanged — all callers remain unaffected.
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;

    /** PBKDF2-derived hash. Null for AD-authenticated users. */
    private String passwordHash;

    private Role role;

    /** Cumulative prediction points. */
    private int totalPoints;

    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;

    // ── Active Directory integration fields ───────────────────────────────
    /** sAMAccountName — source of truth for AD login. */
    private String adUsername;
    private String employeeId;
    private String email;
    private String displayName;

    // ── Constructors ──────────────────────────────────────────────────────

    public User() {
        this.role        = Role.NORMAL_USER;
        this.totalPoints = 0;
        this.createdAt   = LocalDateTime.now();
    }

    public User(String username, String passwordHash) {
        this.username     = username;
        this.passwordHash = passwordHash;
        this.role         = Role.NORMAL_USER;
        this.totalPoints  = 0;
        this.createdAt    = LocalDateTime.now();
    }

    public User(Long id, String username, String passwordHash, Role role, boolean isApproved) {
        this.id           = id;
        this.username     = username;
        this.passwordHash = passwordHash;
        this.role         = role != null ? role : Role.NORMAL_USER;
        this.totalPoints  = 0;
        this.createdAt    = LocalDateTime.now();
    }

    public User(String username, String passwordHash, Role role, boolean isApproved) {
        this.username     = username;
        this.passwordHash = passwordHash;
        this.role         = role != null ? role : Role.NORMAL_USER;
        this.totalPoints  = 0;
        this.createdAt    = LocalDateTime.now();
    }

    public User(String username, String passwordHash, Role role, boolean isApproved, int totalPoints) {
        this.username     = username;
        this.passwordHash = passwordHash;
        this.role         = role != null ? role : Role.NORMAL_USER;
        this.totalPoints  = totalPoints;
        this.createdAt    = LocalDateTime.now();
    }

    // ── Business logic ────────────────────────────────────────────────────

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isNormalUser() {
        return role == Role.NORMAL_USER;
    }

    // ── Accessors ─────────────────────────────────────────────────────────

    public Long getId()                         { return id; }
    public void setId(Long id)                  { this.id = id; }

    public String getUsername()                 { return username; }
    public void setUsername(String username)    { this.username = username; }

    public String getPasswordHash()             { return passwordHash; }
    public void setPasswordHash(String h)       { this.passwordHash = h; }

    public Role getRole()                       { return role; }
    public void setRole(Role role)              { this.role = role != null ? role : Role.NORMAL_USER; }

    public int getTotalPoints()                 { return totalPoints; }
    public void setTotalPoints(int p)           { this.totalPoints = p; }

    public LocalDateTime getCreatedAt()         { return createdAt; }
    public void setCreatedAt(LocalDateTime d)   { this.createdAt = d; }

    public LocalDateTime getLastLogin()         { return lastLogin; }
    public void setLastLogin(LocalDateTime d)   { this.lastLogin = d; }

    public String getAdUsername()               { return adUsername; }
    public void setAdUsername(String s)         { this.adUsername = s; }

    public String getEmployeeId()               { return employeeId; }
    public void setEmployeeId(String s)         { this.employeeId = s; }

    public String getEmail()                    { return email; }
    public void setEmail(String s)              { this.email = s; }

    public String getDisplayName()              { return displayName; }
    public void setDisplayName(String s)        { this.displayName = s; }

    // ── Object contract ───────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(id, ((User) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', role=" + role
                + ", totalPoints=" + totalPoints + ", adUsername='" + adUsername + "'}";
    }
}
