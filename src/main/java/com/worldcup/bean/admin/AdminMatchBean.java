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
import java.util.List;

@Named
@ViewScoped
public class AdminMatchBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject private MatchService matchService;
    @Inject private AuthBean authBean;

    private List<Match> matches;
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
        matches = matchService.getAllMatches();
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

    public void submitResult(Long matchId, Integer homeScore, Integer awayScore) {
        try {
            matchService.updateResult(authBean.getUser(), matchId, homeScore, awayScore);
            addMessage(FacesMessage.SEVERITY_INFO, "Result recorded");
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

    /**
     * Returns the first 3 characters of a team name, uppercased.
     * Used in the view to display team initials without EL Math.min ambiguity.
     */
    public String getTeamInitials(String teamName) {
        if (teamName == null || teamName.isEmpty()) return "???";
        int end = Math.min(teamName.length(), 3);
        return teamName.substring(0, end).toUpperCase();
    }

    public List<Match> getMatches() { return matches; }
    public String getHomeTeam() { return homeTeam; }
    public void setHomeTeam(String homeTeam) { this.homeTeam = homeTeam; }
    public String getAwayTeam() { return awayTeam; }
    public void setAwayTeam(String awayTeam) { this.awayTeam = awayTeam; }
    public String getKickoffDate() { return kickoffDate; }
    public void setKickoffDate(String kickoffDate) { this.kickoffDate = kickoffDate; }
}
