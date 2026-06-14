package com.worldcup.service;

import com.worldcup.model.Team;
import com.worldcup.model.User;
import com.worldcup.repository.TeamRepository;
import com.worldcup.security.SecurityService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class TeamService {

    private TeamRepository teamRepository;

    protected TeamService() {}

    @Inject
    public TeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    public List<Team> getAllTeams() {
        return teamRepository.findAll();
    }

    public Team getTeam(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));
    }

    public Team createTeam(User adminUser, Team team) {
        SecurityService.assertAdmin(adminUser, "create team");
        if (teamRepository.findByName(team.getName()).isPresent()) {
            throw new IllegalArgumentException("Team with this name already exists");
        }
        return teamRepository.save(team);
    }

    public Team updateTeam(User adminUser, Team team) {
        SecurityService.assertAdmin(adminUser, "update team");
        Team existing = getTeam(team.getId());
        existing.setName(team.getName());
        existing.setShortCode(team.getShortCode());
        existing.setLogoPath(team.getLogoPath());
        existing.setFlagEmoji(team.getFlagEmoji());
        return teamRepository.update(existing);
    }

    public void deleteTeam(User adminUser, Long id) {
        SecurityService.assertAdmin(adminUser, "delete team");
        boolean deleted = teamRepository.deleteById(id);
        if (!deleted) {
            throw new IllegalArgumentException("Team not found");
        }
    }
}
