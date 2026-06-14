package com.worldcup.bean.admin;

import com.worldcup.bean.AuthBean;
import com.worldcup.model.Group;
import com.worldcup.model.RoundStatus;
import com.worldcup.service.GroupService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

import com.worldcup.model.Team;
import com.worldcup.service.TeamService;

@Named
@ViewScoped
public class AdminGroupBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject private GroupService groupService;
    @Inject private TeamService teamService;
    @Inject private AuthBean authBean;

    private List<Group> groups;
    private List<Team> allTeams;
    private Group currentGroup;
    private List<Long> selectedTeamIds;

    @PostConstruct
    public void init() {
        loadGroups();
        allTeams = teamService.getAllTeams();
    }

    public void loadGroups() {
        groups = groupService.getAllGroups();
    }

    public void prepareCreate() {
        currentGroup = new Group();
        selectedTeamIds = new java.util.ArrayList<>();
    }

    public void prepareEdit(Group group) {
        this.currentGroup = group;
        this.selectedTeamIds = group.getTeams().stream()
                .map(team -> team.getId())
                .collect(java.util.stream.Collectors.toList());
    }

    public void save() {
        try {
            List<Team> selectedTeams = selectedTeamIds.stream()
                .map(id -> teamService.getTeam((Long) id))
                .collect(java.util.stream.Collectors.toList());
            currentGroup.setTeams(selectedTeams);

            if (currentGroup.getId() == null) {
                currentGroup.setStatus(RoundStatus.OPEN);
                groupService.createGroup(authBean.getUser(), currentGroup);
                addMessage(FacesMessage.SEVERITY_INFO, "Group created successfully");
            } else {
                groupService.updateGroup(authBean.getUser(), currentGroup);
                addMessage(FacesMessage.SEVERITY_INFO, "Group updated successfully");
            }
            loadGroups();
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }
    }

    public void deleteGroup(Long id) {
        try {
            groupService.deleteGroup(authBean.getUser(), id);
            addMessage(FacesMessage.SEVERITY_INFO, "Group deleted successfully");
            loadGroups();
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }
    }

    public void updateStatus(Long id, RoundStatus status) {
        try {
            groupService.updateGroupStatus(authBean.getUser(), id, status);
            addMessage(FacesMessage.SEVERITY_INFO, "Group status updated");
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
    public List<Long> getSelectedTeamIds() { return selectedTeamIds; }
    public void setSelectedTeamIds(List<Long> selectedTeamIds) { this.selectedTeamIds = selectedTeamIds; }
}
