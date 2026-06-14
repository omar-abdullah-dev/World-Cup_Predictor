package com.worldcup.bean.admin;

import com.worldcup.bean.AuthBean;
import com.worldcup.model.WhitelistEntry;
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

    private List<WhitelistEntry> entries;
    private String adUsername;
    private String employeeName;
    private String email;

    @PostConstruct
    public void init() {
        loadEntries();
    }

    public void loadEntries() {
        entries = whitelistService.getAllEntries(authBean.getUser());
    }

    public void addEntry() {
        try {
            whitelistService.addEntry(authBean.getUser(), adUsername, employeeName, email);
            addMessage(FacesMessage.SEVERITY_INFO, "User added to whitelist");
            adUsername = employeeName = email = null;
            loadEntries();
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }
    }

    public void toggleStatus(Long id, boolean enabled) {
        try {
            whitelistService.toggleStatus(authBean.getUser(), id, enabled);
            addMessage(FacesMessage.SEVERITY_INFO, "Status updated");
            loadEntries();
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }
    }

    public void removeEntry(Long id) {
        try {
            whitelistService.removeEntry(authBean.getUser(), id);
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
