package com.worldcup.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Plain Java model: Immutable audit log entry.
 *
 * JPA/Hibernate annotations removed — persistence handled by
 * {@link com.worldcup.service.ActivityLogService} via pure JDBC.
 *
 * Core columns:
 *   opmaj       - operation type  (LOGIN, LOGOUT, PRED-SUB, RES-SAV, …)
 *   datemaj     - timestamp
 *   transmaj    - free-text detail
 *   profilemaj  - username of the actor
 *
 * Extended audit columns:
 *   user_id, session_id, ip_address, user_agent, match_id, old_value, new_value
 */
public class SystemActivityLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String opmaj;
    private LocalDateTime datemaj;
    private String transmaj;
    private String profilemaj;

    // ── Extended audit fields ─────────────────────────────────────────────
    private Long userId;
    private String sessionId;
    private String ipAddress;
    private String userAgent;
    private Long matchId;
    private String oldValue;
    private String newValue;

    // ── Constructors ──────────────────────────────────────────────────────

    public SystemActivityLog() {
        this.datemaj = LocalDateTime.now();
    }

    public SystemActivityLog(String opmaj, String transmaj, String profilemaj) {
        this.opmaj      = opmaj;
        this.transmaj   = transmaj;
        this.profilemaj = profilemaj;
        this.datemaj    = LocalDateTime.now();
    }

    public SystemActivityLog(String opmaj, String transmaj, String profilemaj,
                              Long userId, String sessionId,
                              String ipAddress, String userAgent,
                              Long matchId, String oldValue, String newValue) {
        this.opmaj      = opmaj;
        this.transmaj   = transmaj;
        this.profilemaj = profilemaj;
        this.datemaj    = LocalDateTime.now();
        this.userId     = userId;
        this.sessionId  = sessionId;
        this.ipAddress  = ipAddress;
        this.userAgent  = userAgent;
        this.matchId    = matchId;
        this.oldValue   = oldValue;
        this.newValue   = newValue;
    }

    // ── Accessors ─────────────────────────────────────────────────────────

    public Long getId()                         { return id; }
    public void setId(Long id)                  { this.id = id; }

    public String getOpmaj()                    { return opmaj; }
    public void setOpmaj(String s)              { this.opmaj = s; }

    public LocalDateTime getDatemaj()           { return datemaj; }
    public void setDatemaj(LocalDateTime d)     { this.datemaj = d; }

    public String getTransmaj()                 { return transmaj; }
    public void setTransmaj(String s)           { this.transmaj = s; }

    public String getProfilemaj()               { return profilemaj; }
    public void setProfilemaj(String s)         { this.profilemaj = s; }

    public Long getUserId()                     { return userId; }
    public void setUserId(Long id)              { this.userId = id; }

    public String getSessionId()                { return sessionId; }
    public void setSessionId(String s)          { this.sessionId = s; }

    public String getIpAddress()                { return ipAddress; }
    public void setIpAddress(String s)          { this.ipAddress = s; }

    public String getUserAgent()                { return userAgent; }
    public void setUserAgent(String s)          { this.userAgent = s; }

    public Long getMatchId()                    { return matchId; }
    public void setMatchId(Long id)             { this.matchId = id; }

    public String getOldValue()                 { return oldValue; }
    public void setOldValue(String s)           { this.oldValue = s; }

    public String getNewValue()                 { return newValue; }
    public void setNewValue(String s)           { this.newValue = s; }

    @Override
    public String toString() {
        return "SystemActivityLog{id=" + id + ", opmaj='" + opmaj + "'"
                + ", datemaj=" + datemaj + ", profilemaj='" + profilemaj + "'}";
    }
}
