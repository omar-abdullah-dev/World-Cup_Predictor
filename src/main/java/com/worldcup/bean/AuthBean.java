package com.worldcup.bean;

import com.worldcup.model.User;
import com.worldcup.security.AuthenticationService;
import com.worldcup.security.SecurityException;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

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

    private User user;
    private String username;
    private String password;
    private String errorMessage;
    private String successMessage;

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
            
            LOGGER.info("User logged in: " + user.getUsername());
            
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
     * Clears the user from session and invalidates the session.
     * 
     * @return "redirect:login.xhtml"
     */
    public String logout() {
        if (user != null) {
            LOGGER.info("User logged out: " + user.getUsername());
        }
        this.user = null;
        this.username = null;
        this.password = null;
        this.errorMessage = null;
        this.successMessage = null;

        // Invalidate session
        try {
            FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error invalidating session", e);
        }

        return "redirect:login.xhtml";
    }

    /**
     * Checks if a user is currently logged in and approved.
     * 
     * @return true if user is authenticated and approved, false otherwise
     */
    public boolean isLoggedIn() {
        return user != null && user.isApproved();
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
}
