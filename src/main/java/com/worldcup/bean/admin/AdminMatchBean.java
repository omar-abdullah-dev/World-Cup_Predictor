package com.worldcup.bean.admin;

import com.worldcup.bean.AuthBean;
import com.worldcup.model.Match;
import com.worldcup.service.MatchService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Named
@ViewScoped
public class AdminMatchBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject private MatchService matchService;
    @Inject private AuthBean authBean;

    private List<MatchAdminRow> matchRows = new ArrayList<>();
    private String homeTeam;
    private String awayTeam;
    private String kickoffDate;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @PostConstruct
    public void init() {
        loadMatches();
    }

    public void loadMatches() {
        matchRows = new ArrayList<>();
        for (Match m : matchService.getAllMatches()) {
            MatchAdminRow row = new MatchAdminRow(m);
            if (m.isFinished()) {
                row.setHomeScore(m.getHomeScore());
                row.setAwayScore(m.getAwayScore());
            }
            matchRows.add(row);
        }
    }

    public void createMatch() {
        try {
            LocalDateTime kickoff = LocalDateTime.parse(kickoffDate, DATE_FMT);
            matchService.createMatch(authBean.getUser(), homeTeam, awayTeam, kickoff);
            addMessage(FacesMessage.SEVERITY_INFO, "Match created successfully");
            homeTeam = awayTeam = kickoffDate = null;
            loadMatches();
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }
    }

    /**
     * Force-submits a result for any match, bypassing time window restrictions.
     */
    public void forceSubmitResult(MatchAdminRow row) {
        if (row == null || row.getMatchId() == null) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Invalid match selection.");
            return;
        }
        if (row.getHomeScore() == null || row.getAwayScore() == null) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Please enter both scores.");
            return;
        }
        try {
            matchService.forceUpdateResult(authBean.getUser(), row.getMatchId(),
                    row.getHomeScore(), row.getAwayScore());
            addMessage(FacesMessage.SEVERITY_INFO, "Result recorded successfully");
            loadMatches();
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }
    }

    public void deleteMatch(Long matchId) {
        try {
            matchService.deleteMatch(authBean.getUser(), matchId);
            addMessage(FacesMessage.SEVERITY_INFO, "Match deleted");
            loadMatches();
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }
    }

    private void addMessage(FacesMessage.Severity severity, String summary) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, summary, null));
    }

    public String getTeamInitials(String teamName) {
        if (teamName == null || teamName.isEmpty()) return "???";
        String[] parts = teamName.trim().split("\\s+");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        }
        int end = Math.min(teamName.length(), 3);
        return teamName.substring(0, end).toUpperCase();
    }

    public List<MatchAdminRow> getMatchRows() { return matchRows; }
    public String getHomeTeam() { return homeTeam; }
    public void setHomeTeam(String homeTeam) { this.homeTeam = homeTeam; }
    public String getAwayTeam() { return awayTeam; }
    public void setAwayTeam(String awayTeam) { this.awayTeam = awayTeam; }
    public String getKickoffDate() { return kickoffDate; }
    public void setKickoffDate(String kickoffDate) { this.kickoffDate = kickoffDate; }

    public static class MatchAdminRow implements Serializable {
        private static final long serialVersionUID = 1L;

        private final Match match;
        private Integer homeScore;
        private Integer awayScore;

        MatchAdminRow(Match match) {
            this.match = match;
        }

        public Long getMatchId() {
            return match.getId();
        }

        public Match getMatch() {
            return match;
        }

        public Integer getHomeScore() { return homeScore; }
        public void setHomeScore(Integer homeScore) { this.homeScore = homeScore; }
        public Integer getAwayScore() { return awayScore; }
        public void setAwayScore(Integer awayScore) { this.awayScore = awayScore; }
    }
}
