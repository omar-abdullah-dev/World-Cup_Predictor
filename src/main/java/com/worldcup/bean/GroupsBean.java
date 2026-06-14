package com.worldcup.bean;

import com.worldcup.model.Group;
import com.worldcup.model.Team;
import com.worldcup.service.GroupService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.List;

import java.util.stream.Collectors;

/**
 * Manages World Cup groups and their teams.
 * Loads dynamic data from the database.
 */
@Named
@RequestScoped
public class GroupsBean {

    @Inject
    private GroupService groupService;

    public List<Group> getAllGroups() {
        return groupService.getAllGroups();
    }

    public List<String> getGroupNames() {
        return groupService.getAllGroups().stream()
                .map(Group::getName)
                .collect(Collectors.toList());
    }

    public List<Team> getTeamsByGroup(String groupName) {
        return groupService.getAllGroups().stream()
                .filter(g -> g.getName().equals(groupName))
                .findFirst()
                .map(Group::getTeams)
                .orElse(List.of());
    }

    public String normalizeTeamName(String displayName) {
        if (displayName == null) return "";
        return displayName.replaceAll("^[^A-Za-zÁ-ÿ]+\\s*", "").trim();
    }
}

