package com.worldcup.config;

import com.worldcup.model.Team;
import com.worldcup.model.User;
import com.worldcup.security.Role;
import com.worldcup.service.TeamService;
import com.worldcup.service.UserService;
import com.worldcup.service.WhitelistService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Seeds all required startup data:
 *   - Admin account: admin@QNB.COM.EG  (password: password)
 *   - Sample user accounts (all @QNB.COM.EG)
 *   - 48 World Cup 2026 teams
 *   - All group-stage fixtures from WC_2026/worldcup.json
 *
 * Java 8 compatible — no var, no Map.of(), no List.of().
 *
 * LOGIN CREDENTIALS (MockAuthenticationProvider):
 *   Admin  : admin@QNB.COM.EG   / password
 *   Users  : user1@QNB.COM.EG  / password  (etc.)
 */
@ApplicationScoped
public class DataInitializer {

    private static final Logger LOG = Logger.getLogger(DataInitializer.class.getName());

    @Inject private UserService      userService;
    @Inject private WhitelistService whitelistService;
    @Inject private TeamService      teamService;
    @Inject private FixtureLoader    fixtureLoader;

    protected DataInitializer() {}

    public void onStart(@Observes @Initialized(ApplicationScoped.class) Object init) {
        LOG.info("=== DataInitializer: seeding startup data ===");
        User adminUser = seedAdmin();
        if (adminUser != null) {
            seedNormalUsers(adminUser);
            seedTeams(adminUser);
            fixtureLoader.loadFixtures(adminUser);
        }
        LOG.info("=== DataInitializer: done ===");
    }

    // ── Admin ─────────────────────────────────────────────────────────────

    private User seedAdmin() {
        String adminUsername = "admin@QNB.COM.EG";
        try {
            User admin;
            try {
                admin = userService.findByUsername(adminUsername);
                LOG.info("Admin user already exists: " + adminUsername);
            } catch (IllegalArgumentException ignored) {
                admin = userService.registerUser(adminUsername, "password");
                admin.setRole(Role.ADMIN);
                userService.updateUser(admin);
                LOG.info("Created ADMIN: " + adminUsername + " / password");
            }
            if (!whitelistService.isUserWhitelisted(adminUsername)) {
                whitelistService.addEntry(admin, adminUsername, "System Administrator", adminUsername);
            }
            return admin;
        } catch (Exception e) {
            LOG.warning("Failed to seed admin: " + e.getMessage());
            return null;
        }
    }

    // ── Normal users ──────────────────────────────────────────────────────

    private void seedNormalUsers(User adminUser) {
        String[] users = {
            "messi@QNB.COM.EG",
            "ronaldo@QNB.COM.EG",
            "neymar@QNB.COM.EG",
            "mbappe@QNB.COM.EG",
            "modric@QNB.COM.EG",
            "lewandowski@QNB.COM.EG",
            "vinicius@QNB.COM.EG",
            "bellingham@QNB.COM.EG"
        };
        for (String username : users) {
            try {
                try {
                    userService.findByUsername(username);
                } catch (IllegalArgumentException ignored) {
                    userService.registerUser(username, "password");
                    LOG.info("Created user: " + username + " / password");
                }
                if (!whitelistService.isUserWhitelisted(username)) {
                    whitelistService.addEntry(adminUser, username, username, username);
                }
            } catch (Exception e) {
                LOG.warning("Skipped user '" + username + "': " + e.getMessage());
            }
        }
    }

    // ── Teams ─────────────────────────────────────────────────────────────

