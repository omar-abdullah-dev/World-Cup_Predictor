package com.worldcup.bean.admin;

import com.worldcup.bean.AuthBean;
import com.worldcup.model.Group;
import com.worldcup.model.RoundStatus;
import com.worldcup.model.Team;
import com.worldcup.model.TournamentRound;
import com.worldcup.service.ActivityLogService;
import com.worldcup.service.GroupService;
import com.worldcup.service.MatchService;
import com.worldcup.service.TeamService;
import com.worldcup.service.TournamentRoundService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Named
@ViewScoped
public class AdminGroupBean implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Inject private GroupService groupService;
    @Inject private TeamService teamService;
    @Inject private TournamentRoundService roundService;
    @Inject private MatchService matchService;
    @Inject private AuthBean authBean;
    @Inject private ActivityLogService activityLogService;

    private List<Group> groups;
    private List<Team> allTeams;
    private List<TournamentRound> allRounds;

    private Group currentGroup;
    private Long selectedRoundId;
    
    // Using simple Team list for legacy support if needed
    private List<Long> selectedTeamIds;

    // Fixture pairs: Team A vs Team B
    private List<FixtureEntry> fixtures;

    @PostConstruct
    public void init() {
        loadGroups();
        allTeams = teamService.getAllTeams();
        allRounds = roundService.getAllRounds();
    }

    public void loadGroups() {
        groups = groupService.getAllGroups();
    }

    public void prepareCreate() {
        currentGroup = new Group();
        selectedRoundId = null;
        selectedTeamIds = new ArrayList<>();
        fixtures = new ArrayList<>();
        // Pre-populate with 6 empty fixtures (typical for 4 team group)
        for(int i = 0; i < 6; i++) {
            fixtures.add(new FixtureEntry());
        }
    }

    public void addFixtureRow() {
        fixtures.add(new FixtureEntry());
    }
    
    public void removeFixtureRow(FixtureEntry entry) {
        fixtures.remove(entry);
    }

    public void prepareEdit(Group group) {
        this.currentGroup = group;
        this.selectedRoundId = group.getRound() != null ? group.getRound().getId() : null;
        this.selectedTeamIds = group.getTeams().stream()
                .map(Team::getId)
                .collect(Collectors.toList());
        this.fixtures = new ArrayList<>(); // Existing matches are handled in Match page for now
    }

    public void save() {
        try {
            // 1. Assign Round
            if (selectedRoundId != null) {
                TournamentRound round = roundService.getRound(selectedRoundId);
                currentGroup.setRound(round);
            }

            // 2. Validate Fixtures and gather unique teams
            List<Team> groupTeams = new ArrayList<>();
            for (FixtureEntry f : fixtures) {
                if (f.getTeamAId() != null && f.getTeamBId() != null) {
                    if (f.getTeamAId().equals(f.getTeamBId())) {
                        throw new IllegalArgumentException("A team cannot play against itself.");
                    }
                    Team teamA = teamService.getTeam(f.getTeamAId());
                    Team teamB = teamService.getTeam(f.getTeamBId());
                    if (!groupTeams.contains(teamA)) groupTeams.add(teamA);
                    if (!groupTeams.contains(teamB)) groupTeams.add(teamB);
                }
            }
            
            // If legacy selectedTeamIds is used, combine them
            if (selectedTeamIds != null) {
                for (Long id : selectedTeamIds) {
                    Team t = teamService.getTeam(id);
                    if (!groupTeams.contains(t)) groupTeams.add(t);
                }
            }

            currentGroup.setTeams(groupTeams);

            // 3. Save Group
            boolean isNew = (currentGroup.getId() == null);
            if (isNew) {
                currentGroup.setStatus(RoundStatus.OPEN);
                currentGroup = groupService.createGroup(authBean.getUser(), currentGroup);
                String username = authBean.getUser().getUsername();
                activityLogService.log("GRP-CRE",
                        "GRP-CRE | screen=admin-groups.xhtml | user=" + username
                        + " | detail=Group '" + currentGroup.getName() + "' created",
                        username);
                addMessage(FacesMessage.SEVERITY_INFO, "Group created successfully.");
            } else {
                currentGroup = groupService.updateGroup(authBean.getUser(), currentGroup);
                String username = authBean.getUser().getUsername();
                activityLogService.log("GRP-UPD",
                        "GRP-UPD | screen=admin-groups.xhtml | user=" + username
                        + " | detail=Group '" + currentGroup.getName() + "' updated",
                        username);
                addMessage(FacesMessage.SEVERITY_INFO, "Group updated successfully.");
            }

            // 4. Create Matches from Fixtures
            if (isNew) {
                int matchCount = 0;
                for (FixtureEntry f : fixtures) {
                    if (f.getTeamAId() != null && f.getTeamBId() != null) {
                        Team teamA = teamService.getTeam(f.getTeamAId());
                        Team teamB = teamService.getTeam(f.getTeamBId());
                        
                        // Set kickoff sequentially for generated fixtures (placeholder times)
                        LocalDateTime kickoff = LocalDateTime.now().plusDays(10).plusDays(matchCount);
                        matchService.createMatch(authBean.getUser(), teamA, teamB, currentGroup, kickoff);
                        matchCount++;
                    }
                }
                if (matchCount > 0) {
                    addMessage(FacesMessage.SEVERITY_INFO, matchCount + " fixtures generated.");
                }
            }

            loadGroups();
            currentGroup = null; // Close form
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }
    }

    public void deleteGroup(Long id) {
        try {
            groupService.deleteGroup(authBean.getUser(), id);
            String username = authBean.getUser().getUsername();
            activityLogService.log("GRP-DEL",
                    "GRP-DEL | screen=admin-groups.xhtml | user=" + username
                    + " | detail=groupId=" + id + " deleted",
                    username);
            addMessage(FacesMessage.SEVERITY_INFO, "Group deleted successfully.");
            loadGroups();
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }
    }

    public void updateStatus(Long id, RoundStatus status) {
        try {
            groupService.updateGroupStatus(authBean.getUser(), id, status);
            String username = authBean.getUser().getUsername();
            activityLogService.log("GRP-UPD",
                    "GRP-UPD | screen=admin-groups.xhtml | user=" + username
                    + " | detail=groupId=" + id + " status changed to " + status,
                    username);
            addMessage(FacesMessage.SEVERITY_INFO, "Group status updated.");
            loadGroups();
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }
    }

    private void addMessage(FacesMessage.Severity severity, String summary) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, summary, null));
    }

    public List<Group> getGroups() { return groups; }
    public Group getCurrentGroup() { return currentGroup; }
    public void setCurrentGroup(Group currentGroup) { this.currentGroup = currentGroup; }
    public RoundStatus[] getStatuses() { return RoundStatus.values(); }
    public List<Team> getAllTeams() { return allTeams; }
    public List<TournamentRound> getAllRounds() { return allRounds; }
    public Long getSelectedRoundId() { return selectedRoundId; }
    public void setSelectedRoundId(Long selectedRoundId) { this.selectedRoundId = selectedRoundId; }
    public List<Long> getSelectedTeamIds() { return selectedTeamIds; }
    public void setSelectedTeamIds(List<Long> selectedTeamIds) { this.selectedTeamIds = selectedTeamIds; }
    public List<FixtureEntry> getFixtures() { return fixtures; }

    public static class FixtureEntry implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long teamAId;
        private Long teamBId;

        public Long getTeamAId() { return teamAId; }
        public void setTeamAId(Long teamAId) { this.teamAId = teamAId; }
        public Long getTeamBId() { return teamBId; }
        public void setTeamBId(Long teamBId) { this.teamBId = teamBId; }
    }
}
