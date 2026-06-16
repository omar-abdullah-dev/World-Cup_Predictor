package com.worldcup.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Maps the response from GET /fixtures.
 * Times are in UTC as HH:MM:SS.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FixtureDto {

    @JsonProperty("id")
    private long id;

    @JsonProperty("group_id")
    private long groupId;

    @JsonProperty("date")
    private String date;          // YYYY-MM-DD

    @JsonProperty("time")
    private String time;          // HH:MM:SS UTC

    @JsonProperty("location")
    private String location;

    @JsonProperty("round")
    private String round;

    @JsonProperty("home")
    private TeamRefDto home;

    @JsonProperty("away")
    private TeamRefDto away;

    public long getId()           { return id; }
    public long getGroupId()      { return groupId; }
    public String getDate()       { return date; }
    public String getTime()       { return time; }
    public String getLocation()   { return location; }
    public String getRound()      { return round; }
    public TeamRefDto getHome()   { return home; }
    public TeamRefDto getAway()   { return away; }

    public void setId(long id)              { this.id = id; }
    public void setGroupId(long groupId)    { this.groupId = groupId; }
    public void setDate(String date)        { this.date = date; }
    public void setTime(String time)        { this.time = time; }
    public void setLocation(String l)       { this.location = l; }
    public void setRound(String round)      { this.round = round; }
    public void setHome(TeamRefDto home)    { this.home = home; }
    public void setAway(TeamRefDto away)    { this.away = away; }
}
