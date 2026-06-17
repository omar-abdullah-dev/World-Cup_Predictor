package com.worldcup.api;

import com.worldcup.api.dto.FixtureDto;
import com.worldcup.api.dto.LiveScoreDto;
import com.worldcup.api.dto.ScoresDto;
import com.worldcup.api.dto.StandingDto;
import com.worldcup.api.dto.TeamRefDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class WorldCupApiClient {

    private static final String FIXTURE_RESOURCE = "WC_2026/worldcup.json";
    private static final String TEAM_RESOURCE = "WC_2026/worldcup.teams.json";

    public List<FixtureDto> getFixtures(String group) {
        List<FixtureDto> fixtures = loadFixtures();
        if (group == null || group.trim().isEmpty()) {
            return fixtures;
        }
        String normalized = group.trim();
        List<FixtureDto> result = new ArrayList<FixtureDto>();
        for (FixtureDto fixture : fixtures) {
            if (normalized.equalsIgnoreCase(fixture.getRound())
                    || normalized.equalsIgnoreCase(getGroupName(fixture.getGroupId()))
                    || normalized.equalsIgnoreCase(getGroupLetter(fixture.getGroupId()))) {
                result.add(fixture);
            }
        }
        return result;
    }

    public List<FixtureDto> getFixturesByGroup(String group) {
        return getFixtures(group);
    }

    public List<LiveScoreDto> getLiveScores() {
        List<FixtureDto> fixtures = loadFixtures();
        List<LiveScoreDto> liveScores = new ArrayList<LiveScoreDto>();

        for (FixtureDto fixture : fixtures) {
            LiveScoreDto live = new LiveScoreDto();
            live.setFixtureId(fixture.getId());
            live.setHome(fixture.getHome());
            live.setAway(fixture.getAway());
            live.setLocation(fixture.getLocation());
            live.setStatus("SCHEDULED");
            live.setTime("00:00");

            if (fixture.getDate() != null && fixture.getTime() != null) {
                live.setTime(fixture.getTime());
            }

            String scoreString = createScoreString(fixture);
            ScoresDto scores = new ScoresDto();
            scores.setFtScore(scoreString);
            scores.setHtScore(scoreString);
            scores.setScore(scoreString);
            live.setScores(scores);

            if (scoreString != null && scoreString.contains("-")) {
                live.setStatus("FINISHED");
                live.setTime("FT");
            }
            liveScores.add(live);
        }

        return liveScores;
    }

    public List<StandingDto> getStandings(String group) {
        return new ArrayList<StandingDto>();
    }

    public List<FixtureDto> getHistory(String team1, String team2) {
        List<FixtureDto> fixtures = loadFixtures();
        List<FixtureDto> result = new ArrayList<FixtureDto>();
        if (team1 == null || team2 == null) {
            return result;
        }
        String left = team1.trim();
        String right = team2.trim();
        for (FixtureDto fixture : fixtures) {
            if ((left.equalsIgnoreCase(fixture.getHome().getName()) && right.equalsIgnoreCase(fixture.getAway().getName()))
                    || (left.equalsIgnoreCase(fixture.getAway().getName()) && right.equalsIgnoreCase(fixture.getHome().getName()))) {
                result.add(fixture);
            }
        }
        return result;
    }

    private List<FixtureDto> loadFixtures() {
        JsonObject root = readJsonRoot(FIXTURE_RESOURCE);
        JsonArray matches = root.getJsonArray("matches");
        Map<String, TeamRefDto> teams = loadTeams();
        List<FixtureDto> fixtures = new ArrayList<FixtureDto>();
        if (matches == null) {
            return fixtures;
        }
        for (int i = 0; i < matches.size(); i++) {
            JsonObject match = matches.getJsonObject(i);
            FixtureDto dto = new FixtureDto();
            dto.setId(i + 1L);
            dto.setGroupId(getGroupId(match.getString("group", "")));
            dto.setRound(match.getString("round", ""));
            dto.setDate(match.getString("date", ""));
            dto.setTime(parseTime(match.getString("time", "")));
            dto.setLocation(match.getString("ground", match.getString("location", "")));
            String homeName = match.getString("team1", "");
            String awayName = match.getString("team2", "");
            dto.setHome(findOrCreateTeam(teams, homeName));
            dto.setAway(findOrCreateTeam(teams, awayName));
            fixtures.add(dto);
        }
        return fixtures;
    }

    private JsonObject readJsonRoot(String resourcePath) {
        InputStream resource = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
        if (resource == null) {
            return Json.createObjectBuilder().build();
        }
        try (JsonReader reader = Json.createReader(new InputStreamReader(resource, "UTF-8"))) {
            return reader.readObject();
        } catch (Exception e) {
            return Json.createObjectBuilder().build();
        }
    }

    private Map<String, TeamRefDto> loadTeams() {
        Map<String, TeamRefDto> teams = new HashMap<String, TeamRefDto>();
        InputStream resource = Thread.currentThread().getContextClassLoader().getResourceAsStream(TEAM_RESOURCE);
        if (resource == null) {
            return teams;
        }
        try (JsonReader reader = Json.createReader(new InputStreamReader(resource, "UTF-8"))) {
            JsonArray array = reader.readArray();
            for (int i = 0; i < array.size(); i++) {
                JsonObject team = array.getJsonObject(i);
                TeamRefDto dto = new TeamRefDto();
                dto.setId(i + 1);
                dto.setName(team.getString("name", ""));
                dto.setLogo(team.getString("flag_icon", ""));
                teams.put(dto.getName().toLowerCase(), dto);
            }
        } catch (Exception e) {
            // ignore
        }
        return teams;
    }

    private TeamRefDto findOrCreateTeam(Map<String, TeamRefDto> teams, String name) {
        if (name == null) {
            name = "";
        }
        TeamRefDto existing = teams.get(name.toLowerCase());
        if (existing != null) {
            return existing;
        }
        TeamRefDto dto = new TeamRefDto();
        dto.setId(teams.size() + 1);
        dto.setName(name);
        dto.setLogo("");
        teams.put(name.toLowerCase(), dto);
        return dto;
    }

    private long getGroupId(String groupName) {
        if (groupName == null) {
            return 0L;
        }
        groupName = groupName.trim();
        if (groupName.length() == 1 && Character.isLetter(groupName.charAt(0))) {
            return Character.toUpperCase(groupName.charAt(0)) - 'A' + 1;
        }
        if (groupName.toUpperCase().startsWith("GROUP ") && groupName.length() >= 7) {
            char letter = groupName.toUpperCase().charAt(6);
            if (Character.isLetter(letter)) {
                return letter - 'A' + 1;
            }
        }
        return 0L;
    }

    private String getGroupLetter(long groupId) {
        if (groupId <= 0 || groupId > 26) {
            return "";
        }
        return String.valueOf((char) ('A' + groupId - 1));
    }

    private String getGroupName(long groupId) {
        if (groupId <= 0 || groupId > 26) {
            return "";
        }
        return "Group " + getGroupLetter(groupId);
    }

    private String parseTime(String timeValue) {
        if (timeValue == null) {
            return "";
        }
        int index = timeValue.indexOf(' ');
        if (index > 0) {
            return timeValue.substring(0, Math.min(index, timeValue.length()));
        }
        return timeValue;
    }

    private String createScoreString(FixtureDto fixture) {
        if (fixture == null || fixture.getHome() == null || fixture.getAway() == null) {
            return "";
        }
        String homeName = fixture.getHome().getName();
        String awayName = fixture.getAway().getName();
        if (homeName == null || awayName == null || homeName.isEmpty() || awayName.isEmpty()) {
            return "";
        }
        return "";
    }
}
