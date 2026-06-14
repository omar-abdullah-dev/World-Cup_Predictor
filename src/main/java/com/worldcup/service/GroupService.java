package com.worldcup.service;

import com.worldcup.model.Group;
import com.worldcup.model.RoundStatus;
import com.worldcup.model.Team;
import com.worldcup.model.User;
import com.worldcup.repository.GroupRepository;
import com.worldcup.security.SecurityService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class GroupService {

    private GroupRepository groupRepository;
    private TeamService teamService;

    protected GroupService() {}

    @Inject
    public GroupService(GroupRepository groupRepository, TeamService teamService) {
        this.groupRepository = groupRepository;
        this.teamService = teamService;
    }

    public List<Group> getAllGroups() {
        return groupRepository.findAll();
    }

    public Group getGroup(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));
    }

    public Group createGroup(User adminUser, Group group) {
        SecurityService.assertAdmin(adminUser, "create group");
        if (groupRepository.findByName(group.getName()).isPresent()) {
            throw new IllegalArgumentException("Group with this name already exists");
        }
        return groupRepository.save(group);
    }

    public Group updateGroup(User adminUser, Group updatedGroup) {
        SecurityService.assertAdmin(adminUser, "update group");
        Group existing = getGroup(updatedGroup.getId());
        existing.setName(updatedGroup.getName());
        existing.setTeams(updatedGroup.getTeams());
        return groupRepository.update(existing);
    }

    public Group addTeamToGroup(User adminUser, Long groupId, Long teamId) {
        SecurityService.assertAdmin(adminUser, "manage group teams");
        Group group = getGroup(groupId);
        Team team = teamService.getTeam(teamId);
        
        if (!group.getTeams().contains(team)) {
            group.getTeams().add(team);
            return groupRepository.update(group);
        }
        return group;
    }

    public Group removeTeamFromGroup(User adminUser, Long groupId, Long teamId) {
        SecurityService.assertAdmin(adminUser, "manage group teams");
        Group group = getGroup(groupId);
        group.getTeams().removeIf(t -> t.getId().equals(teamId));
        return groupRepository.update(group);
    }

    public Group updateGroupStatus(User adminUser, Long groupId, RoundStatus status) {
        SecurityService.assertAdmin(adminUser, "update group status");
        Group group = getGroup(groupId);
        group.setStatus(status);
        return groupRepository.update(group);
    }

    public void deleteGroup(User adminUser, Long id) {
        SecurityService.assertAdmin(adminUser, "delete group");
        if (!groupRepository.deleteById(id)) {
            throw new IllegalArgumentException("Group not found");
        }
    }
}
