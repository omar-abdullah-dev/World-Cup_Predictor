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
 * - passwordHash: PBKDF2-derived hash (never null, user must set password)
 * - role: User's assigned role (NORMAL_USER or ADMIN)
 * - isApproved: Whether an admin has granted this user system access
 * - createdAt: Account creation timestamp
 * - lastLogin: Last successful login timestamp
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

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "is_approved", nullable = false)
    private boolean isApproved;

    @Column(name = "total_points", nullable = false)
    private int totalPoints;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;
    public User() {
        this.role = Role.NORMAL_USER;
        this.isApproved = false;
        this.totalPoints = 0;
        this.createdAt = LocalDateTime.now();
    }

    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = Role.NORMAL_USER;
        this.isApproved = false;
        this.totalPoints = 0;
        this.createdAt = LocalDateTime.now();
    }

    public User(Long id, String username, String passwordHash, Role role, boolean isApproved) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role != null ? role : Role.NORMAL_USER;
        this.isApproved = isApproved;
        this.totalPoints = 0;
        this.createdAt = LocalDateTime.now();
    }

    public User(String username, String passwordHash, Role role, boolean isApproved) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role != null ? role : Role.NORMAL_USER;
        this.isApproved = isApproved;
        this.totalPoints = 0;
        this.createdAt = LocalDateTime.now();
    }

    public User(String username, String passwordHash, Role role, boolean isApproved, int totalPoints) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role != null ? role : Role.NORMAL_USER;
        this.isApproved = isApproved;
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

    public boolean isApproved() {
        return isApproved;
    }

    public void setApproved(boolean approved) {
        this.isApproved = approved;
    }

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

    /**
     * Checks if user can access the system (both authenticated and approved by admin).
     */
    public boolean canAccessSystem() {
        return isApproved;
    }

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
            + ", isApproved=" + isApproved + ", totalPoints=" + totalPoints + '}';
    }
}
