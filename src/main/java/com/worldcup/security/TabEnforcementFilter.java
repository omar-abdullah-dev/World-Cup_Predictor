package com.worldcup.security;

import com.worldcup.bean.AuthBean;
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
import java.util.logging.Logger;

/**
 * Single-tab enforcement filter.
 *
 * Every authenticated GET request must carry the correct tab token
 * in the "tabToken" query parameter (appended by the JS in layout.xhtml).
 *
 * If the token is present but does NOT match the session's "activeTabToken",
 * the user is redirected to session-conflict.xhtml — a second tab has been
 * detected and is blocked.
 *
 * POST requests (JSF form submissions) are passed through without token check
 * to avoid breaking the JSF lifecycle.
 *
 * Skipped URLs:
 *  - /login.xhtml, /register.xhtml, /session-conflict.xhtml
 *  - JSF resource requests (jakarta.faces.resource)
 *  - Error pages (/error)
 *  - Requests where no "activeTabToken" is in the session yet (not yet logged in)
 *
 * Java 8 compatible.
 */
@WebFilter(filterName = "TabEnforcementFilter", urlPatterns = {"*.xhtml"})
public class TabEnforcementFilter implements Filter {

    private static final Logger LOG = Logger.getLogger(TabEnforcementFilter.class.getName());

    /** HTTP session attribute set by AuthBean on login. */
    private static final String SESSION_ATTR = AuthBean.TAB_TOKEN_ATTR;

    /** Request parameter appended to every link/form by the JS in layout.xhtml. */
    private static final String PARAM_NAME   = "tabToken";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  req  = (HttpServletRequest)  request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String uri = req.getRequestURI();

        // ── Always skip ───────────────────────────────────────────────────
        if (uri.contains("/jakarta.faces.resource/")
                || uri.contains("/javax.faces.resource/")
                || uri.contains("login.xhtml")
                || uri.contains("register.xhtml")
                || uri.contains("session-conflict.xhtml")
                || uri.contains("/error")
                || uri.endsWith("/")
                // Skip the heartbeat servlet
                || uri.contains("session-heartbeat")) {
            chain.doFilter(request, response);
            return;
        }

        // ── Only enforce on GET requests — POST = JSF form submission ─────
        if (!"GET".equalsIgnoreCase(req.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);

        // No session or no token in session → user not logged in yet, skip
        if (session == null) {
            chain.doFilter(request, response);
            return;
        }

        Object sessionToken = session.getAttribute(SESSION_ATTR);
        if (sessionToken == null) {
            chain.doFilter(request, response);
            return;
        }

        // ── Tab token check ───────────────────────────────────────────────
        String requestToken = req.getParameter(PARAM_NAME);

        if (requestToken != null && !requestToken.trim().isEmpty()) {
            if (!requestToken.trim().equals(sessionToken.toString())) {
                // Token mismatch — second tab detected
                LOG.warning("[TabEnforcementFilter] Tab conflict: uri=" + uri
                        + " sessionToken=" + sessionToken
                        + " requestToken=" + requestToken);
                resp.sendRedirect(req.getContextPath() + "/session-conflict.xhtml");
                return;
            }
        }
        // Token absent on first navigation after login (no JS has run yet) → allow through

        chain.doFilter(request, response);
    }
}
