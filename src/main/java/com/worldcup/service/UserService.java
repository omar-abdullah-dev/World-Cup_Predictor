package com.worldcup.service;

import com.worldcup.model.User;
import com.worldcup.repository.UserRepository;
import com.worldcup.security.PasswordService;
import com.worldcup.security.Role;
import com.worldcup.security.SecurityService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for user management, authentication, and leaderboard generation.
 *
 * Enhanced with security operations:
 * - User registration with hashed passwords
 * - User approval/access management (admin-only)
 * - Role assignment (admin-only)
 * - User retrieval by username (for authentication)
 *
 * FIX: field is non-final and a protected no-args constructor is present so
 * Weld can create the CDI client proxy for this @ApplicationScoped bean.
 */
@ApplicationScoped
public class UserService {

    // Non-final so the CDI proxy subclass can be instantiated via no-args ctor
    private UserRepository userRepository;

    /** Required by CDI / Weld for proxy creation. */
    protected UserService() {}

    @Inject
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * LEGACY: Creates a new user with username only (no password, not approved).
     * Used only for seeding sample data. New registrations should use registerUser().
     * 
     * @param username the username
     * @return created User (not approved, no password)
     * @throws IllegalArgumentException if username is taken
     */
    public User createUser(String username) {
        if (username == null || username.trim().isEmpty())
            throw new IllegalArgumentException("Username cannot be null or empty.");

        if (userRepository.findByUsername(username.trim()).isPresent()) {
            throw new IllegalArgumentException(
                    "Username already exists: " + username);
        }
        // Create user with placeholder password (user must reset)
        String placeholderHash = PasswordService.hashPassword("PLACEHOLDER_" + System.currentTimeMillis());
        User user = new User(null, username.trim(), placeholderHash, Role.NORMAL_USER, false);
        return userRepository.save(user);
    }

    /**
     * Registers a new user with username and password (for login).
     * New users are created in UNAPPROVED state (admin must grant access).
     * 
     * @param username the desired username
     * @param plainPassword the plain-text password
     * @return created User (not approved, password hashed)
     * @throws IllegalArgumentException if username is taken or invalid
     */
    public User registerUser(String username, String plainPassword) {
        if (username == null || username.trim().isEmpty())
            throw new IllegalArgumentException("Username cannot be null or empty.");
        if (plainPassword == null || plainPassword.isEmpty())
            throw new IllegalArgumentException("Password cannot be empty.");

        if (userRepository.findByUsername(username.trim()).isPresent()) {
            throw new IllegalArgumentException(
                    "Username already exists: " + username);
        }
        String passwordHash = PasswordService.hashPassword(plainPassword);
        User user = new User(null, username.trim(), passwordHash, Role.NORMAL_USER, false);
        return userRepository.save(user);
    }

    /**
     * Finds a user by username (for authentication).
     * 
     * @param username the username to search for
     * @return the User if found
     * @throws IllegalArgumentException if user not found
     */


