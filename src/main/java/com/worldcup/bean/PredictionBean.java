package com.worldcup.bean;

import com.worldcup.model.Match;
import com.worldcup.model.Prediction;
import com.worldcup.model.User;
import com.worldcup.service.MatchService;
import com.worldcup.service.PredictionService;
import com.worldcup.service.UserService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Backing bean for the Predictions screen.
 * Uses per-match rows for score inputs to keep JSF binding stable across the match loop.
 */
@Named
@ViewScoped
public class PredictionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private Long matchId;
    private Integer predictedHomeScore;
    private Integer predictedAwayScore;
    private String errorMessage;
    private String successMessage;
    private List<MatchPredictionRow> upcomingRows = new ArrayList<>();

    @Inject private PredictionService predictionService;
    @Inject private UserService userService;
    @Inject private MatchService matchService;
    @Inject private AuthBean authBean;

    @PostConstruct
    public void init() {
        if (authBean != null && authBean.isLoggedIn()) {
            userId = authBean.getCurrentUserId();
        }
        refreshUpcomingRows();
    }

    public void onUserChange() {
        refreshUpcomingRows();
    }

    public String submitPredictionDirect(Long matchId, Integer homeScore, Integer awayScore) {
        errorMessage = null;
        successMessage = null;

        Long activeUserId = resolveActiveUserId();
        if (activeUserId == null) {
            errorMessage = "Please login first to submit predictions.";
            return null;
        }
        if (matchId == null) {
            errorMessage = "Match not found.";
            return null;
        }
        if (homeScore == null || awayScore == null) {
            errorMessage = "Please enter both scores.";
            return null;
        }

        try {
            boolean updating = predictionService.hasPredictionForMatch(activeUserId, matchId);
            predictionService.submitPrediction(activeUserId, matchId, homeScore, awayScore);
            User user = userService.getUser(activeUserId);
            Match match = matchService.getMatch(matchId);
            String action = updating ? "updated prediction" : "predicts";
            successMessage = "✓ " + user.getUsername() + " " + action + " "
                    + match.getHomeTeam() + " "
                    + homeScore + " – " + awayScore + " "
                    + match.getAwayTeam();
            matchId = null;
            predictedHomeScore = predictedAwayScore = null;
            refreshUpcomingRows();
        } catch (IllegalArgumentException e) {
            errorMessage = e.getMessage();
        } catch (Exception e) {
            errorMessage = "An unexpected error occurred: " + e.getMessage();
        }
        return null;
    }

    public String submitPredictionFromLoop(MatchPredictionRow row) {
        if (row == null) {
            return submitPredictionDirect(null, null, null);
        }
        return submitPredictionDirect(row.getMatchId(), row.getHomeScore(), row.getAwayScore());
    }

    public String submitPrediction() {
        return submitPredictionDirect(matchId, predictedHomeScore, predictedAwayScore);
    }

    public List<Prediction> getAllPredictions() {
        return predictionService.getAllPredictions();
    }

    public List<Prediction> getUserPredictions() {
        Long activeUserId = resolveActiveUserId();
        if (activeUserId == null) {
            return List.of();
        }
        return predictionService.getPredictionsByUser(activeUserId);
    }

    public boolean isUserSelected() {
        return resolveActiveUserId() != null;
    }

    public String getSelectedUsername() {
        Long activeUserId = resolveActiveUserId();
        if (activeUserId == null) {
            return "Not Selected";
        }
        return getUsername(activeUserId);
    }

    private Long resolveActiveUserId() {
        if (authBean != null && authBean.isLoggedIn()) {
            return authBean.getCurrentUserId();
        }
        return userId;
    }

    public List<User> getUsers() {
        return userService.getAllUsers();
    }

    public List<Match> getScheduledMatches() {
        return matchService.getPredictableMatches();
    }

    public List<Match> getStartedMatches() {
        return matchService.getScheduledMatches().stream()
                .filter(m -> m.hasStarted() && !m.isFinished())
                .toList();
    }

    public boolean isPredictionOpen(Match match) {
        return match != null && match.isPredictionOpen();
    }

    public boolean hasPredictionForMatch(Long matchId) {
        Long activeUserId = resolveActiveUserId();
        return activeUserId != null && predictionService.hasPredictionForMatch(activeUserId, matchId);
    }

    public List<Match> getAllMatches() {
        return matchService.getAllMatches();
    }

    public List<Match> getFinishedMatches() {
        return matchService.getFinishedMatches();
    }

    public List<MatchPredictionRow> getUpcomingRows() {
        return upcomingRows;
    }

    public String getUsername(Long uid) {
        try {
            return userService.getUser(uid).getUsername();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public String getMatchDescription(Long mid) {
        try {
            Match m = matchService.getMatch(mid);
            return m.getHomeTeam() + " vs " + m.getAwayTeam();
        } catch (Exception e) {
            return "Unknown Match";
        }
    }

    public String getHomeFlag(Long mid) {
        try {
            return getFlag(matchService.getMatch(mid).getHomeTeam());
        } catch (Exception e) {
            return "🏳";
        }
    }

    public String getAwayFlag(Long mid) {
        try {
            return getFlag(matchService.getMatch(mid).getAwayTeam());
        } catch (Exception e) {
            return "🏳";
        }
    }

    public String getFlagForTeam(String teamName) {
        return getFlag(teamName);
    }

    public String formatPredictionKickoff(LocalDateTime kickoff) {
        return kickoff == null ? "" : kickoff.format(DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm"));
    }

    public String getTeamInitials(String teamName) {
        if (teamName == null || teamName.isBlank()) {
            return "??";
        }
        String[] parts = teamName.trim().split("\\s+");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        }
        String name = teamName.trim();
        return name.length() >= 2
                ? name.substring(0, 2).toUpperCase()
                : name.toUpperCase();
    }

    public Prediction getExistingPrediction(Long uid, Long mid) {
        if (uid == null || mid == null) {
            return null;
        }
        return predictionService.getAllPredictions().stream()
                .filter(p -> p.getUserId().equals(uid) && p.getMatchId().equals(mid))
                .findFirst()
                .orElse(null);
    }

    public String getPointsBadgeClass(int earnedPoints) {
        return switch (earnedPoints) {
            case 2 -> "points-exact";
            case 1 -> "points-outcome";
            default -> "points-zero";
        };
    }

    private void refreshUpcomingRows() {
        upcomingRows = new ArrayList<>();
        Long activeUserId = resolveActiveUserId();
        for (Match match : matchService.getPredictableMatches()) {
            MatchPredictionRow row = new MatchPredictionRow(match);
            Prediction existing = getExistingPrediction(activeUserId, match.getId());
            if (existing != null) {
                row.setHomeScore(existing.getPredictedHomeScore());
                row.setAwayScore(existing.getPredictedAwayScore());
            }
            upcomingRows.add(row);
        }
    }

    private String getFlag(String teamName) {
        if (teamName == null) {
            return "🏳";
        }
        return switch (teamName.trim().toLowerCase()) {
            case "brazil" -> "🇧🇷";
            case "argentina" -> "🇦🇷";
            case "france" -> "🇫🇷";
            case "germany" -> "🇩🇪";
            case "spain" -> "🇪🇸";
            case "england" -> "🏴󠁧󠁢󠁥󠁮󠁧󠁿";
            case "portugal" -> "🇵🇹";
            case "netherlands" -> "🇳🇱";
            case "italy" -> "🇮🇹";
            case "croatia" -> "🇭🇷";
            case "morocco" -> "🇲🇦";
            case "senegal" -> "🇸🇳";
            case "qatar" -> "🇶🇦";
            case "ecuador" -> "🇪🇨";
            case "usa", "united states" -> "🇺🇸";
            case "japan" -> "🇯🇵";
            case "south korea" -> "🇰🇷";
            case "australia" -> "🇦🇺";
            case "mexico" -> "🇲🇽";
            case "nigeria" -> "🇳🇬";
            case "cameroon" -> "🇨🇲";
            case "egypt" -> "🇪🇬";
            case "saudi arabia" -> "🇸🇦";
            case "tunisia" -> "🇹🇳";
            case "ghana" -> "🇬🇭";
            case "switzerland" -> "🇨🇭";
            case "belgium" -> "🇧🇪";
            case "denmark" -> "🇩🇰";
            case "poland" -> "🇵🇱";
            case "serbia" -> "🇷🇸";
            case "uruguay" -> "🇺🇾";
            case "canada" -> "🇨🇦";
            case "costa rica" -> "🇨🇷";
            case "iran" -> "🇮🇷";
            case "wales" -> "🏴󠁧󠁢󠁷󠁬󠁳󠁿";
            default -> "🏳";
        };
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long id) {
        this.userId = id;
        refreshUpcomingRows();
    }

    public Long getMatchId() {
        return matchId;
    }

    public void setMatchId(Long id) {
        this.matchId = id;
    }

    public Integer getPredictedHomeScore() {
        return predictedHomeScore;
    }

    public void setPredictedHomeScore(Integer score) {
        this.predictedHomeScore = score;
    }

    public Integer getPredictedAwayScore() {
        return predictedAwayScore;
    }

    public void setPredictedAwayScore(Integer score) {
        this.predictedAwayScore = score;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String message) {
        this.errorMessage = message;
    }

    public String getSuccessMessage() {
        return successMessage;
    }

    public void setSuccessMessage(String message) {
        this.successMessage = message;
    }

    public static class MatchPredictionRow implements Serializable {
        private static final long serialVersionUID = 1L;

        private final Match match;
        private Integer homeScore;
        private Integer awayScore;

        public MatchPredictionRow(Match match) {
            this.match = match;
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
    }
}
