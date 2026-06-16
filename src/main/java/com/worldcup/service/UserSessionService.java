package com.worldcup.service;

import com.worldcup.model.UserSession;
import com.worldcup.repository.JpaUserSessionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Manages the lifecycle of user sessions in the database.
 *
 * Responsibilities:
 *  - Create a UserSession record on login
 *  - Enforce single-active-session per user (displace old session on new login)
 *  - Mark sessions as TERMINATED on explicit logout
 *  - Mark sessions as EXPIRED on timeout
 *  - Validate that an incoming request matches the active DB session
 *  - Update last-activity timestamp on every valid request
 */
@ApplicationScoped
public class UserSessionService {

    private static final Logger LOG = Logger.getLogger(UserSessionService.class.getName());

    /** Session idle timeout: 8 hours. */
    public static final int SESSION_TIMEOUT_HOURS = 8;

    @Inject
    private JpaUserSessionRepository sessionRepository;

    // ── Login ─────────────────────────────────────────────────────────────

    /**
     * Called when a user successfully authenticates.
     *
     * Steps:
     *  1. Find any existing ACTIVE session for this user.
     *  2. Mark it DISPLACED (single-session enforcement).
     *  3. Create and persist a new ACTIVE session.
     *
     * @return the new UserSession (ACTIVE)
     */
    public UserSession onLogin(Long userId, String username,
                               String httpSessionId,
                               String ipAddress, String userAgent) {

        // Terminate any pre-existing active session for this user
        displaceExistingSession(userId);

        // Generate a browser token (stored in localStorage for tab-conflict detection)
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
     * Called on explicit user logout.
     */
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
     * Checks:
     *  - DB record exists for this httpSessionId
     *  - Status is ACTIVE
     *  - Last activity is within SESSION_TIMEOUT_HOURS
     *
     * If valid, updates lastActivityTime.
     *
     * @return true if the session is valid and active
     */
    public boolean validateAndTouch(String httpSessionId) {
        Optional<UserSession> opt = sessionRepository.findBySessionId(httpSessionId);
        if (opt.isEmpty()) return false;

        UserSession session = opt.get();

        if (!session.isActive()) return false;

        // Check idle timeout
        if (session.getLastActivityTime() != null &&
                session.getLastActivityTime().isBefore(
                        LocalDateTime.now().minusHours(SESSION_TIMEOUT_HOURS))) {
            session.expire();
            sessionRepository.save(session);
            LOG.info("[UserSessionService] Session expired (idle timeout): "
                    + httpSessionId + " user=" + session.getUsername());
            return false;
        }

        // Touch — update last activity
        session.touch();
        sessionRepository.save(session);
        return true;
    }

    /**
     * Marks the session as EXPIRED (called by the security filter on timeout).
     */
    public void onExpire(String httpSessionId) {
        sessionRepository.findBySessionId(httpSessionId).ifPresent(s -> {
            s.expire();
            sessionRepository.save(s);
            LOG.info("[UserSessionService] Session expired: " + httpSessionId
                    + " user=" + s.getUsername());
        });
    }

    // ── Browser token (multi-tab detection) ──────────────────────────────

    /**
     * Returns the browser token for the active session of a user.
     * The client stores this in localStorage; tabs that open without it
     * are detected as conflicting.
     */
    public String getBrowserToken(String httpSessionId) {
        return sessionRepository.findBySessionId(httpSessionId)
                .map(UserSession::getBrowserToken)
                .orElse(null);
    }

    // ── Admin queries ─────────────────────────────────────────────────────

    public List<UserSession> getAllActiveSessions() {
        return sessionRepository.findAllActive();
    }

    public List<UserSession> getAllSessions() {
        return sessionRepository.findAll();
    }

    public List<UserSession> getSessionsByUser(Long userId) {
        return sessionRepository.findByUserId(userId);
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private void displaceExistingSession(Long userId) {
        sessionRepository.findActiveByUserId(userId).ifPresent(old -> {
            old.displace();
            sessionRepository.save(old);
            LOG.info("[UserSessionService] Displaced existing session for userId=" + userId
                    + " sessionId=" + old.getSessionId());
        });
    }
}
