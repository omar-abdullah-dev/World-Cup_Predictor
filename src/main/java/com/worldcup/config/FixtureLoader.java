package com.worldcup.config;

import com.worldcup.model.Group;
import com.worldcup.model.Match;
import com.worldcup.model.MatchStatus;
import com.worldcup.model.TournamentRound;
import com.worldcup.model.TournamentStage;
import com.worldcup.model.User;
import com.worldcup.repository.JpaGroupRepository;
import com.worldcup.repository.JpaMatchRepository;
import com.worldcup.service.TournamentRoundService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads WC_2026/worldcup.json from the classpath and seeds all group-stage
 * matches into the database on first startup.
 *
 * This replaces the old WorldCupAPI live sync — all data comes from the
 * bundled JSON file, and admins can add/edit matches via admin-matches.xhtml.
 *
 * Uses a simple hand-rolled JSON parser (no Jackson dependency) for Java 8 compat.
 */
@ApplicationScoped
public class FixtureLoader {

    private static final Logger LOG = Logger.getLogger(FixtureLoader.class.getName());

    private static final String FIXTURE_FILE = "/WC_2026/worldcup.json";

    @Inject private JpaMatchRepository   matchRepository;
    @Inject private JpaGroupRepository   groupRepository;
    @Inject private TournamentRoundService roundService;

    /**
     * Seeds group-stage fixtures from the bundled JSON file.
     * Safe to call multiple times — skips matches whose kickoff date+teams already exist.
     *
     * @param adminUser user used for any admin-level operations
     */
    public void loadFixtures(User adminUser) {
        LOG.info("[FixtureLoader] Loading WC 2026 fixtures from " + FIXTURE_FILE);

        String json = readFile();
        if (json == null || json.trim().isEmpty()) {
            LOG.warning("[FixtureLoader] Could not read fixture file — skipping.");
            return;
        }

        int created = 0;
        int skipped = 0;

        // Simple line-by-line JSON parsing — no Jackson required
        // Each match object is delimited by { ... }
        String matchesSection = extractBetween(json, "\"matches\"", "]");
        if (matchesSection == null) {
            LOG.warning("[FixtureLoader] Could not find matches array in JSON.");
            return;
        }

        String[] rawMatches = matchesSection.split("\\{");
        for (String rawMatch : rawMatches) {
            if (rawMatch.trim().isEmpty()) continue;

            try {
                String round   = extractString(rawMatch, "round");
                String team1   = extractString(rawMatch, "team1");
                String team2   = extractString(rawMatch, "team2");
                String date    = extractString(rawMatch, "date");
                String time    = extractString(rawMatch, "time");
                String group   = extractString(rawMatch, "group");

                // Skip if essential fields are missing
                if (team1 == null || team2 == null || date == null || time == null) continue;

                // Skip knockout placeholders (team names like "2A", "W74", "L101")
                if (isPlaceholder(team1) || isPlaceholder(team2)) {
                    skipped++;
                    continue;
                }

                // Skip non-group-stage (Round of 32 etc.)
                if (group == null) {
                    skipped++;
                    continue;
                }

                LocalDateTime kickoff = parseKickoff(date, time);
                if (kickoff == null) {
                    skipped++;
                    continue;
                }

                // Check if match already exists (same home+away+kickoff date)
                if (matchExists(team1, team2, kickoff.toLocalDate().toString())) {
                    skipped++;
                    continue;
                }

                // Find or create group entity
                Group groupEntity = findOrCreateGroup(group);

                // Build match
                Match match = new Match();
                match.setHomeTeam(team1);
                match.setAwayTeam(team2);
                match.setKickoffDate(kickoff);
                match.setStatus(MatchStatus.SCHEDULED);
                match.setStage(TournamentStage.GROUP_STAGE);

                if (groupEntity != null) {
                    match.setGroup(groupEntity);
                    if (groupEntity.getRound() != null) {
                        match.setRound(groupEntity.getRound());
                    }
                }

                // Set result if available
                String score = extractString(rawMatch, "\"ft\"");
                if (score != null) {
                    int[] goals = parseFtScore(score);
                    if (goals != null) {
                        match.setHomeScore(goals[0]);
                        match.setAwayScore(goals[1]);
                        match.setStatus(MatchStatus.FINISHED);
                        match.setResultEnteredAt(LocalDateTime.now());
                    }
                }

                matchRepository.save(match);
                created++;

            } catch (Exception e) {
                LOG.log(Level.WARNING, "[FixtureLoader] Error parsing match: " + e.getMessage(), e);
            }
        }

        LOG.info("[FixtureLoader] Done — created=" + created + " skipped=" + skipped);
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private String readFile() {
        try {
            InputStream is = getClass().getResourceAsStream(FIXTURE_FILE);
            if (is == null) {
                LOG.warning("[FixtureLoader] Resource not found: " + FIXTURE_FILE);
                return null;
            }
            java.util.Scanner scanner = new java.util.Scanner(is, "UTF-8");
            StringBuilder sb = new StringBuilder();
            while (scanner.hasNextLine()) {
                sb.append(scanner.nextLine()).append('\n');
            }
            scanner.close();
            return sb.toString();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "[FixtureLoader] Failed to read fixture file", e);
            return null;
        }
    }

