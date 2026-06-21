package com.worldcup.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Plain Java model: Represents a tournament group (e.g. Group A).
 *
 * JPA/Hibernate annotations removed — persistence handled by
 * {@link com.worldcup.repository.JpaGroupRepository} via pure JDBC.
 * The group_teams join table is managed explicitly in the repository.
 */
public class Group {

    private Long id;
    private String name;
    private RoundStatus status = RoundStatus.OPEN;
    private List<Team> teams = new ArrayList<Team>();
    private TournamentRound round;

    public Group() {}

    // ── Accessors ─────────────────────────────────────────────────────────

    public Long getId()                             { return id; }
    public void setId(Long id)                      { this.id = id; }

    public String getName()                         { return name; }
    public void setName(String name)                { this.name = name; }

    public RoundStatus getStatus()                  { return status; }
    public void setStatus(RoundStatus status)       { this.status = status; }

    public List<Team> getTeams()                    { return teams; }
    public void setTeams(List<Team> teams)          { this.teams = teams; }

    public TournamentRound getRound()               { return round; }
    public void setRound(TournamentRound round)     { this.round = round; }
}
