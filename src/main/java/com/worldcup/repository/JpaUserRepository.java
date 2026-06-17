package com.worldcup.repository;

import com.worldcup.model.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * JPA/Hibernate implementation of UserRepository using PostgreSQL database.
 *
 * Replaces in-memory storage with persistent database operations.
 * All operations are transactional and thread-safe.
 */
@ApplicationScoped
@Transactional
public class JpaUserRepository implements UserRepository {

    @PersistenceContext(unitName = "WorldCupPU")
    private EntityManager em;

    @Override
    public User save(User user) {
        em.persist(user);
        em.flush();
        return user;
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(em.find(User.class, id));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        System.out.println("QUERY USERNAME = [" + username + "]");
        List<User> results = em.createQuery(
                "SELECT u FROM User u WHERE u.username = :username",
                User.class)
                .setParameter("username", username)
                .getResultList();
        return results.isEmpty() ? Optional.<User>empty() : Optional.of(results.get(0));
    }

    @Override
    public Optional<User> findByAdUsername(String adUsername) {
        List<User> results = em.createQuery(
                "SELECT u FROM User u WHERE u.adUsername = :adUsername",
                User.class)
                .setParameter("adUsername", adUsername)
                .getResultList();
        return results.isEmpty() ? Optional.<User>empty() : Optional.of(results.get(0));
    }

    @Override
    public List<User> findAll() {
        return em.createQuery("SELECT u FROM User u ORDER BY u.createdAt DESC", User.class)
                .getResultList();
    }

    @Override
    public User update(User user) {
        return em.merge(user);
    }

    /**
     * Find all approved users
     */
    /* DEPRECATED - Replaced by Whitelist
    public List<User> findAllApproved() {
        return em.createQuery("SELECT u FROM User u WHERE u.isApproved = true ORDER BY u.username", User.class)
                .getResultList();
    }
    */

    /**
     * Find all admin users
     */
    public List<User> findAllAdmins() {
        return em.createQuery("SELECT u FROM User u WHERE u.role = 'ADMIN' ORDER BY u.username", User.class)
                .getResultList();
    }

    /**
     * Find all pending approval users
     */
    /* DEPRECATED - Replaced by Whitelist
    public List<User> findPendingApproval() {
        return em.createQuery("SELECT u FROM User u WHERE u.isApproved = false ORDER BY u.createdAt", User.class)
                .getResultList();
    }
    */

    /**
     * Delete a user by ID
     */
    @Transactional
    public boolean deleteById(Long id) {
        User user = em.find(User.class, id);
        if (user == null) {
            return false;
        }
        em.remove(user);
        return true;
    }

    /**
     * Count total users
     */
    public long count() {
        return em.createQuery("SELECT COUNT(u) FROM User u", Long.class)
                .getSingleResult();
    }

    /**
     * Check if username exists
     */
    public boolean existsByUsername(String username) {
        return em.createQuery("SELECT COUNT(u) FROM User u WHERE u.username = :username", Long.class)
                .setParameter("username", username)
                .getSingleResult() > 0;
    }
}
