package com.worldcup.bean.admin;

import com.worldcup.api.MatchSyncService;
import com.worldcup.api.dto.SyncResultDto;
import com.worldcup.bean.AuthBean;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin bean for manual API synchronisation controls.
 * Accessible at /admin-sync.xhtml
 */
@Named
@ViewScoped
public class AdminSyncBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");

    @Inject private MatchSyncService matchSyncService;
    @Inject private AuthBean authBean;

    private List<String> syncLog = new ArrayList<>();
    private String lastSyncTime = "Never";
    private String lastSyncStatus = "—";

    @PostConstruct
    public void init() {
        if (authBean.getUser() == null) syncLog.clear();
    }

    /** Trigger a full fixture sync from the API. */
    public void syncFixtures() {
        if (authBean.getUser() == null) return;
        try {
            SyncResultDto result = matchSyncService.syncAllFixtures();
            lastSyncTime   = LocalDateTime.now().format(FMT);
            lastSyncStatus = result.isSuccess() ? "SUCCESS" : "PARTIAL";
            String msg = "Fixtures sync: created=" + result.getCreated()
                    + " updated=" + result.getUpdated()
                    + " skipped=" + result.getSkipped()
                    + " errors=" + result.getErrors();
            syncLog.add(0, "[" + lastSyncTime + "] " + msg);
            if (syncLog.size() > 50) syncLog = syncLog.subList(0, 50);
            addMessage(FacesMessage.SEVERITY_INFO, msg);
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Sync failed: " + e.getMessage());
        }
    }

    /** Trigger a live score sync from the API. */
    public void syncLiveScores() {
        if (authBean.getUser() == null) return;
        try {
            SyncResultDto result = matchSyncService.syncLiveScores();
            lastSyncTime   = LocalDateTime.now().format(FMT);
            lastSyncStatus = result.isSuccess() ? "SUCCESS" : "PARTIAL";
            String msg = "Live scores sync: updated=" + result.getUpdated()
                    + " errors=" + result.getErrors();
            syncLog.add(0, "[" + lastSyncTime + "] " + msg);
            if (syncLog.size() > 50) syncLog = syncLog.subList(0, 50);
            addMessage(FacesMessage.SEVERITY_INFO, msg);
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Live sync failed: " + e.getMessage());
        }
    }

    private void addMessage(FacesMessage.Severity sev, String text) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(sev, text, null));
    }

    public List<String> getSyncLog()     { return syncLog; }
    public String getLastSyncTime()      { return lastSyncTime; }
    public String getLastSyncStatus()    { return lastSyncStatus; }
}
