package com.worldcup.bean;

import com.worldcup.model.User;
import com.worldcup.service.UserService;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;

/**
 * Manages user session state - Option A from AGENTS.md.
 * User selects themselves once when entering the application.
 * Selected user is stored in session and used for all predictions.
 * This reduces clicks and simplifies the prediction workflow.
 */
@Named
@SessionScoped
public class UserSessionBean implements Serializable {

    @Inject private UserService userService;
    private Long selectedUserId;
    private User selectedUser;

    public String selectUser(Long userId) {
        try {
            this.selectedUser = userService.getUser(userId);
            this.selectedUserId = userId;
        } catch (Exception e) {
            this.selectedUser = null;
            this.selectedUserId = null;
        }
        return null;
    }

    public String clearSelection() {
        this.selectedUser = null;
        this.selectedUserId = null;
        return null;
    }

    public boolean isUserSelected() {
        return selectedUserId != null && selectedUser != null;
    }

    public Long getSelectedUserId() {
        return selectedUserId;
    }

    public User getSelectedUser() {
        return selectedUser;
    }

    public String getSelectedUsername() {
        return selectedUser != null ? selectedUser.getUsername() : "Not Selected";
    }
}

