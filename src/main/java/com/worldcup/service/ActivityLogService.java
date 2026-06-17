package com.worldcup.service;

import com.worldcup.model.SystemActivityLog;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persists system activity entries to the system_activity_log table.
 *
 * Each entry captures:
 *   opmaj      - the operation type  (e.g. LOGIN, LOGOUT, RESULT_SAVED, MATCH_DELETED)
 *   datemaj    - timestamp (set automatically in SystemActivityLog constructor)
 *   transmaj   - free-text detail about what happened
 *   profilemaj - username of the actor
 *
 * Call log() from any bean/service after a meaningful user action.
 * The method never throws — a DB failure is swallowed so it never
 * breaks the main business flow.
 */
@ApplicationScoped
public class ActivityLogService {

    private static final Logger LOG = Logger.getLogger(ActivityLogService.class.getName());

    @PersistenceContext(unitName = "WorldCupPU")
    private EntityManager em;

    /**
     * Persists one activity log entry (original simple signature — backward compatible).
     *
     * @param opmaj      operation name   (e.g. "LOGIN")
     * @param transmaj   detail text      (e.g. "User logged in from 192.168.1.1")
     * @param profilemaj actor's username (e.g. "admin")
     */
    @Transactional
    public void log(String opmaj, String transmaj, String profilemaj) {
        try {
            SystemActivityLog entry = new SystemActivityLog(opmaj, transmaj, profilemaj);
            em.persist(entry);
            em.flush();
        } catch (Exception e) {
            // Never let logging break the caller
            LOG.log(Level.WARNING,
                    "[ActivityLogService] Failed to persist log entry"
                    + " opmaj=" + opmaj + " profile=" + profilemaj
                    + ": " + e.getMessage(), e);
        }
    }

    /**
     * Persists a fully-enriched audit entry with session context.
     *
     * @param opmaj      operation name (LOGIN, LOGOUT, PREDICTION_CREATED, PREDICTION_UPDATED, …)
     * @param transmaj   detail text
     * @param profilemaj actor username
     * @param userId     actor user ID
     * @param sessionId  HTTP session ID
     * @param ipAddress  client IP
     * @param userAgent  browser user-agent
     * @param matchId    related match ID (null for non-prediction events)
     * @param oldValue   previous prediction score string, e.g. "1-0" (null for CREATE)
     * @param newValue   new prediction score string, e.g. "2-1"
     */
    @Transactional
    public void log(String opmaj, String transmaj, String profilemaj,
                    Long userId, String sessionId,
                    String ipAddress, String userAgent,
                    Long matchId, String oldValue, String newValue) {
        try {
            SystemActivityLog entry = new SystemActivityLog(
                    opmaj, transmaj, profilemaj,
                    userId, sessionId, ipAddress, userAgent,
                    matchId, oldValue, newValue);
            em.persist(entry);
            em.flush();
        } catch (Exception e) {
            LOG.log(Level.WARNING,
                    "[ActivityLogService] Failed to persist enriched log entry"
                    + " opmaj=" + opmaj + " profile=" + profilemaj
                    + ": " + e.getMessage(), e);
        }
    }

    /**
     * Returns all log entries ordered newest-first.
     * Useful for an admin audit-log UI page later.
     */
    @Transactional
    public List<SystemActivityLog> findAll() {
        return em.createQuery(
                "SELECT l FROM SystemActivityLog l ORDER BY l.datemaj DESC",
                SystemActivityLog.class)
                .getResultList();
    }

    /**
     * Returns log entries for a specific profile, newest-first.
     */
    @Transactional
    public List<SystemActivityLog> findByProfile(String profilemaj) {
        return em.createQuery(
                "SELECT l FROM SystemActivityLog l WHERE l.profilemaj = :p ORDER BY l.datemaj DESC",
                SystemActivityLog.class)
                .setParameter("p", profilemaj)
                .getResultList();
    }

    /**
     * Returns log entries for a specific operation type, newest-first.
     */
    @Transactional
    public List<SystemActivityLog> findByOperation(String opmaj) {
        return findFiltered(null, opmaj);
    }

    /**
     * Returns log entries filtered by profile and/or operation type (nulls ignored), newest-first.
     */
    @Transactional
    public List<SystemActivityLog> findFiltered(String profilemaj, String opmaj) {
        StringBuilder jpql = new StringBuilder(
                "SELECT l FROM SystemActivityLog l WHERE 1=1");
        if (profilemaj != null && !profilemaj.trim().isEmpty())
            jpql.append(" AND LOWER(l.profilemaj) LIKE LOWER(:profile)");
        if (opmaj != null && !opmaj.trim().isEmpty())
            jpql.append(" AND l.opmaj = :op");
        jpql.append(" ORDER BY l.datemaj DESC");

        jakarta.persistence.TypedQuery<SystemActivityLog> query =
                em.createQuery(jpql.toString(), SystemActivityLog.class);
        if (profilemaj != null && !profilemaj.trim().isEmpty())
            query.setParameter("profile", "%" + profilemaj + "%");
        if (opmaj != null && !opmaj.trim().isEmpty())
            query.setParameter("op", opmaj);

        return query.getResultList();
    }

    /** Returns the most recent N log entries. */
    @Transactional
    public List<SystemActivityLog> findRecent(int limit) {
        return em.createQuery(
                "SELECT l FROM SystemActivityLog l ORDER BY l.datemaj DESC",
                SystemActivityLog.class)
                .setMaxResults(limit)
                .getResultList();
    }
}
