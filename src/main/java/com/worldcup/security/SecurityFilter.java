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

        // Check authentication via session attribute set by AuthBean
        HttpSession session = httpRequest.getSession(false);
        boolean isAuthenticated = false;
        if (session != null) {
            Object authBeanAttr = session.getAttribute("authBean");
            if (authBeanAttr == null) {
                // Try to look up via CDI BeanManager through JNDI if available
                // Fall back to trusting index and login pages
                isAuthenticated = isPublicPage;
            } else {
                // AuthBean is stored in session; check loggedIn flag via toString or instanceof
                try {
                    com.worldcup.bean.AuthBean ab = (com.worldcup.bean.AuthBean) authBeanAttr;
                    isAuthenticated = ab.isLoggedIn();
                } catch (ClassCastException e) {
                    isAuthenticated = false;
                }
            }
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

        chain.doFilter(request, response);
    }
}
