package com.worldcup.security;

import com.worldcup.service.ActivityLogService;
import com.worldcup.service.UserSessionService;
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
 * Per-request checks:
 *  1. Skip public pages and JSF resources.
 *  2. Hard 24-hour session age limit (invalidate + redirect).
 *  3. Validate session against UserSession DB record (idle timeout, DISPLACED, EXPIRED).
 *  4. Redirect unauthenticated requests to login.
 *  5. Block non-admins from admin pages.
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

        HttpServletRequest  httpRequest  = (HttpServletRequest)  request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String requestURI = httpRequest.getRequestURI();

        // 1. Skip JSF resource requests (css, js, images)
        if (requestURI.contains("/jakarta.faces.resource/") || requestURI.contains("/javax.faces.resource/")) {
            chain.doFilter(request, response);
            return;
        }

        // Check if the requested page is public
        boolean isPublicPage = PUBLIC_PATTERNS.stream().anyMatch(requestURI::contains)
            || requestURI.endsWith("/")
            || requestURI.contains("/error");

        // 2. Hard 24-hour session age check
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            long maxAge = 24L * 60L * 60L * 1000L;
            if (System.currentTimeMillis() - session.getCreationTime() > maxAge) {
                expireSession(session, httpRequest);
                LOGGER.log(Level.INFO, "Session expired (24h limit) for: {0}", requestURI);
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/login.xhtml");
                return;
            }
        }

        // 3. Resolve AuthBean via CDI
        boolean isAuthenticated = false;
        com.worldcup.bean.AuthBean ab = null;
        try {
            ab = jakarta.enterprise.inject.spi.CDI.current()
                    .select(com.worldcup.bean.AuthBean.class).get();
            isAuthenticated = ab != null && ab.isLoggedIn();
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Failed to resolve AuthBean via CDI", e);
        }

        // 4. Validate DB session on every authenticated request
        if (isAuthenticated && session != null) {
            try {
                UserSessionService uss = jakarta.enterprise.inject.spi.CDI.current()
                        .select(UserSessionService.class).get();
                boolean valid = uss.validateAndTouch(session.getId());
                if (!valid) {
                    // DB session is no longer active (displaced, expired, or timed out)
                    LOGGER.log(Level.INFO,
                            "DB session invalid/expired for sessionId={0}, forcing logout",
                            session.getId());
                    ActivityLogService als = jakarta.enterprise.inject.spi.CDI.current()
                            .select(ActivityLogService.class).get();
                    String username = ab != null ? ab.getCurrentUsername() : "unknown";
                    als.log("SESSION_INVALIDATED",
                            "SESSION_INVALIDATED | sessionId=" + session.getId()
                            + " | user=" + username + " | reason=DB session no longer active",
                            username);
                    // Force logout — clear AuthBean state and invalidate HTTP session
                    if (ab != null) {
                        try { ab.logout(); } catch (Exception ignored) {}
                    } else {
                        session.invalidate();
                    }
                    httpResponse.sendRedirect(httpRequest.getContextPath()
                            + "/login.xhtml?sessionExpired=true");
                    return;
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error validating DB session", e);
                // On error, allow request through rather than locking everyone out
            }
        }

        // Allow index.xhtml / root as landing page
        if (requestURI.contains("/index.xhtml") || requestURI.endsWith("/world-cup-predictor/")) {
            chain.doFilter(request, response);
            return;
        }

        if (!isPublicPage && !isAuthenticated) {
            LOGGER.log(Level.INFO, "Unauthenticated access attempt to: {0}", requestURI);
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/login.xhtml");
            return;
        }

        // 5. Enforce Admin role on admin pages
        if (requestURI.contains("admin-") || requestURI.contains("/admin/")) {
            if (ab == null || !ab.isAdmin()) {
                LOGGER.log(Level.WARNING, "Unauthorized admin access attempt to: {0}", requestURI);
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied: Admin role required");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private void expireSession(HttpSession session, HttpServletRequest req) {
        try {
            UserSessionService uss = jakarta.enterprise.inject.spi.CDI.current()
                    .select(UserSessionService.class).get();
            uss.onExpire(session.getId());
        } catch (Exception ignored) {}
        try { session.invalidate(); } catch (Exception ignored) {}
    }
}
