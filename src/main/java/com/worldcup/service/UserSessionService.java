package com.worldcup.service;

import com.worldcup.model.UserSession;
import com.worldcup.repository.JpaUserSessionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Manages the lifecycle of user sessions in the database.
 *
 * ALL public methods are @Transactional to ensure session state
 * changes (touch, displace, expire, terminate) are committed.
 */
@ApplicationScoped
public class UserSessionService {

    private static final Logger LOG = Logger.getLogger(UserSessionService.class.getName());

    /** Session idle timeout in hours. */
    public static final int SESSION_TIMEOUT_HOURS = 8;

    @Inject private JpaUserSessionRepository sessionRepository;
    @Inject private ActivityLogService activityLogService;

    // ── Login ─────────────────────────────────────────────────────────────

    /**
     * Called when a user successfully authenticates.
     *
     * Displaces any existing active session for the user (single-session enforcement),
     * creates and persists a new ACTIVE session, and logs SESSION_CONFLICT if displaced.
     *
     * @return the new UserSession (ACTIVE)
     */
    @Transactional
    public UserSession onLogin(Long userId, String username,
                               String httpSessionId,
                               String ipAddress, String userAgent) {

        // Displace any existing active session — single-session enforcement
        displaceExistingSession(userId, username, ipAddress, userAgent);

        String browserToken = UUID.randomUUID().toString().replace("-", "");

        UserSession session = UserSession.createNew(
                userId, username, httpSessionId, browserToken, ipAddress, userAgent);
        session = sessionRepository.save(session);

        LOG.info("[UserSessionService] New session created: user=" + username
                + " sessionId=" + httpSessionId + " ip=" + ipAddress);
        return session;
    }

    // ── Logout ────────────────────────────────────────────────────────────

    /**
     * Marks the session identified by httpSessionId as TERMINATED.
     */
    @Transactional
    public void onLogout(String httpSessionId) {
        sessionRepository.findBySessionId(httpSessionId).ifPresent(s -> {
            s.terminate();
            sessionRepository.save(s);
            LOG.info("[UserSessionService] Session terminated: " + httpSessionId
                    + " user=" + s.getUsername());
        });
    }

    // ── Validation ────────────────────────────────────────────────────────

    /**
     * Validates a request's session against the database record.
     *
     * Returns ValidationResult with the outcome so the filter can
     * log the right event without making a second DB call.
     *
     * @return ValidationResult indicating VALID, INVALID (no record / displaced),
     *         or EXPIRED (idle timeout exceeded)
     */
    @Transactional
    public ValidationResult validateAndTouch(String httpSessionId) {
        Optional<UserSession> opt = sessionRepository.findBySessionId(httpSessionId);
        if (opt.isEmpty()) {
            return ValidationResult.INVALID;
        }

        UserSession session = opt.get();

        if (!session.isActive()) {
            // Already displaced or terminated — reject
            return ValidationResult.DISPLACED;
        }

        // Check idle timeout
        if (session.getLastActivityTime() != null &&
                session.getLastActivityTime().isBefore(
                        LocalDateTime.now().minusHours(SESSION_TIMEOUT_HOURS))) {
            session.expire();
            sessionRepository.save(session);
            LOG.info("[UserSessionService] Session expired (idle): " + httpSessionId
                    + " user=" + session.getUsername());
            return ValidationResult.EXPIRED;
        }

        // Valid — touch
        session.touch();
        sessionRepository.save(session);
        return ValidationResult.VALID;
    }

    /** Outcome of a session validation check. */
    public enum ValidationResult {
        VALID,      // session exists, active, within timeout
        INVALID,    // no DB record found (orphan HTTP session)
        DISPLACED,  // session was displaced by a new login
        EXPIRED     // idle timeout exceeded
    }

    /**
     * Marks the session as EXPIRED (called by the filter on hard 24h limit).
     */
    @Transactional
    public void onExpire(String httpSessionId) {
        sessionRepository.findBySessionId(httpSessionId).ifPresent(s -> {
            s.expire();
            sessionRepository.save(s);
            LOG.info("[UserSessionService] Session hard-expired: " + httpSessionId
                    + " user=" + s.getUsername());
        });
    }

    /**
     * Looks up the username stored in the DB session record for an HTTP session ID.
     * Used by the filter to log events without needing AuthBean.
     */
    @Transactional
    public String getUsernameForSession(String httpSessionId) {
        return sessionRepository.findBySessionId(httpSessionId)
                .map(UserSession::getUsername)
                .orElse("unknown");
    }

    /**
     * Looks up the userId stored in the DB session record for an HTTP session ID.
     */
    @Transactional
    public Long getUserIdForSession(String httpSessionId) {
        return sessionRepository.findBySessionId(httpSessionId)
                .map(UserSession::getUserId)
                .orElse(null);
    }

    // ── Browser token ─────────────────────────────────────────────────────

    @Transactional
    public String getBrowserToken(String httpSessionId) {
        return sessionRepository.findBySessionId(httpSessionId)
                .map(UserSession::getBrowserToken)
                .orElse(null);
    }

    // ── Admin queries ─────────────────────────────────────────────────────

    @Transactional
    public List<UserSession> getAllActiveSessions() {
        return sessionRepository.findAllActive();
    }

    @Transactional
    public List<UserSession> getAllSessions() {
        return sessionRepository.findAll();
    }

    @Transactional
    public List<UserSession> getSessionsByUser(Long userId) {
        return sessionRepository.findByUserId(userId);
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private void displaceExistingSession(Long userId, String newUsername,
                                          String newIp, String newUa) {
        sessionRepository.findActiveByUserId(userId).ifPresent(old -> {
            String oldSessionId = old.getSessionId();
            String oldIp        = old.getIpAddress();
            old.displace();
            sessionRepository.save(old);
            LOG.info("[UserSessionService] Displaced session for userId=" + userId
                    + " oldSessionId=" + oldSessionId);

            // Log SESSION_CONFLICT — new login displaced an existing session
            activityLogService.log(
                    "SESSION_CONFLICT",
                    "SESSION_CONFLICT | user=" + newUsername
                    + " | new_ip=" + newIp + " | old_ip=" + oldIp
                    + " | old_session=" + oldSessionId
                    + " | action=old_session_displaced",
                    newUsername,
                    userId, oldSessionId, newIp, newUa,
                    null, null, null);
        });
    }
}