    private String extractBetween(String text, String afterKey, String endChar) {
        int start = text.indexOf(afterKey);
        if (start < 0) return null;
        start = text.indexOf("[", start);
        if (start < 0) return null;
        int depth = 0;
        int end = start;
        while (end < text.length()) {
            char c = text.charAt(end);
            if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) break;
            }
            end++;
        }
        return text.substring(start, end + 1);
    }

    private String extractString(String block, String key) {
        String search = "\"" + key + "\"";
        int idx = block.indexOf(search);
        if (idx < 0) return null;
        idx = block.indexOf(":", idx);
        if (idx < 0) return null;
        idx++;
        while (idx < block.length() && Character.isWhitespace(block.charAt(idx))) idx++;
        if (idx >= block.length()) return null;
        if (block.charAt(idx) == '"') {
            int start = idx + 1;
            int end = block.indexOf('"', start);
            if (end < 0) return null;
            return block.substring(start, end);
        }
        return null;
    }

    /**
     * Converts date "2026-06-11" + time "13:00 UTC-6" → UTC LocalDateTime.
     */
    private LocalDateTime parseKickoff(String date, String time) {
        try {
            // Parse UTC offset from time string e.g. "13:00 UTC-6" or "20:00 UTC-6"
            String[] parts = time.split("UTC");
            String hhmm = parts[0].trim();
            int offsetHours = 0;
            if (parts.length > 1) {
                String offsetStr = parts[1].trim();
                if (!offsetStr.isEmpty()) {
                    offsetHours = Integer.parseInt(offsetStr);
                }
            }
            String isoStr = date + "T" + hhmm + ":00";
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            LocalDateTime local = LocalDateTime.parse(isoStr, fmt);
            // Convert local time to UTC: utc = local - offset
            ZonedDateTime zdt = local.atZone(ZoneOffset.ofHours(offsetHours));
            return zdt.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        } catch (Exception e) {
            LOG.warning("[FixtureLoader] Failed to parse kickoff date=" + date + " time=" + time + ": " + e.getMessage());
            return null;
        }
    }

    private int[] parseFtScore(String ftValue) {
        try {
            // ft value looks like: [2, 0]
            String cleaned = ftValue.replaceAll("[\\[\\]]", "").trim();
            String[] parts = cleaned.split(",");
            if (parts.length == 2) {
                return new int[]{
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim())
                };
            }
        } catch (Exception ignored) {}
        return null;
    }

    private boolean isPlaceholder(String teamName) {
        if (teamName == null) return true;
        String t = teamName.trim();
        // Placeholders: "2A", "1E", "W74", "L101", "3A/B/C/D/F" etc.
        return t.matches("[0-9][A-L]") || t.startsWith("W") || t.startsWith("L")
                || t.contains("/") || t.matches("TBD");
    }

    private boolean matchExists(String team1, String team2, String date) {
        for (Match m : matchRepository.findAll()) {
            if (m.getHomeTeam().equalsIgnoreCase(team1)
                    && m.getAwayTeam().equalsIgnoreCase(team2)
                    && m.getKickoffDate() != null
                    && m.getKickoffDate().toLocalDate().toString().equals(date)) {
                return true;
            }
        }
        return false;
    }

    private Group findOrCreateGroup(String groupName) {
        try {
            java.util.Optional<Group> existing = groupRepository.findByName(groupName);
            if (existing.isPresent()) return existing.get();

            // Find the GROUP_STAGE round
            TournamentRound round = null;
            try {
                round = roundService.getRoundByStage(TournamentStage.GROUP_STAGE);
            } catch (Exception ignored) {}

            Group g = new Group();
            g.setName(groupName);
            g.setRound(round);
            return groupRepository.save(g);
        } catch (Exception e) {
            LOG.warning("[FixtureLoader] Could not find/create group '" + groupName + "': " + e.getMessage());
            return null;
        }
    }
}
