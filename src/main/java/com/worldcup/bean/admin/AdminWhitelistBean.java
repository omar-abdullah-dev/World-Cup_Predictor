package com.worldcup.bean.admin;

import com.worldcup.bean.AuthBean;
import com.worldcup.model.WhitelistEntry;
import com.worldcup.service.ActivityLogService;
import com.worldcup.service.WhitelistService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

@Named
@ViewScoped
public class AdminWhitelistBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject private WhitelistService whitelistService;
    @Inject private AuthBean authBean;
    @Inject private ActivityLogService activityLogService;

    private List<WhitelistEntry> entries;
    private String adUsername;
    private String employeeName;
    private String email;

    @PostConstruct
    public void init() {
        loadEntries();
    }

    public void loadEntries() {
        // Guard against stale ViewScoped bean when session has expired
        if (authBean.getUser() == null) {
            entries = java.util.Collections.emptyList();
            return;
        }
        entries = whitelistService.getAllEntries(authBean.getUser());
    }

    public void addEntry() {
        if (authBean.getUser() == null) return;
        try {
            // Enforce corporate domain on both AD username and email
            if (adUsername == null || !adUsername.trim().toUpperCase().endsWith("@QNB.COM.EG")) {
                throw new IllegalArgumentException("AD username must end with @QNB.COM.EG");
            }
            if (email == null || !email.trim().toUpperCase().endsWith("@QNB.COM.EG")) {
                throw new IllegalArgumentException("Email must end with @QNB.COM.EG");
            }
            whitelistService.addEntry(authBean.getUser(), adUsername, employeeName, email);
            String username = authBean.getUser().getUsername();
            activityLogService.log("WL-CRE",
                    "WL-CRE | screen=admin-whitelist.xhtml | user=" + username
                    + " | detail=Added '" + adUsername + "' to whitelist",
                    username);
            addMessage(FacesMessage.SEVERITY_INFO, "User added to whitelist");
            adUsername = employeeName = email = null;
            loadEntries();
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }
    }

    public void toggleStatus(Long id, boolean enabled) {
        if (authBean.getUser() == null) return;
        try {
            whitelistService.toggleStatus(authBean.getUser(), id, enabled);
            String username = authBean.getUser().getUsername();
            activityLogService.log("WL-UPD",
                    "WL-UPD | screen=admin-whitelist.xhtml | user=" + username
                    + " | detail=entryId=" + id + " enabled=" + enabled,
                    username);
            addMessage(FacesMessage.SEVERITY_INFO, "Status updated");
            loadEntries();
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }
    }

    public void removeEntry(Long id) {
        if (authBean.getUser() == null) return;
        try {
            whitelistService.removeEntry(authBean.getUser(), id);
            String username = authBean.getUser().getUsername();
            activityLogService.log("WL-DEL",
                    "WL-DEL | screen=admin-whitelist.xhtml | user=" + username
                    + " | detail=entryId=" + id + " removed from whitelist",
                    username);
            addMessage(FacesMessage.SEVERITY_INFO, "User removed from whitelist");
            loadEntries();
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }
    }

    private void addMessage(FacesMessage.Severity severity, String summary) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, summary, null));
    }

    public List<WhitelistEntry> getEntries() { return entries; }
    public String getAdUsername() { return adUsername; }
    public void setAdUsername(String adUsername) { this.adUsername = adUsername; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
