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
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages user authentication and session state.
 *
 * @SessionScoped — one instance per HTTP session.
 *
 * Java 8 compatible — no String.isBlank(), no var, no switch expressions.
 */
@Named
@SessionScoped
public class AuthBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(AuthBean.class.getName());

    /** HTTP session attribute key for single-tab enforcement token. */
    public static final String TAB_TOKEN_ATTR = "activeTabToken";

    @Inject private AuthenticationService authenticationService;
    @Inject private ActivityLogService    activityLogService;
    @Inject private UserSessionService    userSessionService;

    private User        user;
    private String      username;
    private String      password;
    private String      errorMessage;
    private String      successMessage;

    /** DB session record — kept only as a transient reference, not serialized via JPA proxy. */
    private transient UserSession currentSession;

    /** Browser token stored as plain String — safe across serialization. */
    private String browserToken;

    /**
     * Single-tab enforcement token.
     * Generated on login, stored in the HTTP session attribute "activeTabToken"
     * AND in this bean field. Injected into every page via layout.xhtml.
     * TabEnforcementFilter validates it on every GET request.
     */
    private String currentTabToken;

    // ── Login ─────────────────────────────────────────────────────────────

    public String login() {
        errorMessage   = null;
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

            this.user = authenticationService.authenticate(username.trim(), password);
            this.successMessage = "Welcome, " + user.getUsername() + "!";
            this.password = null;

            FacesContext fc        = FacesContext.getCurrentInstance();
            HttpServletRequest req = (HttpServletRequest) fc.getExternalContext().getRequest();
            HttpSession httpSession = req.getSession(true);
            String sessionId = httpSession.getId();
            String ipAddress = getClientIp(req);
            String userAgent = req.getHeader("User-Agent");

            // ── DB session tracking ───────────────────────────────────────
            this.currentSession = userSessionService.onLogin(
                    user.getId(), user.getUsername(), sessionId, ipAddress, userAgent);
            this.browserToken = (this.currentSession != null)
                    ? this.currentSession.getBrowserToken() : null;

            // ── Single-tab token ──────────────────────────────────────────
            this.currentTabToken = UUID.randomUUID().toString();
            httpSession.setAttribute(TAB_TOKEN_ATTR, this.currentTabToken);

            LOGGER.info("User logged in: " + user.getUsername());
            activityLogService.log("LOGIN",
                    "LOGIN | user=" + user.getUsername()
                    + " | ip=" + ipAddress + " | ua=" + abbreviate(userAgent, 80),
                    user.getUsername(),
                    user.getId(), sessionId, ipAddress, userAgent,
                    null, null, null);

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

    // ── Logout ────────────────────────────────────────────────────────────

    public void logout() {
        if (user != null) {
            LOGGER.info("User logged out: " + user.getUsername());
            try {
                FacesContext fc        = FacesContext.getCurrentInstance();
                HttpServletRequest req = (HttpServletRequest) fc.getExternalContext().getRequest();
                HttpSession httpSess   = req.getSession(false);
                String sessionId = (httpSess != null) ? httpSess.getId() : "unknown";
                String ipAddress = getClientIp(req);
                String userAgent = req.getHeader("User-Agent");

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
        this.user             = null;
        this.currentSession   = null;
        this.browserToken     = null;
        this.currentTabToken  = null;
        this.username         = null;
        this.password         = null;
        this.errorMessage     = null;
        this.successMessage   = null;

        try {
            FacesContext fc = FacesContext.getCurrentInstance();
            // Remove tab token from session before invalidating
            HttpServletRequest req = (HttpServletRequest) fc.getExternalContext().getRequest();
            HttpSession s = req.getSession(false);
            if (s != null) {
                s.removeAttribute(TAB_TOKEN_ATTR);
            }
            fc.getExternalContext().invalidateSession();
            fc.getExternalContext().redirect(
                    fc.getExternalContext().getRequestContextPath() + "/login.xhtml");
            fc.responseComplete();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error during logout redirect", e);
        }
    }

    // ── Session check ─────────────────────────────────────────────────────

    public boolean isLoggedIn() {
        if (user == null) return false;
        User activeUser = authenticationService.validateActiveSession(user.getId());
        if (activeUser == null) {
            this.user = null;
            return false;
        }
        this.user = activeUser;
        return true;
    }

    public boolean isAdmin()      { return user != null && user.isAdmin(); }
    public boolean isNormalUser() { return user != null && user.isNormalUser(); }

    // ── Accessors ─────────────────────────────────────────────────────────

    public User   getUser()               { return user; }
    public String getCurrentUsername()    { return user != null ? user.getUsername() : "Guest"; }
    public Long   getCurrentUserId()      { return user != null ? user.getId() : null; }

    public String getUsername()           { return username; }
    public void   setUsername(String u)   { this.username = u; }

    public String getPassword()           { return password; }
    public void   setPassword(String p)   { this.password = p; }

    public String getErrorMessage()       { return errorMessage; }
    public void   setErrorMessage(String m) { this.errorMessage = m; }

    public String getSuccessMessage()     { return successMessage; }
    public void   setSuccessMessage(String m) { this.successMessage = m; }

    public UserSession getCurrentSession() { return currentSession; }

    /** Browser token for multi-browser session conflict detection. */
    public String getBrowserToken()        { return browserToken; }

    /** Tab token for single-tab enforcement — injected into every page. */
    public String getCurrentTabToken()     { return currentTabToken; }
    public void   setCurrentTabToken(String t) { this.currentTabToken = t; }

    public String getHttpSessionId() {
        try {
            FacesContext fc = FacesContext.getCurrentInstance();
            if (fc == null) return null;
            HttpServletRequest req = (HttpServletRequest) fc.getExternalContext().getRequest();
            HttpSession s = req.getSession(false);
            return (s != null) ? s.getId() : null;
        } catch (Exception e) {
            return null;
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private String getClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.trim().isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    private String abbreviate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
