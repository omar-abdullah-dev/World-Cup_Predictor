package com.worldcup.api.dto;

public class StandingDto {

    private int rank;
    private int points;
    private int matches;
    private int won;
    private int drawn;
    private int lost;
    private int goalsScored;
    private int goalsConceded;
    private int goalDiff;
    private TeamRefDto team;

    public StandingDto() {}

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }

    public int getMatches() { return matches; }
    public void setMatches(int matches) { this.matches = matches; }

    public int getWon() { return won; }
    public void setWon(int won) { this.won = won; }

    public int getDrawn() { return drawn; }
    public void setDrawn(int drawn) { this.drawn = drawn; }

    public int getLost() { return lost; }
    public void setLost(int lost) { this.lost = lost; }

    public int getGoalsScored() { return goalsScored; }
    public void setGoalsScored(int goalsScored) { this.goalsScored = goalsScored; }

    public int getGoalsConceded() { return goalsConceded; }
    public void setGoalsConceded(int goalsConceded) { this.goalsConceded = goalsConceded; }

    public int getGoalDiff() { return goalDiff; }
    public void setGoalDiff(int goalDiff) { this.goalDiff = goalDiff; }

    public TeamRefDto getTeam() { return team; }
    public void setTeam(TeamRefDto team) { this.team = team; }
}
