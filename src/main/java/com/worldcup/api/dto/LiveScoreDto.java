package com.worldcup.api.dto;

public class LiveScoreDto {

    private long id;
    private long fixtureId;
    private String status;
    private String time;
    private ScoresDto scores;
    private TeamRefDto home;
    private TeamRefDto away;
    private String location;

    public LiveScoreDto() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getFixtureId() { return fixtureId; }
    public void setFixtureId(long fixtureId) { this.fixtureId = fixtureId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public ScoresDto getScores() { return scores; }
    public void setScores(ScoresDto scores) { this.scores = scores; }

    public TeamRefDto getHome() { return home; }
    public void setHome(TeamRefDto home) { this.home = home; }

    public TeamRefDto getAway() { return away; }
    public void setAway(TeamRefDto away) { this.away = away; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