    /**
     * Retrieves a user by ID.
     * 
     * @param id the user ID
     * @return the User
     * @throws IllegalArgumentException if user not found
     */
    public User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));
    }

    public User findByUsername(String username) {

        System.out.println("REPOSITORY = " + userRepository.getClass().getName());

        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found with username: " + username));
    }

    /**
     * Retrieves all users in the system (admin-only operation).
     * 
     * @return list of all users
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Updates a user's total points (called by scoring engine).
     * 
     * @param userId the user ID
     * @param totalPoints new total points value
     * @return updated User
     */
    public User updatePoints(Long userId, int totalPoints) {
        User user = getUser(userId);
        user.setTotalPoints(totalPoints);
        return userRepository.update(user);
    }

    /**
     * Returns users sorted by total points desc; username asc as tie-breaker.
     * Only includes APPROVED users (who can actually earn points).
     * 
     * @return sorted leaderboard of approved users
     */
    public List<User> getLeaderboard() {
        return userRepository.findAll().stream()
                .filter(User::isApproved)  // Only approved users
                .sorted(Comparator.comparingInt(User::getTotalPoints).reversed()
                        .thenComparing(User::getUsername))
                .collect(Collectors.toList());
    }

    /**
     * Approves a user for system access (ADMIN-ONLY).
     * User must be approved before they can log in or access any features.
     * 
     * @param adminUser the admin performing approval
     * @param userIdToApprove the user ID to approve
     * @return approved User
     * @throws SecurityException if caller is not an admin
     */
    public User approveUser(User adminUser, Long userIdToApprove) {
        SecurityService.assertAdmin(adminUser, "approve user");
        
        User user = getUser(userIdToApprove);
        user.setApproved(true);
        return userRepository.update(user);
    }

    /**
     * Denies/revokes system access for a user (ADMIN-ONLY).
     * 
     * @param adminUser the admin performing denial
     * @param userIdToDeny the user ID to deny access
     * @return updated User (not approved)
     * @throws SecurityException if caller is not an admin
     */
    public User denyUser(User adminUser, Long userIdToDeny) {
        SecurityService.assertAdmin(adminUser, "deny user access");
        
        User user = getUser(userIdToDeny);
        user.setApproved(false);
        return userRepository.update(user);
    }

    /**
     * Assigns an admin role to a user (ADMIN-ONLY).
     * 
     * @param adminUser the admin performing the assignment
     * @param userIdToPromote the user ID to promote to admin
     * @return updated User with ADMIN role
     * @throws SecurityException if caller is not an admin
     */
    public User promoteToAdmin(User adminUser, Long userIdToPromote) {
        SecurityService.assertAdmin(adminUser, "promote user to admin");
        
        User user = getUser(userIdToPromote);
        user.setRole(Role.ADMIN);
        return userRepository.update(user);
    }

    /**
     * Demotes an admin user to normal user role (ADMIN-ONLY).
     * 
     * @param adminUser the admin performing the demotion
     * @param userIdToDemote the user ID to demote
     * @return updated User with NORMAL_USER role
     * @throws SecurityException if caller is not an admin
     */
    public User demoteFromAdmin(User adminUser, Long userIdToDemote) {
        SecurityService.assertAdmin(adminUser, "demote admin user");
        
        User user = getUser(userIdToDemote);
        user.setRole(Role.NORMAL_USER);
        return userRepository.update(user);
    }

    /**
     * Retrieves all unapproved users pending admin review (ADMIN-ONLY).
     * 
     * @param adminUser the admin performing the query
     * @return list of unapproved users
     * @throws SecurityException if caller is not an admin
     */
    public List<User> getUnapprovedUsers(User adminUser) {
        SecurityService.assertAdmin(adminUser, "view unapproved users");
        
        return userRepository.findAll().stream()
                .filter(u -> !u.isApproved())
                .collect(Collectors.toList());
    }

    /**
     * Changes a user's password (user themselves, or admin).
     * 
     * @param requestingUser the user requesting the change (self or admin)
     * @param targetUserId the user whose password to change
     * @param newPlainPassword the new plain-text password
     * @return updated User
     * @throws SecurityException if requestingUser is neither the target user nor an admin
     */
    /**
     * Directly updates a user entity (used by DataInitializer for bootstrapping admin).
     * Bypasses role/approval checks.
     *
     * @param user the user to update
     * @return updated User
     */
    public User updateUser(User user) {
        return userRepository.update(user);
    }

    /**
     * Directly saves a user entity (used for bootstrap or admin operations).
     *
     * @param user the user to save
     * @return saved User
     */
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public User changePassword(User requestingUser, Long targetUserId, String newPlainPassword) {
        if (newPlainPassword == null || newPlainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        
        // Allow self-change or admin-change
        if (!requestingUser.getId().equals(targetUserId)) {
            SecurityService.assertAdmin(requestingUser, "change another user's password");
        }
        
        User user = getUser(targetUserId);
        String newHash = PasswordService.hashPassword(newPlainPassword);
        user.setPasswordHash(newHash);
        return userRepository.update(user);
    }
}
