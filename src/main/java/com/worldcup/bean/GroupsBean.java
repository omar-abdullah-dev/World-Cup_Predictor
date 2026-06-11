package com.worldcup.bean;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Manages World Cup groups and their teams.
 * Provides group data for display and navigation.
 * This bean is UI-only; groups are derived from match data in services.
 */
@Named
@RequestScoped
public class GroupsBean {

    /**
     * World Cup 2026 Groups with teams
     * Data is static and serves for UI organization only.
     */
    private static final Map<String, List<String>> GROUPS = Map.ofEntries(
        Map.entry("A", Arrays.asList("🇶🇦 Qatar", "🇪🇨 Ecuador", "🇸🇳 Senegal", "🇳🇱 Netherlands")),
        Map.entry("B", Arrays.asList("🏴 England", "🇺🇸 USA", "🏴 Wales", "🇮🇷 Iran")),
        Map.entry("C", Arrays.asList("🇦🇷 Argentina", "🇸🇦 Saudi Arabia", "🇲🇽 Mexico", "🇵🇱 Poland")),
        Map.entry("D", Arrays.asList("🇫🇷 France", "🇩🇪 Germany", "🇪🇸 Spain", "🇯🇵 Japan"))
    );

    public List<String> getGroupNames() {
        return Arrays.asList("A", "B", "C", "D");
    }

    public List<String> getTeamsByGroup(String groupName) {
        return GROUPS.getOrDefault(groupName, Collections.emptyList());
    }

    public Map<String, List<String>> getAllGroups() {
        return GROUPS;
    }

    public String normalizeTeamName(String displayName) {
        if (displayName == null) return "";
        return displayName.replaceAll("^[^A-Za-zÁ-ÿ]+\\s*", "").trim();
    }
}

