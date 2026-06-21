package com.worldcup.model;

import java.time.LocalDateTime;

/**
 * Plain Java model: Represents an approved Active Directory user in the whitelist.
 *
 * JPA/Hibernate annotations removed — persistence handled by
 * {@link com.worldcup.repository.JpaWhitelistRepository} via pure JDBC.
 *
 * NOTE: The database column name is "adusername" (no underscore).
 * The field is named adUsername here to preserve the existing Java API.
 */
public class WhitelistEntry {

    private Long id;

    /** Maps to DB column "adusername". */
    private String adUsername;

    private String employeeName;
    private String email;
    private boolean enabled = true;
    private LocalDateTime addedAt = LocalDateTime.now();
    private Long addedByUserId;

    public WhitelistEntry() {}

    // ── Accessors ─────────────────────────────────────────────────────────

    public Long getId()                                 { return id; }
    public void setId(Long id)                          { this.id = id; }

    public String getAdUsername()                       { return adUsername; }
    public void setAdUsername(String s)                 { this.adUsername = s; }

    public String getEmployeeName()                     { return employeeName; }
    public void setEmployeeName(String s)               { this.employeeName = s; }

    public String getEmail()                            { return email; }
    public void setEmail(String s)                      { this.email = s; }

    public boolean isEnabled()                          { return enabled; }
    public void setEnabled(boolean b)                   { this.enabled = b; }

    public LocalDateTime getAddedAt()                   { return addedAt; }
    public void setAddedAt(LocalDateTime d)             { this.addedAt = d; }

    public Long getAddedByUserId()                      { return addedByUserId; }
    public void setAddedByUserId(Long id)               { this.addedByUserId = id; }
}
