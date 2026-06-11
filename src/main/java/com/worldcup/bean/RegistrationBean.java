package com.worldcup.bean;

import com.worldcup.model.User;
import com.worldcup.service.UserService;
import com.worldcup.security.SecurityException;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Manages user self-registration.
 * 
 * This bean handles the registration form submission.
 * New users are created in UNAPPROVED state and cannot log in until
 * an administrator grants them system access.
 * 
 * @author Security Team
 */
@Named
@RequestScoped
public class RegistrationBean {
    private static final Logger LOGGER = Logger.getLogger(RegistrationBean.class.getName());

    @Inject private UserService userService;

    private String username;
    private String password;
    private String confirmPassword;
    private String errorMessage;
    private String successMessage;

    // Password validation regex: at least 8 chars, mix of upper/lower/numbers
    private static final Pattern PASSWORD_PATTERN = 
        Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");

    // Username validation regex: alphanumeric + underscore, 3-20 chars
    private static final Pattern USERNAME_PATTERN = 
        Pattern.compile("^[a-zA-Z0-9_]{3,20}$");

    /**
     * Handles registration form submission.
     * 
     * Validation steps:
     * 1. Check username format and length
     * 2. Check password strength
     * 3. Verify password confirmation
     * 4. Create user via UserService
     * 5. Display success message
     * 
     * @return "redirect:login.xhtml" on success, null to stay on registration page on failure
     */
    public String register() {
        errorMessage = null;
        successMessage = null;

        try {
            // Validate inputs
            if (username == null || username.trim().isEmpty()) {
                errorMessage = "Username is required.";
                return null;
            }

            username = username.trim();
            if (!USERNAME_PATTERN.matcher(username).matches()) {
                errorMessage = "Username must be 3-20 characters, alphanumeric and underscores only.";
                return null;
            }

            if (password == null || password.isEmpty()) {
                errorMessage = "Password is required.";
                return null;
            }

            if (!PASSWORD_PATTERN.matcher(password).matches()) {
                errorMessage = "Password must be at least 8 characters with uppercase, lowercase, and numbers.";
                return null;
            }

            if (confirmPassword == null || !confirmPassword.equals(password)) {
                errorMessage = "Passwords do not match.";
                return null;
            }

            // Attempt registration
            User newUser = userService.registerUser(username, password);
            
            successMessage = "Registration successful! Your account is pending admin approval. "
                + "You will receive confirmation once approved.";
            
            LOGGER.info("New user registered: " + username);
            
            // Clear form
            this.username = null;
            this.password = null;
            this.confirmPassword = null;
            
            // Redirect to login after success
            return "redirect:login.xhtml";
        } catch (IllegalArgumentException e) {
            errorMessage = e.getMessage();
            LOGGER.log(Level.INFO, "Registration validation error: " + errorMessage);
            return null;
        } catch (SecurityException e) {
            errorMessage = e.getMessage();
            LOGGER.log(Level.WARNING, "Registration security error: " + errorMessage);
            return null;
        } catch (Exception e) {
            errorMessage = "An unexpected error occurred during registration.";
            LOGGER.log(Level.SEVERE, "Registration error", e);
            return null;
        }
    }

    // ===== Form Input Properties =====

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String msg) { this.errorMessage = msg; }

    public String getSuccessMessage() { return successMessage; }
    public void setSuccessMessage(String msg) { this.successMessage = msg; }
}
