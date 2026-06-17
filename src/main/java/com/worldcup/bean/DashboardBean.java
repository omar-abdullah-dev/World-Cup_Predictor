package com.worldcup.bean;

import com.worldcup.model.User;
import com.worldcup.service.MatchService;
import com.worldcup.service.PredictionService;
import com.worldcup.service.UserService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.List;

/**
 * Manages dashboard overview statistics.
 * Provides summary cards for homepage: total users, matches, predictions, top user.
 */
@Named
@RequestScoped
public class DashboardBean {

    @Inject private UserService userService;
    @Inject private MatchService matchService;
    @Inject private PredictionService predictionService;

    public int getTotalUsers() {
        return userService.getAllUsers().size();
    }

    public int getTotalMatches() {
        return matchService.getAllMatches().size();
    }

    public int getTotalPredictions() {
        return predictionService.getAllPredictions().size();
    }

    public User getTopUser() {
        List<User> leaderboard = userService.getLeaderboard();
        return leaderboard.isEmpty() ? null : leaderboard.get(0);
    }

    public int getFinishedMatches() {
        return matchService.getFinishedMatches().size();
    }

    public int getScheduledMatches() {
        return matchService.getScheduledMatches().size();
    }
}

