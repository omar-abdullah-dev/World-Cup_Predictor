package com.worldcup.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Plain Java model: Tracks every active/past user HTTP session.
 *
 * JPA/Hibernate annotations removed — persistence handled by
 * {@link com.worldcup.repository.JpaUserSessionRepository} via pure JDBC.
 *
 * Lifecycle:
 *   ACTIVE     - user has logged in and the session is valid
 *   TERMINATED - user explicitly logged out
 *   EXPIRED    - session timed out or was server-invalidated
 *   DISPLACED  - replaced by a newer login from the same user
 */
public class UserSession implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Status { ACTIVE, TERMINATED, EXPIRED, DISPLACED }

    private Long id;
    private Long userId;
    private String username;
    private String sessionId;
    private String browserToken;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime loginTime;
    private LocalDateTime lastActivityTime;
    private LocalDateTime logoutTime;
    private Status status;

    public UserSession() {
        this.loginTime        = LocalDateTime.now();
        this.lastActivityTime = LocalDateTime.now();
        this.status           = Status.ACTIVE;
    }

    // ── Factory ───────────────────────────────────────────────────────────

    public static UserSession createNew(Long userId, String username,
                                        String sessionId, String browserToken,
                                        String ipAddress, String userAgent) {
        UserSession s  = new UserSession();
        s.userId       = userId;
        s.username     = username;
        s.sessionId    = sessionId;
        s.browserToken = browserToken;
        s.ipAddress    = ipAddress;
        s.userAgent    = userAgent;
        return s;
    }

    // ── Lifecycle helpers ─────────────────────────────────────────────────

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

    // ── Accessors ─────────────────────────────────────────────────────────

    public Long getId()                                     { return id; }
    public void setId(Long id)                              { this.id = id; }

    public Long getUserId()                                 { return userId; }
    public void setUserId(Long userId)                      { this.userId = userId; }

    public String getUsername()                             { return username; }
    public void setUsername(String username)                { this.username = username; }

    public String getSessionId()                            { return sessionId; }
    public void setSessionId(String sessionId)              { this.sessionId = sessionId; }

    public String getBrowserToken()                         { return browserToken; }
    public void setBrowserToken(String browserToken)        { this.browserToken = browserToken; }

    public String getIpAddress()                            { return ipAddress; }
    public void setIpAddress(String ipAddress)              { this.ipAddress = ipAddress; }

    public String getUserAgent()                            { return userAgent; }
    public void setUserAgent(String userAgent)              { this.userAgent = userAgent; }

    public LocalDateTime getLoginTime()                     { return loginTime; }
    public void setLoginTime(LocalDateTime loginTime)       { this.loginTime = loginTime; }

    public LocalDateTime getLastActivityTime()              { return lastActivityTime; }
    public void setLastActivityTime(LocalDateTime d)        { this.lastActivityTime = d; }

    public LocalDateTime getLogoutTime()                    { return logoutTime; }
    public void setLogoutTime(LocalDateTime logoutTime)     { this.logoutTime = logoutTime; }

    public Status getStatus()                               { return status; }
    public void setStatus(Status status)                    { this.status = status; }

    // ── Object contract ───────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(id, ((UserSession) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "UserSession{id=" + id + ", username='" + username + "'"
                + ", sessionId='" + sessionId + "', status=" + status
                + ", loginTime=" + loginTime + "}";
    }
}
