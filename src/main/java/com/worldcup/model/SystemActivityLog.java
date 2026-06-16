package com.worldcup.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * JPA Entity: Records a system activity log entry for each user interaction.
 *
 * Immutable audit log — records are never updated or deleted.
 *
 * Core columns (original):
 *  opmaj       - operation type  (e.g. LOGIN, LOGOUT, PREDICTION_CREATED, PREDICTION_UPDATED)
 *  datemaj     - timestamp of the operation
 *  transmaj    - free-text detail
 *  profilemaj  - username of the actor
 *
 * Extended audit columns (added for session tracking):
 *  user_id     - FK to users (denormalised for fast queries)
 *  session_id  - HTTP session ID at the time of the event
 *  ip_address  - client IP
 *  user_agent  - browser user-agent string
 *  match_id    - for prediction events
 *  old_value   - previous prediction value (PREDICTION_UPDATED)
 *  new_value   - new prediction value (PREDICTION_CREATED / PREDICTION_UPDATED)
 */
@Entity
@Table(name = "system_activity_log",
       indexes = {
           @Index(name = "idx_sal_profilemaj", columnList = "profilemaj"),
           @Index(name = "idx_sal_datemaj",    columnList = "datemaj"),
           @Index(name = "idx_sal_opmaj",      columnList = "opmaj"),
           @Index(name = "idx_sal_user_id",    columnList = "user_id"),
           @Index(name = "idx_sal_session_id", columnList = "session_id")
       })
public class SystemActivityLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Major operation name (e.g. LOGIN, LOGOUT, PREDICTION_CREATED, PREDICTION_UPDATED). */
    @Column(name = "opmaj", nullable = false, length = 100)
    private String opmaj;

    /** Timestamp when the operation occurred. */
    @Column(name = "datemaj", nullable = false)
    private LocalDateTime datemaj;

    /** Transaction detail / free-text description of what happened. */
    @Column(name = "transmaj", length = 500)
    private String transmaj;

    /** Username / profile identifier of the actor. */
    @Column(name = "profilemaj", nullable = false, length = 100)
    private String profilemaj;

    // ── Extended audit fields ─────────────────────────────────────────────

    /** User ID — denormalised for fast audit queries. */
    @Column(name = "user_id")
    private Long userId;

    /** HTTP session ID at the time of the event. */
    @Column(name = "session_id", length = 255)
    private String sessionId;

    /** Client IP address. */
    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    /** Browser User-Agent string. */
    @Column(name = "user_agent", length = 512)
    private String userAgent;

    /** Match ID — populated for prediction events. */
    @Column(name = "match_id")
    private Long matchId;

    /** Previous value — populated for PREDICTION_UPDATED events (e.g. "1-0"). */
    @Column(name = "old_value", length = 100)
    private String oldValue;

    /** New value — populated for PREDICTION_CREATED/UPDATED events (e.g. "2-1"). */
    @Column(name = "new_value", length = 100)
    private String newValue;

    // ── constructors ──────────────────────────────────────────────────────

    public SystemActivityLog() {
        this.datemaj = LocalDateTime.now();
    }

    /** Backward-compatible constructor — existing callers unchanged. */
    public SystemActivityLog(String opmaj, String transmaj, String profilemaj) {
        this.opmaj      = opmaj;
        this.transmaj   = transmaj;
        this.profilemaj = profilemaj;
        this.datemaj    = LocalDateTime.now();
    }

    /** Full constructor for session-aware audit entries. */
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

    // ── accessors ─────────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOpmaj() { return opmaj; }
    public void setOpmaj(String opmaj) { this.opmaj = opmaj; }

    public LocalDateTime getDatemaj() { return datemaj; }
    public void setDatemaj(LocalDateTime datemaj) { this.datemaj = datemaj; }

    public String getTransmaj() { return transmaj; }
    public void setTransmaj(String transmaj) { this.transmaj = transmaj; }

    public String getProfilemaj() { return profilemaj; }
    public void setProfilemaj(String profilemaj) { this.profilemaj = profilemaj; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public Long getMatchId() { return matchId; }
    public void setMatchId(Long matchId) { this.matchId = matchId; }

    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }

    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }

    @Override
    public String toString() {
        return "SystemActivityLog{id=" + id
                + ", opmaj='" + opmaj + '\''
                + ", datemaj=" + datemaj
                + ", profilemaj='" + profilemaj + '\''
                + ", transmaj='" + transmaj + '\'' + '}';
    }
}
