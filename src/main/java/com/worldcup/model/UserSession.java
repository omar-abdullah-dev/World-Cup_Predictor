package com.worldcup.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA Entity: Tracks every active/past user HTTP session.
 *
 * Lifecycle:
 *  ACTIVE      - user has logged in and session is valid
 *  TERMINATED  - user explicitly logged out
 *  EXPIRED     - session timed out or was invalidated by the server
 *  DISPLACED   - replaced by a newer login from the same user (single-session enforcement)
 */
@Entity
@Table(name = "user_sessions",
       indexes = {
           @Index(name = "idx_us_user_id",    columnList = "user_id"),
           @Index(name = "idx_us_session_id", columnList = "session_id"),
           @Index(name = "idx_us_status",     columnList = "status")
       })
public class UserSession implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Status { ACTIVE, TERMINATED, EXPIRED, DISPLACED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK to users table. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Username — denormalised for fast audit queries. */
    @Column(name = "username", nullable = false, length = 100)
    private String username;

    /** Servlet container HTTP session ID (jsessionid). */
    @Column(name = "session_id", nullable = false, length = 255)
    private String sessionId;

    /**
     * Browser fingerprint / token stored in browser localStorage.
     * Used for multi-tab detection: every tab that opens sends this token;
     * only the session that owns it is authoritative.
     */
    @Column(name = "browser_token", length = 128)
    private String browserToken;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "login_time", nullable = false)
    private LocalDateTime loginTime;

    @Column(name = "last_activity_time")
    private LocalDateTime lastActivityTime;

    @Column(name = "logout_time")
    private LocalDateTime logoutTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    public UserSession() {
        this.loginTime = LocalDateTime.now();
        this.lastActivityTime = LocalDateTime.now();
        this.status = Status.ACTIVE;
    }

    // ── convenience factory ───────────────────────────────────────────────

    public static UserSession createNew(Long userId, String username,
                                        String sessionId, String browserToken,
                                        String ipAddress, String userAgent) {
        UserSession s = new UserSession();
        s.userId           = userId;
        s.username         = username;
        s.sessionId        = sessionId;
        s.browserToken     = browserToken;
        s.ipAddress        = ipAddress;
        s.userAgent        = userAgent;
        return s;
    }

    // ── lifecycle helpers ─────────────────────────────────────────────────

    public void terminate() {
        this.status     = Status.TERMINATED;
        this.logoutTime = LocalDateTime.now();
    }

    public void expire() {
        this.status     = Status.EXPIRED;
        this.logoutTime = LocalDateTime.now();
    }

    public void displace() {
        this.status     = Status.DISPLACED;
        this.logoutTime = LocalDateTime.now();
    }

    public void touch() {
        this.lastActivityTime = LocalDateTime.now();
    }

    public boolean isActive() {
        return status == Status.ACTIVE;
    }

    // ── accessors ─────────────────────────────────────────────────────────

    public Long getId()                         { return id; }
    public void setId(Long id)                  { this.id = id; }

    public Long getUserId()                     { return userId; }
    public void setUserId(Long userId)          { this.userId = userId; }

    public String getUsername()                 { return username; }
    public void setUsername(String username)    { this.username = username; }

    public String getSessionId()                { return sessionId; }
    public void setSessionId(String sessionId)  { this.sessionId = sessionId; }

    public String getBrowserToken()                         { return browserToken; }
    public void setBrowserToken(String browserToken)        { this.browserToken = browserToken; }

    public String getIpAddress()                { return ipAddress; }
    public void setIpAddress(String ipAddress)  { this.ipAddress = ipAddress; }

    public String getUserAgent()                { return userAgent; }
    public void setUserAgent(String userAgent)  { this.userAgent = userAgent; }

    public LocalDateTime getLoginTime()                         { return loginTime; }
    public void setLoginTime(LocalDateTime loginTime)           { this.loginTime = loginTime; }

    public LocalDateTime getLastActivityTime()                          { return lastActivityTime; }
    public void setLastActivityTime(LocalDateTime lastActivityTime)     { this.lastActivityTime = lastActivityTime; }

    public LocalDateTime getLogoutTime()                        { return logoutTime; }
    public void setLogoutTime(LocalDateTime logoutTime)         { this.logoutTime = logoutTime; }

    public Status getStatus()                   { return status; }
    public void setStatus(Status status)        { this.status = status; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(id, ((UserSession) o).id);
    }

    @Override public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "UserSession{id=" + id + ", username='" + username + "'"
                + ", sessionId='" + sessionId + "', status=" + status
                + ", loginTime=" + loginTime + "}";
    }
}
