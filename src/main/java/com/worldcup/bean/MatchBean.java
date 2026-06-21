package com.worldcup.bean;

import com.worldcup.model.Match;
import com.worldcup.model.MatchStatus;
import com.worldcup.security.SecurityException;
import com.worldcup.service.MatchService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Named
@ViewScoped
public class MatchBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(MatchBean.class.getName());

    private static final ZoneId EGYPT = ZoneId.of("Africa/Cairo");
    private static final DateTimeFormatter EGYPT_DISPLAY =
            DateTimeFormatter.ofPattern("dd MMM yyyy - HH:mm");
    private static final DateTimeFormatter EGYPT_DISPLAY_DOT =
            DateTimeFormatter.ofPattern("dd MMM yyyy \u00b7 HH:mm");

    private static final List<String> TEAM_NAMES = new ArrayList<String>(Arrays.asList(
            "Algeria", "Argentina", "Australia", "Austria", "Belgium",
            "Bosnia & Herzegovina", "Brazil", "Canada", "Cape Verde", "Colombia",
            "Croatia", "Curacao", "Czech Republic", "DR Congo", "Ecuador",
            "Egypt", "England", "France", "Germany", "Ghana",
            "Haiti", "Iran", "Iraq", "Ivory Coast", "Japan",
            "Jordan", "Mexico", "Morocco", "Netherlands", "New Zealand",
            "Norway", "Panama", "Paraguay", "Portugal", "Qatar",
            "Saudi Arabia", "Scotland", "Senegal", "South Africa", "South Korea",
            "Spain", "Sweden", "Switzerland", "Tunisia", "Turkey",
            "Uruguay", "USA", "Uzbekistan"
    ));

    private String homeTeam;
    private String awayTeam;
    private String kickoffDate;
    private String errorMessage;
    private String successMessage;
    private boolean showCreateForm;
    private List<MatchDashboardRow> matchRows = new ArrayList<>();

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @Inject private MatchService matchService;
    @Inject private AuthBean authBean;

    @PostConstruct
    public void init() {
        refreshMatchRows();
    }

    public String createMatch() {
        errorMessage = null;
        successMessage = null;
        try {
            LocalDateTime kickoff = LocalDateTime.parse(kickoffDate, DATE_FMT);
            matchService.createMatch(authBean.getUser(), homeTeam, awayTeam, kickoff);
            successMessage = "Match " + homeTeam + " vs " + awayTeam + " created!";
            homeTeam = awayTeam = kickoffDate = null;
            showCreateForm = false;
            refreshMatchRows();
        } catch (SecurityException e) {
            errorMessage = "Authorization error: " + e.getMessage();
        } catch (IllegalArgumentException e) {
            errorMessage = e.getMessage();
        } catch (Exception e) {
            errorMessage = "Invalid date format. Please use the date-time picker.";
        }
        return null;
    }

    public String submitResultFromRow(Long matchId) {
        errorMessage = null;
        successMessage = null;
        return saveResult(matchId, false);
    }

    public String forceResultFromRow(Long matchId) {
        errorMessage = null;
        successMessage = null;
        return saveResult(matchId, true);
    }

    private String saveResult(Long matchId, boolean force) {
        if (matchId == null) {
            errorMessage = "Invalid match selection.";
            return null;
        }

        ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
        Map<String, String> params = ec.getRequestParameterMap();
        String hsRaw = params.get("hs_" + matchId);
        String asRaw = params.get("as_" + matchId);

        if (hsRaw == null || hsRaw.trim().isEmpty() || asRaw == null || asRaw.trim().isEmpty()) {
            errorMessage = "Please enter both scores before saving the result.";
            return null;
        }

        int hs, as;
        try {
            hs = Integer.parseInt(hsRaw.trim());
            as = Integer.parseInt(asRaw.trim());
        } catch (NumberFormatException e) {
            errorMessage = "Scores must be whole numbers.";
            return null;
        }

        if (hs < 0 || as < 0) {
            errorMessage = "Scores cannot be negative.";
            return null;
        }

        try {
            Match match = matchService.getMatch(matchId);
            if (force) {
                matchService.forceUpdateResult(authBean.getUser(), matchId, hs, as);
            } else {
                matchService.updateResult(authBean.getUser(), matchId, hs, as);
            }
            successMessage = "Result recorded for "
                    + match.getHomeTeam() + " vs " + match.getAwayTeam()
                    + " (" + hs + " – " + as + "). Predictions recalculated!";
            refreshMatchRows();
        } catch (SecurityException e) {
            errorMessage = "Authorization error: " + e.getMessage();
        } catch (IllegalArgumentException e) {
            errorMessage = e.getMessage();
        }
        return null;
    }

    public String deleteMatch(Long matchId) {
        errorMessage = null;
        successMessage = null;
        try {
            Match match = matchService.getMatch(matchId);
            matchService.deleteMatch(authBean.getUser(), matchId);
            successMessage = "Match " + match.getHomeTeam() + " vs " + match.getAwayTeam() + " deleted.";
            refreshMatchRows();
        } catch (SecurityException e) {
            errorMessage = "Authorization error: " + e.getMessage();
        } catch (IllegalArgumentException e) {
            errorMessage = e.getMessage();
        }
        return null;
    }

    public String toggleCreateForm() {
        showCreateForm = !showCreateForm;
        return null;
    }

    public List<Match> getAllMatches() {
        return matchService.getAllMatches();
    }

    public List<MatchDashboardRow> getMatchRows() { return matchRows; }
    public int getTotalMatchCount()               { return matchRows.size(); }
    public List<String> getTeamNames()            { return TEAM_NAMES; }

    public String formatMatchKickoff(LocalDateTime dt) {
        if (dt == null) return "";
        ZonedDateTime egypt = dt.atZone(ZoneId.of("UTC")).withZoneSameInstant(EGYPT);
        return egypt.format(EGYPT_DISPLAY) + " (Cairo)";
    }

    public String formatPredictionKickoff(LocalDateTime dt) {
        if (dt == null) return "";
        ZonedDateTime egypt = dt.atZone(ZoneId.of("UTC")).withZoneSameInstant(EGYPT);
        return egypt.format(EGYPT_DISPLAY_DOT) + " (Cairo)";
    }

    public String formatDateTime(LocalDateTime dt) {
        if (dt == null) return "";
        ZonedDateTime egypt = dt.atZone(ZoneId.of("UTC")).withZoneSameInstant(EGYPT);
        return egypt.format(EGYPT_DISPLAY);
    }

    public String getStatusBarText(Match match) {
        if (match == null) return "";
        if (match.isFinished()) {
            if (authBean != null && authBean.isAdmin() && match.canEditResult()) {
                return "Result entered. Editable within " + Match.RESULT_WINDOW_HOURS + "h";
            }
            return "Finished";
        }
        if (match.hasStarted()) return "Live / In Progress";
        return "Scheduled";
    }

    public String getStatusBarClass(Match match) {
        if (match == null) return "match-dash-status";
        if (match.isFinished()) {
            if (match.canEditResult()) return "match-dash-status match-dash-status-live";
            return "match-dash-status match-dash-status-finished";
        }
        if (match.canRecordResult()) return "match-dash-status match-dash-status-live";
        if (match.hasStarted())      return "match-dash-status match-dash-status-live";
        return "match-dash-status match-dash-status-scheduled";
    }

    public String getTeamInitials(String teamName) {
        if (teamName == null || teamName.trim().isEmpty()) return "??";
        String[] parts = teamName.trim().split("\\s+");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        }
        String name = teamName.trim();
        return name.length() >= 2 ? name.substring(0, 2).toUpperCase() : name.toUpperCase();
    }

    public String getFlagForTeam(String teamName) {
        if (teamName == null) return "\uD83C\uDFF3";
        String t = teamName.trim().toLowerCase();
        if ("brazil".equals(t))          return "\uD83C\uDDE7\uD83C\uDDF7";
        if ("argentina".equals(t))       return "\uD83C\uDDE6\uD83C\uDDF7";
        if ("france".equals(t))          return "\uD83C\uDDEB\uD83C\uDDF7";
        if ("germany".equals(t))         return "\uD83C\uDDE9\uD83C\uDDEA";
        if ("spain".equals(t))           return "\uD83C\uDDEA\uD83C\uDDF8";
        if ("portugal".equals(t))        return "\uD83C\uDDF5\uD83C\uDDF9";
        if ("netherlands".equals(t))     return "\uD83C\uDDF3\uD83C\uDDF1";
        if ("italy".equals(t))           return "\uD83C\uDDEE\uD83C\uDDF9";
        if ("senegal".equals(t))         return "\uD83C\uDDF8\uD83C\uDDF3";
        if ("qatar".equals(t))           return "\uD83C\uDDF6\uD83C\uDDE6";
        if ("ecuador".equals(t))         return "\uD83C\uDDEA\uD83C\uDDE8";
        if ("usa".equals(t))             return "\uD83C\uDDFA\uD83C\uDDF8";
        if ("japan".equals(t))           return "\uD83C\uDDEF\uD83C\uDDF5";
        if ("mexico".equals(t))          return "\uD83C\uDDF2\uD83C\uDDFD";
        if ("saudi arabia".equals(t))    return "\uD83C\uDDF8\uD83C\uDDE6";
        if ("poland".equals(t))          return "\uD83C\uDDF5\uD83C\uDDF1";
        if ("iran".equals(t))            return "\uD83C\uDDEE\uD83C\uDDF7";
        if ("england".equals(t))         return "\uD83C\uDFF4";
        if ("belgium".equals(t))         return "\uD83C\uDDE7\uD83C\uDDEA";
        if ("croatia".equals(t))         return "\uD83C\uDDED\uD83C\uDDF7";
        if ("morocco".equals(t))         return "\uD83C\uDDF2\uD83C\uDDE6";
        if ("south korea".equals(t))     return "\uD83C\uDDF0\uD83C\uDDF7";
        if ("australia".equals(t))       return "\uD83C\uDDE6\uD83C\uDDFA";
        if ("canada".equals(t))          return "\uD83C\uDDE8\uD83C\uDDE6";
        if ("switzerland".equals(t))     return "\uD83C\uDDE8\uD83C\uDDED";
        if ("uruguay".equals(t))         return "\uD83C\uDDFA\uD83C\uDDFE";
        if ("egypt".equals(t))           return "\uD83C\uDDEA\uD83C\uDDEC";
        if ("ghana".equals(t))           return "\uD83C\uDDEC\uD83C\uDDED";
        if ("norway".equals(t))          return "\uD83C\uDDF3\uD83C\uDDF4";
        if ("sweden".equals(t))          return "\uD83C\uDDF8\uD83C\uDDEA";
        if ("turkey".equals(t))          return "\uD83C\uDDF9\uD83C\uDDF7";
        if ("colombia".equals(t))        return "\uD83C\uDDE8\uD83C\uDDF4";
        if ("south africa".equals(t))    return "\uD83C\uDDFF\uD83C\uDDE6";
        if ("scotland".equals(t))        return "\uD83C\uDFF4";
        if ("algeria".equals(t))         return "\uD83C\uDDE9\uD83C\uDDFF";
        if ("austria".equals(t))         return "\uD83C\uDDE6\uD83C\uDDF9";
        if ("tunisia".equals(t))         return "\uD83C\uDDF9\uD83C\uDDF3";
        if ("iraq".equals(t))            return "\uD83C\uDDEE\uD83C\uDDF6";
        if ("jordan".equals(t))          return "\uD83C\uDDEF\uD83C\uDDF4";
        if ("paraguay".equals(t))        return "\uD83C\uDDF5\uD83C\uDDFE";
        if ("ivory coast".equals(t))     return "\uD83C\uDDE8\uD83C\uDDEE";
        if ("cape verde".equals(t))      return "\uD83C\uDDE8\uD83C\uDDFB";
        if ("new zealand".equals(t))     return "\uD83C\uDDF3\uD83C\uDDFF";
        if ("panama".equals(t))          return "\uD83C\uDDF5\uD83C\uDDE6";
        if ("haiti".equals(t))           return "\uD83C\uDDED\uD83C\uDDF9";
        if ("uzbekistan".equals(t))      return "\uD83C\uDDFA\uD83C\uDDFF";
        if ("dr congo".equals(t))        return "\uD83C\uDDE8\uD83C\uDDE9";
        if ("bosnia & herzegovina".equals(t)) return "\uD83C\uDDE7\uD83C\uDDE6";
        if ("czech republic".equals(t))  return "\uD83C\uDDE8\uD83C\uDDFF";
        return "\uD83C\uDFF3";
    }

    public boolean canDelete(Match match) {
        return match != null && match.getStatus() == MatchStatus.SCHEDULED && !match.hasStarted();
    }

    private void refreshMatchRows() {
        matchRows = new ArrayList<>();
        java.util.Set<Long> seen = new java.util.LinkedHashSet<>();
        matchService.getAllMatches().stream()
                .filter(m -> seen.add(m.getId()))
                .sorted(Comparator.comparing(Match::getKickoffDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(match -> matchRows.add(new MatchDashboardRow(match)));
    }

    public String getHomeTeam()              { return homeTeam; }
    public void setHomeTeam(String t)        { this.homeTeam = t; }
    public String getAwayTeam()              { return awayTeam; }
    public void setAwayTeam(String t)        { this.awayTeam = t; }
    public String getKickoffDate()           { return kickoffDate; }
    public void setKickoffDate(String d)     { this.kickoffDate = d; }
    public String getErrorMessage()          { return errorMessage; }
    public void setErrorMessage(String m)    { this.errorMessage = m; }
    public String getSuccessMessage()        { return successMessage; }
    public void setSuccessMessage(String m)  { this.successMessage = m; }
    public boolean isShowCreateForm()        { return showCreateForm; }
    public void setShowCreateForm(boolean s) { this.showCreateForm = s; }

    public static class MatchDashboardRow implements Serializable {
        private static final long serialVersionUID = 1L;
        private final Match match;
        private Integer homeScore;
        private Integer awayScore;

        MatchDashboardRow(Match match) {
            this.match = match;
            if (match.isFinished()) {
                this.homeScore = match.getHomeScore();
                this.awayScore = match.getAwayScore();
            }
        }

        public Long    getMatchId()              { return match.getId(); }
        public Match   getMatch()                { return match; }
        public Integer getHomeScore()            { return homeScore; }
        public void    setHomeScore(Integer s)   { this.homeScore = s; }
        public Integer getAwayScore()            { return awayScore; }
        public void    setAwayScore(Integer s)   { this.awayScore = s; }
        public boolean isCanRecordResult()       { return match.canRecordResult(); }
        public boolean isFinished()              { return match.isFinished(); }
        public boolean isDeletable()             { return match.getStatus() == MatchStatus.SCHEDULED && !match.hasStarted(); }
    }
}