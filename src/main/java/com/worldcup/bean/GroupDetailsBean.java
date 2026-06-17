package com.worldcup.bean;

import com.worldcup.model.Match;
import com.worldcup.model.Prediction;
import com.worldcup.model.User;
import com.worldcup.service.MatchService;
import com.worldcup.service.PredictionService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Backing bean for the Group Details screen.
 * Uses the logged-in AuthBean user for predictions.
 * Displays kickoff times in Egypt time (Africa/Cairo = UTC+3 in summer).
 */
@Named
@ViewScoped
public class GroupDetailsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Egypt Standard Time / EET — UTC+3 (no DST in summer). */
    private static final ZoneId EGYPT_ZONE = ZoneId.of("Africa/Cairo");
    private static final DateTimeFormatter EGYPT_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm");

    @Inject private MatchService matchService;
    @Inject private PredictionService predictionService;
    @Inject private AuthBean authBean;

    private String group;
    private List<MatchRow> matchRows = new ArrayList<>();
    private String errorMessage;
    private String successMessage;
    private boolean groupMissing;

    @PostConstruct
    public void init() {
        resolveGroupFromRequest();
        loadMatchRows();
    }

    public void setGroup(String group) {
        String normalized = (group == null || group.trim().isEmpty()) ? null : group.trim();
        if (java.util.Objects.equals(this.group, normalized)) {
            return;
        }
        this.group = normalized;
        this.groupMissing = normalized == null;
        loadMatchRows();
    }

    public String submitPrediction(Long matchId) {
        errorMessage = null;
        successMessage = null;

        MatchRow row = findRow(matchId);
        if (row == null) {
            errorMessage = "Invalid match selection.";
            return null;
        }

        if (!authBean.isLoggedIn()) {
            errorMessage = "You must be logged in to submit predictions.";
            return null;
        }

        Integer predHome = row.getHomeScore();
        Integer predAway = row.getAwayScore();

        if (predHome == null || predAway == null) {
            errorMessage = "Please enter both scores.";
            return null;
        }

        if (!row.getMatch().isPredictionOpen()) {
            errorMessage = "Cannot submit or change a prediction after the match has started.";
            return null;
        }

        try {
            User user = authBean.getUser();
            boolean updating = predictionService.hasPredictionForMatch(user.getId(), matchId);
            predictionService.submitPrediction(
                    user.getId(),
                    matchId,
                    predHome,
                    predAway
            );
            row.refreshPrediction();
            successMessage = updating
                    ? "Prediction updated."
                    : "Prediction saved.";
        } catch (IllegalArgumentException e) {
            errorMessage = e.getMessage();
        }

        return null;
    }

    public Prediction getUserPrediction(Long matchId) {
        if (!authBean.isLoggedIn() || matchId == null) {
            return null;
        }
        return predictionService.getPredictionsByUser(authBean.getCurrentUserId()).stream()
                .filter(p -> matchId.equals(p.getMatchId()))
                .findFirst()
                .orElse(null);
    }

    public List<MatchRow> getMatchRows() {
        return matchRows;
    }

    public List<Match> getMatches() {
        return matchRows.stream().map(MatchRow::getMatch).collect(Collectors.toList());
    }

    public String getGroupTitle() {
        if (groupMissing || group == null || group.trim().isEmpty()) {
            return "Group";
        }
        if (group.toLowerCase(Locale.ROOT).startsWith("group")) {
            return group;
        }
        return "Group " + group;
    }

    public boolean isUserSelected() {
        return authBean.isLoggedIn();
    }

    public boolean isGroupMissing() {
        return groupMissing;
    }

    public String getSelectedUsername() {
        return authBean.getCurrentUsername();
    }

    public String getGroup() {
        return group;
    }

    public String formatDateTime(java.time.LocalDateTime dt) {
        if (dt == null) return "";
        // Kickoff times are stored as UTC in the DB.
        // Convert to Egypt time (Africa/Cairo = UTC+3 in summer, no DST).
        ZonedDateTime egyptTime = dt.atZone(ZoneId.of("UTC")).withZoneSameInstant(EGYPT_ZONE);
        return egyptTime.format(EGYPT_FMT) + " (Cairo)";
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getSuccessMessage() {
        return successMessage;
    }

    private void resolveGroupFromRequest() {
        if (group != null && !group.trim().isEmpty()) {
            groupMissing = false;
            return;
        }
        FacesContext context = FacesContext.getCurrentInstance();
        if (context == null) {
            groupMissing = true;
            return;
        }
        String groupParam = context.getExternalContext().getRequestParameterMap().get("group");
        if (groupParam != null && !groupParam.trim().isEmpty()) {
            this.group = groupParam.trim();
            groupMissing = false;
        } else {
            groupMissing = true;
        }
    }

    private MatchRow findRow(Long matchId) {
        if (matchId == null) {
            return null;
        }
        return matchRows.stream()
                .filter(row -> matchId.equals(row.getMatchId()))
                .findFirst()
                .orElse(null);
    }

    private void loadMatchRows() {
        matchRows = new ArrayList<MatchRow>();
        if (group == null || group.trim().isEmpty()) {
            return;
        }

        List<Match> matches = matchService.getAllMatches().stream()
                .filter(match -> match.getGroup() != null && match.getGroup().getName().equalsIgnoreCase(group))
                .collect(Collectors.toList());

        for (Match match : matches) {
            matchRows.add(new MatchRow(match));
        }
    }

    /**
     * Per-match UI row for stable JSF binding (avoids Map[Long] EL issues).
     */
    public class MatchRow implements Serializable {
        private static final long serialVersionUID = 1L;

        private final Match match;
        private Integer homeScore;
        private Integer awayScore;

        MatchRow(Match match) {
            this.match = match;
            refreshPrediction();
        }

        void refreshPrediction() {
            Prediction prediction = getUserPrediction(match.getId());
            if (prediction != null) {
                homeScore = prediction.getPredictedHomeScore();
                awayScore = prediction.getPredictedAwayScore();
            }
        }

        public Long getMatchId() {
            return match.getId();
        }

        public Match getMatch() {
            return match;
        }

        public Integer getHomeScore() {
            return homeScore;
        }

        public void setHomeScore(Integer homeScore) {
            this.homeScore = homeScore;
        }

        public Integer getAwayScore() {
            return awayScore;
        }

        public void setAwayScore(Integer awayScore) {
            this.awayScore = awayScore;
        }

        public boolean isHasPrediction() {
            return getUserPrediction(match.getId()) != null;
        }

        public boolean isPredictionOpen() {
            return match.isPredictionOpen();
        }

        public boolean isPredictionLocked() {
            return match.hasStarted() && !match.isFinished();
        }

        public Prediction getPrediction() {
            return getUserPrediction(match.getId());
        }
    }
}
