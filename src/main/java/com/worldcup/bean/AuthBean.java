package com.worldcup.bean;

import com.worldcup.model.User;
import com.worldcup.model.UserSession;
import com.worldcup.security.AuthenticationService;
import com.worldcup.security.SecurityException;
import com.worldcup.service.ActivityLogService;
import com.worldcup.service.UserSessionService;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages user authentication and session state.
 * 
 * This bean is @SessionScoped (one instance per HTTP session).
 * Once a user logs in, their User object is stored in the session.
 * All subsequent requests use the same authenticated User.
 * 
 * Usage in JSF:
 * - #{authBean.loggedIn} to check if user is authenticated
 * - #{authBean.user} to access the current user
 * - #{authBean.login()} to handle login form submission
 * - #{authBean.logout()} to clear session
 * 
 * @author Security Team
 */
@Named
@SessionScoped
public class AuthBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(AuthBean.class.getName());

    @Inject private AuthenticationService authenticationService;
    @Inject private ActivityLogService activityLogService;
    @Inject private UserSessionService userSessionService;

    private User user;
    private String username;
    private String password;
    private String errorMessage;
    private String successMessage;

    /** DB session record for the current login — stored in the HTTP session. */
    private UserSession currentSession;

    /**
     * Handles login form submission.
     * Validates username/password and stores authenticated User in session.
     * 
     * @return "redirect:index.xhtml" on success, null to stay on login page on failure
     */

    public String login() {
        errorMessage = null;
        successMessage = null;

        try {
            if (username == null || username.trim().isEmpty()) {
                errorMessage = "Username is required.";
                return null;
            }
            if (password == null || password.isEmpty()) {
                errorMessage = "Password is required.";
                return null;
            }

            // Authenticate user with provided credentials
            this.user = authenticationService.authenticate(username.trim(), password);
            this.successMessage = "Welcome, " + user.getUsername() + "!";
            
            // Clear sensitive data
            this.password = null;

            // ── Session tracking ──────────────────────────────────────────
            FacesContext fc = FacesContext.getCurrentInstance();
            HttpServletRequest req = (HttpServletRequest) fc.getExternalContext().getRequest();
            HttpSession httpSession = req.getSession(true);
            String sessionId  = httpSession.getId();
            String ipAddress  = getClientIp(req);
            String userAgent  = req.getHeader("User-Agent");

            this.currentSession = userSessionService.onLogin(
                    user.getId(), user.getUsername(), sessionId, ipAddress, userAgent);

            LOGGER.info("User logged in: " + user.getUsername());
            activityLogService.log("LOGIN",
                    "LOGIN | user=" + user.getUsername()
                    + " | ip=" + ipAddress + " | ua=" + abbreviate(userAgent, 80),
                    user.getUsername(),
                    user.getId(), sessionId, ipAddress, userAgent,
                    null, null, null);
            
            // Redirect to dashboard
            return "/index.xhtml?faces-redirect=true";
        } catch (SecurityException e) {
            errorMessage = e.getMessage();
            LOGGER.log(Level.WARNING, "Login failed: " + errorMessage);
            return null;
        } catch (Exception e) {
            errorMessage = "An unexpected error occurred during login.";
            LOGGER.log(Level.SEVERE, "Login error", e);
            return null;
        }
    }

    /**
     * Handles logout.
     * Uses direct redirect via ExternalContext to avoid JSF navigation case errors
     * when logging out from any page (including admin pages with no faces-config rules).
     */
    public void logout() {
        if (user != null) {
            LOGGER.info("User logged out: " + user.getUsername());
            // ── Session tracking ──────────────────────────────────────────
            try {
                FacesContext fc2 = FacesContext.getCurrentInstance();
                HttpServletRequest req2 = (HttpServletRequest) fc2.getExternalContext().getRequest();
                HttpSession httpSess = req2.getSession(false);
                String sessionId = httpSess != null ? httpSess.getId() : "unknown";
                String ipAddress = getClientIp(req2);
                String userAgent = req2.getHeader("User-Agent");

                userSessionService.onLogout(sessionId);
                activityLogService.log("LOGOUT",
                        "LOGOUT | user=" + user.getUsername() + " | ip=" + ipAddress,
                        user.getUsername(),
                        user.getId(), sessionId, ipAddress, userAgent,
                        null, null, null);
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Error recording logout audit", ex);
            }
        }
        this.user           = null;
        this.currentSession = null;
        this.username       = null;
        this.password       = null;
        this.errorMessage   = null;
        this.successMessage = null;

        try {
            FacesContext fc = FacesContext.getCurrentInstance();
            fc.getExternalContext().invalidateSession();
            fc.getExternalContext().redirect(
                fc.getExternalContext().getRequestContextPath() + "/login.xhtml");
            fc.responseComplete();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error during logout redirect", e);
        }
    }

    /**
     * Checks if a user is currently logged in and approved.
     * 
     * @return true if user is authenticated and approved, false otherwise
     */
    public boolean isLoggedIn() {
        if (user == null) return false;
        
        // Dynamically check against Whitelist
        User activeUser = authenticationService.validateActiveSession(user.getId());
        if (activeUser == null) {
            this.user = null;
            return false;
        }
        this.user = activeUser;
        return true;
    }

    /**
     * Checks if the current user is an admin.
     * 
     * @return true if user is logged in and has ADMIN role, false otherwise
     */
    public boolean isAdmin() {
        return user != null && user.isAdmin();
    }

    /**
     * Checks if the current user is a normal user (non-admin).
     * 
     * @return true if user is logged in and has NORMAL_USER role, false otherwise
     */
    public boolean isNormalUser() {
        return user != null && user.isNormalUser();
    }

    /**
     * Gets the currently authenticated user.
     * 
     * @return User object, or null if not logged in
     */
    public User getUser() {
        return user;
    }

    /**
     * Gets the username of the currently authenticated user.
     * 
     * @return username string, or "Guest" if not logged in
     */
    public String getCurrentUsername() {
        return user != null ? user.getUsername() : "Guest";
    }

    /**
     * Gets the user ID of the currently authenticated user.
     * 
     * @return user ID, or null if not logged in
     */
    public Long getCurrentUserId() {
        return user != null ? user.getId() : null;
    }

    // ===== Form Input Properties =====

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String msg) { this.errorMessage = msg; }

    public String getSuccessMessage() { return successMessage; }
    public void setSuccessMessage(String msg) { this.successMessage = msg; }

    /** The DB-tracked session for this login — may be null before login. */
    public UserSession getCurrentSession() { return currentSession; }

    /** Returns the current HTTP session ID, or null if no session exists. */
    public String getHttpSessionId() {
        try {
            FacesContext fc = FacesContext.getCurrentInstance();
            if (fc == null) return null;
            HttpServletRequest req = (HttpServletRequest) fc.getExternalContext().getRequest();
            HttpSession s = req.getSession(false);
            return s != null ? s.getId() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Returns the browser token to be injected into the page for tab-conflict detection. */
    public String getBrowserToken() {
        return currentSession != null ? currentSession.getBrowserToken() : null;
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private String getClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    private String abbreviate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
