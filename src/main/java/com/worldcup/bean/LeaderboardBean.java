package com.worldcup.bean;

import com.worldcup.model.User;
import com.worldcup.service.UserService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.ArrayList;
import java.util.List;

@Named
@RequestScoped
public class LeaderboardBean {

    @Inject private UserService userService;

    public List<User> getLeaderboard()    { return userService.getLeaderboard(); }

    public User getLeader() {
        List<User> lb = getLeaderboard();
        return lb.isEmpty() ? null : lb.get(0);
    }

    public List<User> getPodium() {
        List<User> lb = getLeaderboard();
        List<User> podium = new ArrayList<>();
        if (lb.size() > 0) podium.add(lb.get(0));
        if (lb.size() > 1) podium.add(lb.get(1));
        if (lb.size() > 2) podium.add(lb.get(2));
        return podium;
    }

    public String getPodiumMedal(int index) {
        if (index == 0) return "\uD83E\uDD47";
        if (index == 1) return "\uD83E\uDD48";
        if (index == 2) return "\uD83E\uDD49";
        return "";
    }

    public int getTotalParticipants() { return userService.getAllUsers().size(); }
}
