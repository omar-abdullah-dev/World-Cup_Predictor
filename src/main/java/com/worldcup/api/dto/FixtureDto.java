package com.worldcup.api.dto;

public class FixtureDto {

    private long id;
    private long groupId;
    private String date;
    private String time;
    private String location;
    private String round;
    private TeamRefDto home;
    private TeamRefDto away;

    public FixtureDto() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getGroupId() { return groupId; }
    public void setGroupId(long groupId) { this.groupId = groupId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getRound() { return round; }
    public void setRound(String round) { this.round = round; }

    public TeamRefDto getHome() { return home; }
    public void setHome(TeamRefDto home) { this.home = home; }

    public TeamRefDto getAway() { return away; }
    public void setAway(TeamRefDto away) { this.away = away; }
}
