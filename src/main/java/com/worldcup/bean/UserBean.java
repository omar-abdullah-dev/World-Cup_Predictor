package com.worldcup.bean;

import com.worldcup.model.User;
import com.worldcup.service.UserService;
import com.worldcup.security.SecurityException;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Named
@RequestScoped
public class UserBean {

    private static final Logger LOGGER = Logger.getLogger(UserBean.class.getName());

    private String username;
    private List<User> users;
    private List<User> pendingUsers;
    private String errorMessage;
    private String successMessage;

    @Inject private UserService userService;
    @Inject private AuthBean authBean;

    public String createUser() {
        errorMessage = null;
        successMessage = null;
        try {
            // Creating users directly is intended for admins only; service will enforce checks if needed
            userService.createUser(username);
            successMessage = "User '" + username + "' created successfully!";
            username = null;
        } catch (IllegalArgumentException e) {
            errorMessage = e.getMessage();
        }
        return null;
    }

    public List<User> getUsers() {
        users = userService.getAllUsers();
        return users;
    }

    public List<User> getPendingUsers() {
        try {
            // Only fetch pending users if current session user is admin
            if (authBean != null && authBean.isAdmin()) {
                pendingUsers = userService.getUnapprovedUsers(authBean.getUser());
            } else {
                pendingUsers = List.of();
            }
        } catch (SecurityException e) {
            pendingUsers = List.of();
            LOGGER.log(Level.WARNING, "Unable to fetch pending users: " + e.getMessage());
        }
        return pendingUsers;
    }

    public String approveUser(Long userId) {
        errorMessage = null;
        successMessage = null;
        try {
            userService.approveUser(authBean.getUser(), userId);
            successMessage = "User approved.";
        } catch (Exception e) {
            errorMessage = e.getMessage();
        }
        return null;
    }

    public String denyUser(Long userId) {
        errorMessage = null;
        successMessage = null;
        try {
            userService.denyUser(authBean.getUser(), userId);
            successMessage = "User denied access.";
        } catch (Exception e) {
            errorMessage = e.getMessage();
        }
        return null;
    }

    public String promoteToAdmin(Long userId) {
        errorMessage = null;
        successMessage = null;
        try {
            userService.promoteToAdmin(authBean.getUser(), userId);
            successMessage = "User promoted to ADMIN.";
        } catch (Exception e) {
            errorMessage = e.getMessage();
        }
        return null;
    }

    public String demoteFromAdmin(Long userId) {
        errorMessage = null;
        successMessage = null;
        try {
            userService.demoteFromAdmin(authBean.getUser(), userId);
            successMessage = "User demoted to NORMAL_USER.";
        } catch (Exception e) {
            errorMessage = e.getMessage();
        }
        return null;
    }

    public String getUsername()                   { return username; }
    public void setUsername(String username)      { this.username = username; }
    public String getErrorMessage()               { return errorMessage; }
    public void setErrorMessage(String m)         { this.errorMessage = m; }
    public String getSuccessMessage()             { return successMessage; }
    public void setSuccessMessage(String m)       { this.successMessage = m; }
}
