package com.worldcup.security;

import com.worldcup.service.ActivityLogService;
import com.worldcup.service.UserSessionService;
import com.worldcup.service.UserSessionService.ValidationResult;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Security Filter — enforces authentication and validates DB-tracked sessions on every request.
 *
 * Per-request pipeline:
 *  1. Skip JSF resources (css/js/images).
 *  2. Hard 24-hour session age → expire + redirect to login.
 *  3. For authenticated requests: validate DB session record.
 *     - DISPLACED  → log SESSION_CONFLICT, invalidate HTTP session, redirect.
 *     - EXPIRED    → log SESSION_EXPIRED, invalidate HTTP session, redirect.
 *     - INVALID    → log UNAUTHORIZED_ACCESS_ATTEMPT, invalidate, redirect.
 *     - VALID      → allow, heartbeat already updated by validateAndTouch().
 *  4. Unauthenticated access to protected pages → redirect to login.
 *  5. Non-admin access to admin pages → 403.
 *
 * IMPORTANT: All redirects use HttpServletResponse directly.
 * FacesContext is NOT available in a servlet filter — never call it here.
 */
@WebFilter(
    filterName = "SecurityFilter",
    urlPatterns = {"*.xhtml"}
)
public class SecurityFilter implements Filter {

    private static final Logger LOGGER = Logger.getLogger(SecurityFilter.class.getName());

    private static final List<String> PUBLIC_PATTERNS = Arrays.asList(
        "/login.xhtml",
        "/register.xhtml"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  req  = (HttpServletRequest)  request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String uri = req.getRequestURI();

        // 1. Skip JSF resource requests
        if (uri.contains("/jakarta.faces.resource/") || uri.contains("/javax.faces.resource/")) {
            chain.doFilter(request, response);
            return;
        }

        boolean isPublicPage = PUBLIC_PATTERNS.stream().anyMatch(uri::contains)
                || uri.endsWith("/")
                || uri.contains("/error");

        HttpSession httpSession = req.getSession(false);

        // 2. Hard 24-hour session age limit
        if (httpSession != null) {
            long maxAgeMs = 24L * 60L * 60L * 1000L;
            if (System.currentTimeMillis() - httpSession.getCreationTime() > maxAgeMs) {
                String sid      = httpSession.getId();
                String username = resolveUsername(sid);
                String ip       = clientIp(req);
                LOGGER.log(Level.INFO, "Hard 24h session limit reached for user={0} sid={1}",
                        new Object[]{username, sid});
                auditLog("SESSION_EXPIRED",
                        "SESSION_EXPIRED | user=" + username + " | reason=24h_hard_limit | sid=" + sid,
                        username, null, sid, ip, req.getHeader("User-Agent"));
                expireAndInvalidate(httpSession);
                resp.sendRedirect(req.getContextPath() + "/login.xhtml?reason=expired");
                return;
            }
        }

        // 3. Resolve AuthBean to check authentication state
        boolean isAuthenticated = false;
        com.worldcup.bean.AuthBean ab = null;
        try {
            ab = jakarta.enterprise.inject.spi.CDI.current()
                    .select(com.worldcup.bean.AuthBean.class).get();
            isAuthenticated = (ab != null) && ab.isLoggedIn();
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "CDI AuthBean resolution failed", e);
        }

