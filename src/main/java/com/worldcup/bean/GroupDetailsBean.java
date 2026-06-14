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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Backing bean for the Group Details screen.
 * Uses row DTOs for score inputs to avoid JSF Map-key binding issues.
 */
@Named
@ViewScoped
public class GroupDetailsBean implements Serializable {

    private static final long serialVersionUID = 1L;


    @Inject private MatchService matchService;
    @Inject private PredictionService predictionService;
    @Inject private UserSessionBean userSessionBean;

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
        String normalized = group == null || group.isBlank()
                ? null
                : group.trim().toUpperCase(Locale.ROOT);
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

        if (!userSessionBean.isUserSelected()) {
            errorMessage = "Please select a user before making predictions.";
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
            User user = userSessionBean.getSelectedUser();
            boolean updating = predictionService.hasPredictionForMatch(user.getId(), matchId);
            predictionService.submitPrediction(
                    user.getId(),
                    matchId,
                    predHome,
                    predAway
            );
            row.refreshPrediction();
            successMessage = updating
                    ? "Prediction updated for " + user.getUsername() + "."
                    : "Prediction saved for " + user.getUsername() + ".";
        } catch (IllegalArgumentException e) {
            errorMessage = e.getMessage();
        }

        return null;
    }

    public Prediction getUserPrediction(Long matchId) {
        if (!userSessionBean.isUserSelected() || matchId == null) {
            return null;
        }
        return predictionService.getPredictionsByUser(userSessionBean.getSelectedUser().getId()).stream()
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
        if (groupMissing) {
            return "Group";
        }
        return group == null || group.isBlank() ? "Group" : "Group " + group.toUpperCase(Locale.ROOT);
    }

    public boolean isUserSelected() {
        return userSessionBean.isUserSelected();
    }

    public boolean isGroupMissing() {
        return groupMissing;
    }

    public String getSelectedUsername() {
        return userSessionBean.getSelectedUsername();
    }

    public String getGroup() {
        return group;
    }

    public String formatDateTime(java.time.LocalDateTime dt) {
        return dt == null ? "" : dt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getSuccessMessage() {
        return successMessage;
    }

    private void resolveGroupFromRequest() {
        if (group != null && !group.isBlank()) {
            groupMissing = false;
            return;
        }
        FacesContext context = FacesContext.getCurrentInstance();
        if (context == null) {
            groupMissing = true;
            return;
        }
        String groupParam = context.getExternalContext().getRequestParameterMap().get("group");
        if (groupParam != null && !groupParam.isBlank()) {
            this.group = groupParam.trim().toUpperCase(Locale.ROOT);
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
        matchRows = new ArrayList<>();
        if (group == null || group.isBlank()) {
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
