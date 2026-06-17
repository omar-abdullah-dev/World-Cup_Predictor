package com.worldcup.repository;

import com.worldcup.model.UserSession;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for UserSession — session tracking persistence.
 * Java 8 compatible — uses getResultList() instead of getResultStream().
 */
@ApplicationScoped
@Transactional
public class JpaUserSessionRepository {

    @PersistenceContext(unitName = "WorldCupPU")
    private EntityManager em;

    public UserSession save(UserSession session) {
        if (session.getId() == null) {
            em.persist(session);
            em.flush();
        } else {
            session = em.merge(session);
        }
        return session;
    }

    public Optional<UserSession> findBySessionId(String sessionId) {
        List<UserSession> results = em.createQuery(
                "SELECT s FROM UserSession s WHERE s.sessionId = :sid",
                UserSession.class)
                .setParameter("sid", sessionId)
                .getResultList();
        return results.isEmpty() ? Optional.<UserSession>empty() : Optional.of(results.get(0));
    }

    /** Returns the single ACTIVE session for a user, if any. */
    public Optional<UserSession> findActiveByUserId(Long userId) {
        List<UserSession> results = em.createQuery(
                "SELECT s FROM UserSession s WHERE s.userId = :uid AND s.status = 'ACTIVE'",
                UserSession.class)
                .setParameter("uid", userId)
                .getResultList();
        return results.isEmpty() ? Optional.<UserSession>empty() : Optional.of(results.get(0));
    }

    /** All sessions for a user, newest first. */
    public List<UserSession> findByUserId(Long userId) {
        return em.createQuery(
                "SELECT s FROM UserSession s WHERE s.userId = :uid ORDER BY s.loginTime DESC",
                UserSession.class)
                .setParameter("uid", userId)
                .getResultList();
    }

    /** All sessions across all users, newest first — for admin monitoring. */
    public List<UserSession> findAll() {
        return em.createQuery(
                "SELECT s FROM UserSession s ORDER BY s.loginTime DESC",
                UserSession.class)
                .getResultList();
    }

    /** All currently ACTIVE sessions — for admin dashboard. */
    public List<UserSession> findAllActive() {
        return em.createQuery(
                "SELECT s FROM UserSession s WHERE s.status = 'ACTIVE' ORDER BY s.loginTime DESC",
                UserSession.class)
                .getResultList();
    }
}
