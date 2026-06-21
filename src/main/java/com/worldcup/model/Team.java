package com.worldcup.model;

/**
 * Plain Java model: Represents a national team in the tournament.
 *
 * JPA/Hibernate annotations removed — persistence handled by
 * {@link com.worldcup.repository.JpaTeamRepository} via pure JDBC.
 */
public class Team {

    private Long id;
    private String name;
    private String shortCode;
    private String logoPath;
    private String flagEmoji;

    public Team() {}

    // ── Accessors ─────────────────────────────────────────────────────────

    public Long getId()                         { return id; }
    public void setId(Long id)                  { this.id = id; }

    public String getName()                     { return name; }
    public void setName(String name)            { this.name = name; }

    public String getShortCode()                { return shortCode; }
    public void setShortCode(String s)          { this.shortCode = s; }

    public String getLogoPath()                 { return logoPath; }
    public void setLogoPath(String p)           { this.logoPath = p; }

    public String getFlagEmoji()                { return flagEmoji; }
    public void setFlagEmoji(String e)          { this.flagEmoji = e; }
}
