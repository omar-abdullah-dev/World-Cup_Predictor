package com.worldcup.model;

import jakarta.persistence.*;

@Entity
@Table(name = "teams")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "short_code", length = 3, unique = true)
    private String shortCode;

    @Column(name = "logo_path")
    private String logoPath;

    @Column(name = "flag_emoji")
    private String flagEmoji;

    public Team() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }

    public String getLogoPath() { return logoPath; }
    public void setLogoPath(String logoPath) { this.logoPath = logoPath; }

    public String getFlagEmoji() { return flagEmoji; }
    public void setFlagEmoji(String flagEmoji) { this.flagEmoji = flagEmoji; }
}
