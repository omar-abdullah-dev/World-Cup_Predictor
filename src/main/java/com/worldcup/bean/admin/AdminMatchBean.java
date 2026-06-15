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
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Named
@ViewScoped
public class AdminMatchBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(AdminMatchBean.class.getName());

    @Inject private MatchService matchService;
    @Inject private AuthBean authBean;
    @Inject private ActivityLogService activityLogService;

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
        if (authBean.getUser() == null) {
            matchRows = new ArrayList<>();
            return;
        }
        matchRows = new ArrayList<>();
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

    /**
     * Reads homeScore and awayScore directly from the raw HTTP request parameter map,
     * keyed as "hs_<matchId>" and "as_<matchId>".
     *
     * Why: JSF EL cannot write back into a Map<Long,Integer> inside ui:repeat —
     * during Apply Request Values the key arrives as a String and the put never fires.
     * Reading raw params bypasses the JSF binding phase entirely and is always reliable.
     */
    public void forceSubmitResult(Long matchId) {
        if (matchId == null) {
            LOG.warning("[forceSubmitResult] Rejected: matchId is null.");
            addMessage(FacesMessage.SEVERITY_ERROR, "Invalid match selection.");
            return;
        }

        ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
        Map<String, String> params = ec.getRequestParameterMap();

        String hsRaw = params.get("hs_" + matchId);
        String asRaw = params.get("as_" + matchId);

        LOG.info("[forceSubmitResult] matchId=" + matchId
                + " | raw params hs_" + matchId + "='" + hsRaw
                + "' as_" + matchId + "='" + asRaw + "'");

        if (hsRaw == null || hsRaw.isBlank() || asRaw == null || asRaw.isBlank()) {
            LOG.warning("[forceSubmitResult] Rejected for matchId=" + matchId
                    + " — one or both score params missing/blank."
                    + " hs_" + matchId + "='" + hsRaw + "'"
                    + " as_" + matchId + "='" + asRaw + "'");
            addMessage(FacesMessage.SEVERITY_ERROR, "Please enter both scores.");
            return;
        }

        Integer hs, as;
        try {
            hs = Integer.parseInt(hsRaw.trim());
            as = Integer.parseInt(asRaw.trim());
        } catch (NumberFormatException e) {
            LOG.warning("[forceSubmitResult] Rejected for matchId=" + matchId
                    + " — non-numeric score: hs='" + hsRaw + "' as='" + asRaw + "'");
            addMessage(FacesMessage.SEVERITY_ERROR, "Scores must be whole numbers.");
            return;
        }

        if (hs < 0 || as < 0) {
            LOG.warning("[forceSubmitResult] Rejected for matchId=" + matchId
                    + " — negative score: hs=" + hs + " as=" + as);
            addMessage(FacesMessage.SEVERITY_ERROR, "Scores cannot be negative.");
            return;
        }

        try {
            matchService.forceUpdateResult(authBean.getUser(), matchId, hs, as);
            LOG.info("[forceSubmitResult] Result saved: matchId=" + matchId
                    + " score=" + hs + "-" + as);
            String username = authBean.getUser().getUsername();
            activityLogService.log("RES-SAV",
                    "RES-SAV | screen=admin-matches.xhtml | user=" + username
                    + " | detail=matchId=" + matchId + " score=" + hs + "-" + as,
                    username);
            addMessage(FacesMessage.SEVERITY_INFO, "Result recorded successfully");
            loadMatches();
        } catch (Exception e) {
            LOG.warning("[forceSubmitResult] Service error for matchId=" + matchId
                    + ": " + e.getMessage());
            addMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }
    }

    public void deleteMatch(Long matchId) {
        try {
            matchService.deleteMatch(authBean.getUser(), matchId);
            String username = authBean.getUser().getUsername();
            activityLogService.log("MATCH-DEL",
                    "MATCH-DEL | screen=admin-matches.xhtml | user=" + username
                    + " | detail=matchId=" + matchId,
                    username);
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
        return teamName.substring(0, Math.min(teamName.length(), 3)).toUpperCase();
    }

    public List<MatchAdminRow> getMatchRows() { return matchRows; }
    public String getHomeTeam() { return homeTeam; }
    public void setHomeTeam(String homeTeam) { this.homeTeam = homeTeam; }
    public String getAwayTeam() { return awayTeam; }
    public void setAwayTeam(String awayTeam) { this.awayTeam = awayTeam; }
    public String getKickoffDate() { return kickoffDate; }
    public void setKickoffDate(String kickoffDate) { this.kickoffDate = kickoffDate; }

    // -----------------------------------------------------------------------
    // Inner row — read-only wrapper; no mutable score fields here
    // -----------------------------------------------------------------------
    public static class MatchAdminRow implements Serializable {
        private static final long serialVersionUID = 1L;
        private final Match match;

        MatchAdminRow(Match match) { this.match = match; }

        public Long getMatchId() { return match.getId(); }
        public Match getMatch()  { return match; }
    }
}
