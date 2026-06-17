package com.worldcup.bean.admin;

import com.worldcup.model.SystemActivityLog;
import com.worldcup.model.UserSession;
import com.worldcup.service.ActivityLogService;
import com.worldcup.service.UserSessionService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Admin backing bean for the Session Monitoring and Activity Audit page.
 *
 * Provides:
 *  - Active session list
 *  - Full session history
 *  - Activity audit log with filtering by username and event type
 */
@Named
@ViewScoped
public class AdminAuditBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");

    @Inject private UserSessionService userSessionService;
    @Inject private ActivityLogService activityLogService;

    // ── Session data ──────────────────────────────────────────────────────
    private List<UserSession> activeSessions;
    private List<UserSession> allSessions;

    // ── Audit log data ────────────────────────────────────────────────────
    private List<SystemActivityLog> auditLogs;

    // ── Filter fields ─────────────────────────────────────────────────────
    private String filterUsername;
    private String filterEventType;

    /** Available event types for the dropdown filter. */
    private static final String[] EVENT_TYPES = {
        "", "LOGIN", "LOGOUT", "PREDICTION_CREATED", "PREDICTION_UPDATED",
        "SESSION_INVALIDATED", "PRED-SUB", "PRED-UPD", "RES-SAV",
        "CRE", "MATCH-DEL", "RND-UPD"
    };

    @PostConstruct
    public void init() {
        refresh();
    }

    public void refresh() {
        activeSessions = userSessionService.getAllActiveSessions();
        allSessions    = userSessionService.getAllSessions();
        applyFilter();
    }

    public void applyFilter() {
        auditLogs = activityLogService.findFiltered(
                nullIfBlank(filterUsername),
                nullIfBlank(filterEventType));
    }

    public void clearFilter() {
        filterUsername  = null;
        filterEventType = null;
        applyFilter();
    }

    /** Admin action: forcibly terminate an active session. */
    public void terminateSession(Long sessionDbId) {
        allSessions.stream()
                .filter(s -> s.getId().equals(sessionDbId))
                .findFirst()
                .ifPresent(s -> userSessionService.onLogout(s.getSessionId()));
        refresh();
    }

    // ── Formatting helpers ────────────────────────────────────────────────

    public String formatDateTime(java.time.LocalDateTime dt) {
        return dt == null ? "" : dt.format(FMT);
    }

    public String sessionStatusBadge(UserSession.Status status) {
        if (status == null) return "badge-default";
        if (status == UserSession.Status.ACTIVE)     return "badge-active";
        if (status == UserSession.Status.TERMINATED) return "badge-terminated";
        if (status == UserSession.Status.EXPIRED)    return "badge-expired";
        if (status == UserSession.Status.DISPLACED)  return "badge-displaced";
        return "badge-default";
    }

    public String eventBadgeClass(String opmaj) {
        if (opmaj == null) return "";
        if ("LOGIN".equals(opmaj))               return "ev-login";
        if ("LOGOUT".equals(opmaj))              return "ev-logout";
        if ("PREDICTION_CREATED".equals(opmaj))  return "ev-pred-new";
        if ("PREDICTION_UPDATED".equals(opmaj))  return "ev-pred-upd";
        if ("SESSION_INVALIDATED".equals(opmaj)) return "ev-session-inv";
        if ("SESSION_CONFLICT".equals(opmaj))    return "ev-session-inv";
        return "ev-default";
    }

    // ── Accessors ─────────────────────────────────────────────────────────

    public List<UserSession>       getActiveSessions()  { return activeSessions; }
    public List<UserSession>       getAllSessions()      { return allSessions; }
    public List<SystemActivityLog> getAuditLogs()       { return auditLogs; }

    public String getFilterUsername()                           { return filterUsername; }
    public void   setFilterUsername(String filterUsername)      { this.filterUsername = filterUsername; }

    public String getFilterEventType()                          { return filterEventType; }
    public void   setFilterEventType(String filterEventType)    { this.filterEventType = filterEventType; }

    public String[] getEventTypes() { return EVENT_TYPES; }

    public int getActiveSessionCount() {
        return activeSessions == null ? 0 : activeSessions.size();
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private String nullIfBlank(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s;
    }
}
