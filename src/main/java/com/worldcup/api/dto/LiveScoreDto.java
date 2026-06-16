package com.worldcup.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LiveScoreDto {
    @JsonProperty("id")          private long id;
    @JsonProperty("fixture_id")  private long fixtureId;
    @JsonProperty("status")      private String status;
    @JsonProperty("time")        private String time;
    @JsonProperty("scores")      private ScoresDto scores;
    @JsonProperty("home")        private TeamRefDto home;
    @JsonProperty("away")        private TeamRefDto away;
    @JsonProperty("location")    private String location;

    public long getId()           { return id; }
    public long getFixtureId()    { return fixtureId; }
    public String getStatus()     { return status == null ? "" : status; }
    public String getTime()       { return time == null ? "" : time; }
    public ScoresDto getScores()  { return scores; }
    public TeamRefDto getHome()   { return home; }
    public TeamRefDto getAway()   { return away; }
    public String getLocation()   { return location; }

    public void setId(long id)             { this.id = id; }
    public void setFixtureId(long fid)     { this.fixtureId = fid; }
    public void setStatus(String s)        { this.status = s; }
    public void setTime(String t)          { this.time = t; }
    public void setScores(ScoresDto sc)    { this.scores = sc; }
    public void setHome(TeamRefDto h)      { this.home = h; }
    public void setAway(TeamRefDto a)      { this.away = a; }
    public void setLocation(String l)      { this.location = l; }
}
