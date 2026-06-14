package com.worldcup.model;

import com.worldcup.security.Role;
import jakarta.persistence.*;


import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA Entity: Represents a participant in the World Cup Predictor competition.
 *
 * Enhanced with security fields for authentication and authorization:
 * - passwordHash: PBKDF2-derived hash (nullable, AD users do not have local passwords)
 * - role: User's assigned role (NORMAL_USER or ADMIN)
 * - isApproved: /* DEPRECATED - Replaced by Whitelist - Whether the user is approved by an admin to access the system (default: false)
 * - totalPoints: Cumulative points from predictions (default: 0)
 * - createdAt: Account creation timestamp
 * - lastLogin: Last successful login timestamp
 * 
 * AD Integration Fields:
 * - adUsername: Source of truth for AD login (sAMAccountName)
 * - employeeId: Active Directory employee ID
 * - email: Email address from AD
 * - displayName: Full name from AD
 */

/**
 *
 */
@Entity
@Table(name = "users", uniqueConstraints = {@UniqueConstraint(columnNames = "username")})
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /* DEPRECATED - Replaced by Whitelist
    @Column(name = "is_approved", nullable = false)
    private boolean isApproved;
    */

    @Column(name = "total_points", nullable = false)
    private int totalPoints;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "ad_username", unique = true, length = 100)
    private String adUsername;

    @Column(name = "employee_id", length = 50)
    private String employeeId;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "display_name", length = 100)
    private String displayName;

    public User() {
        this.role = Role.NORMAL_USER;
        // this.isApproved = false; /* DEPRECATED */
        this.totalPoints = 0;
        this.createdAt = LocalDateTime.now();
    }

    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = Role.NORMAL_USER;
        // this.isApproved = false; /* DEPRECATED */
        this.totalPoints = 0;
        this.createdAt = LocalDateTime.now();
    }

    public User(Long id, String username, String passwordHash, Role role, boolean isApproved) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role != null ? role : Role.NORMAL_USER;
        // this.isApproved = isApproved; /* DEPRECATED */
        this.totalPoints = 0;
        this.createdAt = LocalDateTime.now();
    }

    public User(String username, String passwordHash, Role role, boolean isApproved) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role != null ? role : Role.NORMAL_USER;
        // this.isApproved = isApproved; /* DEPRECATED */
        this.totalPoints = 0;
        this.createdAt = LocalDateTime.now();
    }

    public User(String username, String passwordHash, Role role, boolean isApproved, int totalPoints) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role != null ? role : Role.NORMAL_USER;
        // this.isApproved = isApproved; /* DEPRECATED */
        this.totalPoints = totalPoints;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role != null ? role : Role.NORMAL_USER;
    }

    /* DEPRECATED - Replaced by Whitelist
    public boolean isApproved() {
        return isApproved;
    }

    public void setApproved(boolean approved) {
        this.isApproved = approved;
    }
    */

    public int getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(int totalPoints) {
        this.totalPoints = totalPoints;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public String getAdUsername() { return adUsername; }
    public void setAdUsername(String adUsername) { this.adUsername = adUsername; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    /**
     * Checks if user can access the system (both authenticated and approved by admin).
     */
    /* DEPRECATED - Replaced by Whitelist
    public boolean canAccessSystem() {
        return isApproved;
    }
    */

    /**
     * Checks if user has admin role.
     */
    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    /**
     * Checks if user has normal user role.
     */
    public boolean isNormalUser() {
        return role == Role.NORMAL_USER;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(id, ((User) o).id);
    }

    @Override public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', role=" + role 
            + ", totalPoints=" + totalPoints + ", adUsername='" + adUsername + "'}";
    }
}
