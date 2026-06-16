package com.worldcup.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StandingDto {
    @JsonProperty("rank")            private int rank;
    @JsonProperty("points")          private int points;
    @JsonProperty("matches")         private int matches;
    @JsonProperty("won")             private int won;
    @JsonProperty("drawn")           private int drawn;
    @JsonProperty("lost")            private int lost;
    @JsonProperty("goals_scored")    private int goalsScored;
    @JsonProperty("goals_conceded")  private int goalsConceded;
    @JsonProperty("goal_diff")       private int goalDiff;
    @JsonProperty("team")            private TeamRefDto team;

    public int getRank()             { return rank; }
    public int getPoints()           { return points; }
    public int getMatches()          { return matches; }
    public int getWon()              { return won; }
    public int getDrawn()            { return drawn; }
    public int getLost()             { return lost; }
    public int getGoalsScored()      { return goalsScored; }
    public int getGoalsConceded()    { return goalsConceded; }
    public int getGoalDiff()         { return goalDiff; }
    public TeamRefDto getTeam()      { return team; }

    public void setRank(int r)              { this.rank = r; }
    public void setPoints(int p)            { this.points = p; }
    public void setMatches(int m)           { this.matches = m; }
    public void setWon(int w)               { this.won = w; }
    public void setDrawn(int d)             { this.drawn = d; }
    public void setLost(int l)              { this.lost = l; }
    public void setGoalsScored(int gs)      { this.goalsScored = gs; }
    public void setGoalsConceded(int gc)    { this.goalsConceded = gc; }
    public void setGoalDiff(int gd)         { this.goalDiff = gd; }
    public void setTeam(TeamRefDto t)       { this.team = t; }
}
