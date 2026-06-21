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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Admin backing bean for the Session Monitoring and Activity Audit page.
 *
 * Provides:
 *  - Active session list
 *  - Full session history
 *  - Activity audit log with filtering by username and event type
 *
 * Java 8 compatible — no var, no List.of(), no switch expressions.
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
    private List<UserSession>       activeSessions;
    private List<UserSession>       allSessions;

    // ── Audit log data ────────────────────────────────────────────────────
    private List<SystemActivityLog> auditLogs;

    // ── Filter fields ─────────────────────────────────────────────────────
    private String filterUsername;
    private String filterEventType;

    /**
     * Available event types for the dropdown filter.
     * The empty string entry renders as "— All events —" in the XHTML.
     */
    private static final List<String> EVENT_TYPES = Arrays.asList(
        "", "LOGIN", "LOGOUT", "PREDICTION_CREATED", "PREDICTION_UPDATED",
        "SESSION_INVALIDATED", "PRED-SUB", "PRED-UPD", "RES-SAV",
        "CRE", "MATCH-DEL", "RND-UPD", "SESSION_CONFLICT"
    );

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
        if (sessionDbId == null || allSessions == null) return;
        for (UserSession s : allSessions) {
            if (sessionDbId.equals(s.getId())) {
                userSessionService.onLogout(s.getSessionId());
                break;
            }
        }
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
        if (opmaj == null) return "ev-default";
        if ("LOGIN".equals(opmaj))               return "ev-login";
        if ("LOGOUT".equals(opmaj))              return "ev-logout";
        if ("PREDICTION_CREATED".equals(opmaj))  return "ev-pred-new";
        if ("PRED-SUB".equals(opmaj))            return "ev-pred-new";
        if ("PREDICTION_UPDATED".equals(opmaj))  return "ev-pred-upd";
        if ("PRED-UPD".equals(opmaj))            return "ev-pred-upd";
        if ("SESSION_INVALIDATED".equals(opmaj)) return "ev-session-inv";
        if ("SESSION_CONFLICT".equals(opmaj))    return "ev-session-inv";
        return "ev-default";
    }

    /**
     * Returns the label for a dropdown event-type entry.
     * Replaces the broken EL expression #{et.empty ? '...' : et}
     * which fails because String.empty() is not a valid EL method.
     * Called from admin-audit.xhtml as #{adminAuditBean.eventTypeLabel(et)}.
     */
    public String eventTypeLabel(String et) {
        if (et == null || et.trim().isEmpty()) {
            return "\u2014 All events \u2014";
        }
        return et;
    }

    /**
     * Truncates a User-Agent string to at most {@code maxLen} characters.
     * Replaces the broken EL expression [a,b].min() which does not work in JSF EL.
     * Called from XHTML as #{adminAuditBean.truncateUserAgent(s.userAgent)}.
     *
     * @param value the raw User-Agent string (may be null)
     * @return truncated string or '-' if null/empty
     */
    public String truncateUserAgent(String value) {
        return truncate(value, 60);
    }

    /**
     * Truncates any string to the given maximum length.
     *
     * @param value  the string to truncate (may be null)
     * @param maxLen maximum number of characters to return
     * @return truncated string, or '-' if null/empty
     */
    public String truncate(String value, int maxLen) {
        if (value == null || value.trim().isEmpty()) return "-";
        if (value.length() <= maxLen) return value;
        return value.substring(0, maxLen) + "\u2026"; // ellipsis
    }

    // ── Accessors ─────────────────────────────────────────────────────────

    public List<UserSession>       getActiveSessions()  { return activeSessions; }
    public List<UserSession>       getAllSessions()      { return allSessions; }
    public List<SystemActivityLog> getAuditLogs()       { return auditLogs; }

    public String getFilterUsername()                          { return filterUsername; }
    public void   setFilterUsername(String filterUsername)     { this.filterUsername = filterUsername; }

    public String getFilterEventType()                         { return filterEventType; }
    public void   setFilterEventType(String filterEventType)   { this.filterEventType = filterEventType; }

    /** Returns the event-type list for the filter dropdown. */
    public List<String> getEventTypes() { return EVENT_TYPES; }

    public int getActiveSessionCount() {
        return activeSessions == null ? 0 : activeSessions.size();
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private String nullIfBlank(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s;
    }
}