    private void seedTeams(User adminUser) {
        String[] teamNames = {
            "USA", "Canada", "Mexico", "Argentina", "Brazil", "France",
            "England", "Spain", "Germany", "Portugal", "Netherlands",
            "Belgium", "Croatia", "Uruguay", "Colombia", "Senegal",
            "Morocco", "Japan", "South Korea", "Iran", "Australia",
            "Saudi Arabia", "Qatar", "Nigeria", "Egypt", "Algeria",
            "Ivory Coast", "Ghana", "Cameroon", "Tunisia", "Ecuador",
            "Peru", "Chile", "Paraguay", "Venezuela", "Costa Rica",
            "Panama", "Jamaica", "Honduras", "El Salvador", "New Zealand",
            "Wales", "Poland", "Switzerland", "Sweden", "Turkey",
            "DR Congo", "Uzbekistan", "Norway", "Iraq", "Jordan",
            "Austria", "Scotland", "Haiti", "Cape Verde",
            "Bosnia & Herzegovina", "Curaçao", "South Africa",
            "Czech Republic", "Bosnia & Herzegovina"
        };

        List<Team> existing = teamService.getAllTeams();
        Set<String> existingNames = new HashSet<String>();
        Set<String> usedShortCodes = new HashSet<String>();
        for (Team t : existing) {
            existingNames.add(t.getName().toLowerCase());
            if (t.getShortCode() != null) {
                usedShortCodes.add(t.getShortCode().toUpperCase());
            }
        }

        int created = 0;
        for (String tName : teamNames) {
            if (existingNames.contains(tName.toLowerCase())) continue;
            try {
                String shortCode = generateUniqueShortCode(tName, usedShortCodes);
                Team t = new Team();
                t.setName(tName);
                t.setShortCode(shortCode);
                teamService.createTeam(adminUser, t);
                usedShortCodes.add(shortCode);
                existingNames.add(tName.toLowerCase());
                created++;
            } catch (Exception e) {
                LOG.warning("Skipped team '" + tName + "': " + e.getMessage());
            }
        }
        if (created > 0) LOG.info("Seeded " + created + " teams.");
    }

    private static String generateUniqueShortCode(String teamName, Set<String> used) {
        String preferred = KNOWN_SHORT_CODES.get(teamName);
        if (preferred != null && !used.contains(preferred)) return preferred;

        String[] parts = teamName.trim().split("\\s+");
        String candidate;
        if (parts.length >= 2) {
            StringBuilder initials = new StringBuilder();
            for (String p : parts) {
                if (!p.isEmpty() && initials.length() < 3)
                    initials.append(Character.toUpperCase(p.charAt(0)));
            }
            candidate = initials.toString();
            while (candidate.length() < 3) candidate += "X";
        } else {
            candidate = teamName.substring(0, Math.min(3, teamName.length())).toUpperCase();
        }

        if (!used.contains(candidate)) return candidate;
        for (int i = 1; i <= 9; i++) {
            String s = candidate.substring(0, 2) + i;
            if (!used.contains(s)) return s;
        }
        throw new IllegalStateException("No unique shortcode for: " + teamName);
    }

    private static final Map<String, String> KNOWN_SHORT_CODES;
    static {
        KNOWN_SHORT_CODES = new HashMap<String, String>();
        KNOWN_SHORT_CODES.put("South Korea",         "KOR");
        KNOWN_SHORT_CODES.put("South Africa",        "RSA");
        KNOWN_SHORT_CODES.put("Ivory Coast",         "CIV");
        KNOWN_SHORT_CODES.put("Costa Rica",          "CRC");
        KNOWN_SHORT_CODES.put("Saudi Arabia",        "KSA");
        KNOWN_SHORT_CODES.put("New Zealand",         "NZL");
        KNOWN_SHORT_CODES.put("El Salvador",         "SLV");
        KNOWN_SHORT_CODES.put("Burkina Faso",        "BFA");
        KNOWN_SHORT_CODES.put("DR Congo",            "COD");
        KNOWN_SHORT_CODES.put("Bosnia & Herzegovina","BIH");
        KNOWN_SHORT_CODES.put("Czech Republic",      "CZE");
        KNOWN_SHORT_CODES.put("Cape Verde",          "CPV");
        KNOWN_SHORT_CODES.put("Curaçao",             "CUW");
    }
}
