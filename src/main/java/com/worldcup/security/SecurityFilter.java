package com.worldcup.security;

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
 * Security Filter for enforcing authentication on protected resources.
 * Uses session attribute to check login state (avoids CDI injection issues in filters).
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

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String requestURI = httpRequest.getRequestURI();

        // Skip JSF resource requests (css, js, images)
        if (requestURI.contains("/jakarta.faces.resource/") || requestURI.contains("/javax.faces.resource/")) {
            chain.doFilter(request, response);
            return;
        }

        // Check if the requested page is public
        boolean isPublicPage = PUBLIC_PATTERNS.stream().anyMatch(requestURI::contains)
            || requestURI.endsWith("/")
            || requestURI.contains("/error");

        // Check authentication via session and CDI
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            long maxAge = 24L * 60L * 60L * 1000L; // 24 hours
            if (System.currentTimeMillis() - session.getCreationTime() > maxAge) {
                session.invalidate();
                LOGGER.log(Level.INFO, "Session expired (24h limit) for: {0}", requestURI);
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/login.xhtml");
                return;
            }
        }

        boolean isAuthenticated = false;
        com.worldcup.bean.AuthBean ab = null;
        try {
            ab = jakarta.enterprise.inject.spi.CDI.current().select(com.worldcup.bean.AuthBean.class).get();
            isAuthenticated = ab != null && ab.isLoggedIn();
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Failed to resolve AuthBean via CDI", e);
        }

        // Allow index.xhtml as landing page (it shows login prompt if not authenticated)
        if (requestURI.contains("/index.xhtml") || requestURI.endsWith("/world-cup-predictor/")) {
            chain.doFilter(request, response);
            return;
        }

        if (!isPublicPage && !isAuthenticated) {
            LOGGER.log(Level.INFO, "Unauthenticated access attempt to: {0}", requestURI);
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/login.xhtml");
            return;
        }

        // Enforce Admin role on admin pages
        if (requestURI.contains("admin-") || requestURI.contains("/admin/")) {
            if (ab == null || !ab.isAdmin()) {
                LOGGER.log(Level.WARNING, "Unauthorized admin access attempt to: {0}", requestURI);
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied: Admin role required");
                return;
            }
        }

        chain.doFilter(request, response);
    }
}
