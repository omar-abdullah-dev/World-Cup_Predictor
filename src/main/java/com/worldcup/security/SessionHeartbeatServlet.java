package com.worldcup.security;

import com.worldcup.service.UserSessionService;
import com.worldcup.service.UserSessionService.ValidationResult;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Lightweight JSON heartbeat endpoint.
 *
 * Called by the client-side JS every 15 seconds to verify the session
 * is still valid in the database. Responds with:
 *   {"status":"ok"}          — session valid, keep going
 *   {"status":"displaced"}   — session was displaced by a new login elsewhere
 *   {"status":"expired"}     — session timed out
 *   {"status":"invalid"}     — no DB record (should not normally happen)
 *
 * URL: /session-heartbeat
 * The SecurityFilter skips this servlet (it is not *.xhtml).
 */
@WebServlet(urlPatterns = {"/session-heartbeat"})
public class SessionHeartbeatServlet extends HttpServlet {

    @Inject
    private UserSessionService userSessionService;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setHeader("Cache-Control", "no-store");

        HttpSession session = req.getSession(false);
        if (session == null) {
            resp.getWriter().write("{\"status\":\"invalid\"}");
            return;
        }

        try {
            ValidationResult result = userSessionService.validateAndTouch(session.getId());
            String status;
            if (result == ValidationResult.VALID)      { status = "ok"; }
            else if (result == ValidationResult.DISPLACED) { status = "displaced"; }
            else if (result == ValidationResult.EXPIRED)   { status = "expired"; }
            else                                            { status = "invalid"; }
            resp.getWriter().write("{\"status\":\"" + status + "\"}");
        } catch (Exception e) {
            resp.getWriter().write("{\"status\":\"ok\"}");
        }
    }
}