        // 4. Validate DB session on every authenticated request
        if (isAuthenticated && httpSession != null) {
            String sid = httpSession.getId();
            try {
                UserSessionService uss = cdi(UserSessionService.class);
                ValidationResult result = uss.validateAndTouch(sid);

                if (result != ValidationResult.VALID) {
                    String username = (ab != null) ? ab.getCurrentUsername() : resolveUsername(sid);
                    Long   userId   = (ab != null) ? ab.getCurrentUserId()   : resolveUserId(sid);
                    String ip       = clientIp(req);
                    String ua       = req.getHeader("User-Agent");

                    switch (result) {
                        case DISPLACED -> {
                            LOGGER.log(Level.WARNING,
                                    "Session displaced for user={0} sid={1}", new Object[]{username, sid});
                            auditLog("SESSION_CONFLICT",
                                    "SESSION_CONFLICT | user=" + username
                                    + " | sid=" + sid + " | reason=displaced_by_new_login"
                                    + " | ip=" + ip,
                                    username, userId, sid, ip, ua);
                        }
                        case EXPIRED -> {
                            LOGGER.log(Level.INFO,
                                    "Session idle-expired for user={0} sid={1}", new Object[]{username, sid});
                            auditLog("SESSION_EXPIRED",
                                    "SESSION_EXPIRED | user=" + username
                                    + " | sid=" + sid + " | reason=idle_timeout"
                                    + " | ip=" + ip,
                                    username, userId, sid, ip, ua);
                        }
                        case INVALID -> {
                            LOGGER.log(Level.WARNING,
                                    "Orphan HTTP session for user={0} sid={1}", new Object[]{username, sid});
                            auditLog("UNAUTHORIZED_ACCESS_ATTEMPT",
                                    "UNAUTHORIZED_ACCESS_ATTEMPT | user=" + username
                                    + " | sid=" + sid + " | reason=no_db_record"
                                    + " | ip=" + ip,
                                    username, userId, sid, ip, ua);
                        }
                        default -> { /* VALID — handled above */ }
                    }

                    // Invalidate the HTTP session (AuthBean state stays until GC,
                    // but the session is gone so isLoggedIn() will return false on next req)
                    try { httpSession.invalidate(); } catch (Exception ignored) {}

                    resp.sendRedirect(req.getContextPath()
                            + "/login.xhtml?reason=" + result.name().toLowerCase());
                    return;
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "DB session validation error — allowing request through", e);
            }
        }

        // Allow index.xhtml / app root as landing page (shows login prompt if not authenticated)
        if (uri.contains("/index.xhtml") || uri.endsWith("/world-cup-predictor/")) {
            chain.doFilter(request, response);
            return;
        }

        // 5. Unauthenticated access to protected pages
        if (!isPublicPage && !isAuthenticated) {
            String ip = clientIp(req);
            LOGGER.log(Level.INFO, "Unauthenticated access to {0} from {1}", new Object[]{uri, ip});
            auditLog("UNAUTHORIZED_ACCESS_ATTEMPT",
                    "UNAUTHORIZED_ACCESS_ATTEMPT | uri=" + uri + " | ip=" + ip + " | reason=not_authenticated",
                    "anonymous", null,
                    httpSession != null ? httpSession.getId() : "none",
                    ip, req.getHeader("User-Agent"));
            resp.sendRedirect(req.getContextPath() + "/login.xhtml");
            return;
        }

        // 6. Enforce admin role on admin pages
        if (uri.contains("admin-") || uri.contains("/admin/")) {
            if (ab == null || !ab.isAdmin()) {
                String username = ab != null ? ab.getCurrentUsername() : "unknown";
                LOGGER.log(Level.WARNING, "Non-admin access attempt to {0} by {1}",
                        new Object[]{uri, username});
                auditLog("UNAUTHORIZED_ACCESS_ATTEMPT",
                        "UNAUTHORIZED_ACCESS_ATTEMPT | uri=" + uri
                        + " | user=" + username + " | reason=not_admin",
                        username,
                        ab != null ? ab.getCurrentUserId() : null,
                        httpSession != null ? httpSession.getId() : "none",
                        clientIp(req), req.getHeader("User-Agent"));
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied: Admin role required");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private void expireAndInvalidate(HttpSession session) {
        try {
            cdi(UserSessionService.class).onExpire(session.getId());
        } catch (Exception ignored) {}
        try { session.invalidate(); } catch (Exception ignored) {}
    }

    private void auditLog(String event, String detail, String username,
                          Long userId, String sessionId, String ip, String ua) {
        try {
            cdi(ActivityLogService.class).log(
                    event, detail, username,
                    userId, sessionId, ip, ua,
                    null, null, null);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to write audit log for event=" + event, e);
        }
    }

    private String resolveUsername(String sessionId) {
        try { return cdi(UserSessionService.class).getUsernameForSession(sessionId); }
        catch (Exception e) { return "unknown"; }
    }

    private Long resolveUserId(String sessionId) {
        try { return cdi(UserSessionService.class).getUserIdForSession(sessionId); }
        catch (Exception e) { return null; }
    }

    private static String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        return (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : req.getRemoteAddr();
    }

    private static <T> T cdi(Class<T> type) {
        return jakarta.enterprise.inject.spi.CDI.current().select(type).get();
    }
}
