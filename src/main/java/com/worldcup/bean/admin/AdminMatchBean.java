package com.worldcup.bean.admin;

import com.worldcup.bean.AuthBean;
import com.worldcup.model.Match;
import com.worldcup.service.ActivityLogService;
import com.worldcup.service.MatchService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Named
@ViewScoped
public class AdminMatchBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(AdminMatchBean.class.getName());

    private static final List<String> TEAM_NAMES = new ArrayList<String>(Arrays.asList(
            "Algeria", "Argentina", "Australia", "Austria", "Belgium",
            "Bosnia & Herzegovina", "Brazil", "Canada", "Cape Verde", "Colombia",
            "Croatia", "Czech Republic", "DR Congo", "Ecuador", "Egypt",
            "England", "France", "Germany", "Ghana", "Haiti",
            "Iran", "Iraq", "Ivory Coast", "Japan", "Jordan",
            "Mexico", "Morocco", "Netherlands", "New Zealand", "Norway",
            "Panama", "Paraguay", "Portugal", "Qatar", "Saudi Arabia",
            "Scotland", "Senegal", "South Africa", "South Korea", "Spain",
            "Sweden", "Switzerland", "Tunisia", "Turkey", "Uruguay",
            "USA", "Uzbekistan", "Curacao"
    ));

    @Inject private MatchService matchService;
    @Inject private AuthBean authBean;
    @Inject private ActivityLogService activityLogService;

    private List<MatchAdminRow> matchRows = new ArrayList<MatchAdminRow>();
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
        if (authBean.getUser() == null) {
            matchRows = new ArrayList<MatchAdminRow>();
            return;
        }
        matchRows = new ArrayList<MatchAdminRow>();
        for (Match m : matchService.getAllMatches()) {
            matchRows.add(new MatchAdminRow(m));
        }
    }

    public void createMatch() {
        try {
            LocalDateTime kickoff = LocalDateTime.parse(kickoffDate, DATE_FMT);
            matchService.createMatch(authBean.getUser(), homeTeam, awayTeam, kickoff);
            String username = authBean.getUser().getUsername();
            activityLogService.log("CRE",
                    "CRE | screen=admin-matches.xhtml | user=" + username
                            + " | detail=Match created: " + homeTeam + " vs " + awayTeam + " at " + kickoff,
                    username);
            addMessage(FacesMessage.SEVERITY_INFO, "Match created successfully");
            homeTeam = awayTeam = kickoffDate = null;
            loadMatches();
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }
    }

    public void forceSubmitResult(Long matchId) {
        if (matchId == null) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Invalid match selection.");
            return;
        }

        ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
        Map<String, String> params = ec.getRequestParameterMap();

        String hsRaw = params.get("hs_" + matchId);
        String asRaw = params.get("as_" + matchId);

        if (isNullOrEmpty(hsRaw) || isNullOrEmpty(asRaw)) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Please enter both scores.");
            return;
        }

        int hs, as;
        try {
            hs = Integer.parseInt(hsRaw.trim());
            as = Integer.parseInt(asRaw.trim());
        } catch (NumberFormatException e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Scores must be whole numbers.");
            return;
        }

        if (hs < 0 || as < 0) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Scores cannot be negative.");
            return;
        }

        String etHsRaw      = params.get("et_hs_"      + matchId);
        String etAsRaw      = params.get("et_as_"      + matchId);
        String penHsRaw     = params.get("pen_hs_"     + matchId);
        String penAsRaw     = params.get("pen_as_"     + matchId);
        String decidedByRaw = params.get("decided_by_" + matchId);

        boolean hasKnockoutData = !isNullOrEmpty(decidedByRaw)
                && !"90".equals(decidedByRaw.trim());

        try {
            String username = authBean.getUser().getUsername();

            if (hasKnockoutData) {
                Integer etHs     = parseOptionalInt(etHsRaw);
                Integer etAs     = parseOptionalInt(etAsRaw);
                Integer penHs    = parseOptionalInt(penHsRaw);
                Integer penAs    = parseOptionalInt(penAsRaw);
                String decidedBy = decidedByRaw.trim();

                matchService.forceUpdateResult(
                        authBean.getUser(), matchId,
                        hs, as, etHs, etAs, penHs, penAs, decidedBy);

                activityLogService.log("RES-SAV",
                        "RES-SAV | user=" + username + " | matchId=" + matchId
                                + " | score=" + hs + "-" + as + " | decidedBy=" + decidedBy,
                        username);
            } else {
                matchService.forceUpdateResult(authBean.getUser(), matchId, hs, as);
                activityLogService.log("RES-SAV",
                        "RES-SAV | user=" + username + " | matchId=" + matchId
                                + " | score=" + hs + "-" + as,
                        username);
            }

            addMessage(FacesMessage.SEVERITY_INFO, "Result recorded successfully");
            loadMatches();

        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }
    }

    private void addMessage(FacesMessage.Severity severity, String summary) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(severity, summary, null));
    }

    private boolean isNullOrEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private Integer parseOptionalInt(String raw) {
        if (isNullOrEmpty(raw)) return null;
        try { return Integer.parseInt(raw.trim()); }
        catch (NumberFormatException e) { return null; }
    }

    public String getTeamInitials(String teamName) {
        if (teamName == null || teamName.isEmpty()) return "???";
        String[] parts = teamName.trim().split("\\s+");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        }
        return teamName.substring(0, Math.min(teamName.length(), 3)).toUpperCase();
    }

    public List<String> getTeamNames()              { return TEAM_NAMES; }
    public List<MatchAdminRow> getMatchRows()        { return matchRows; }
    public String getHomeTeam()                      { return homeTeam; }
    public void   setHomeTeam(String homeTeam)       { this.homeTeam = homeTeam; }
    public String getAwayTeam()                      { return awayTeam; }
    public void   setAwayTeam(String awayTeam)       { this.awayTeam = awayTeam; }
    public String getKickoffDate()                   { return kickoffDate; }
    public void   setKickoffDate(String kickoffDate) { this.kickoffDate = kickoffDate; }

    public static class MatchAdminRow implements Serializable {
        private static final long serialVersionUID = 1L;
        private final Match match;

        MatchAdminRow(Match match) { this.match = match; }

        public Long  getMatchId() { return match.getId(); }
        public Match getMatch()   { return match; }
    }
}