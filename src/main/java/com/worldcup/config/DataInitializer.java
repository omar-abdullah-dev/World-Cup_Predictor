package com.worldcup.config;

import com.worldcup.model.User;
import com.worldcup.security.Role;
import com.worldcup.service.MatchService;
import com.worldcup.service.UserService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import com.worldcup.model.Team;


/**
 * Seeds sample users and matches on application startup.
 *
 * Creates:
 * - 1 ADMIN user (password: "AdminPass123")
 * - 8 NORMAL_USER accounts (password: "UserPass123")
 */
@ApplicationScoped
public class DataInitializer {

    private static final Logger LOG = Logger.getLogger(DataInitializer.class.getName());

    @Inject private UserService userService;
    @Inject private MatchService matchService;
    @Inject private com.worldcup.service.WhitelistService whitelistService;
    @Inject private com.worldcup.service.TeamService teamService;

    protected DataInitializer() {}

    public void onStart(@Observes @Initialized(ApplicationScoped.class) Object init) {
        LOG.info("=== DataInitializer: seeding sample data ===");
        User adminUser = seedAdmin();
        if (adminUser != null) {
            seedNormalUsers(adminUser);
            seedTeams(adminUser);
            // Matches will be created by admins via Group Creation UI
        }
        LOG.info("=== DataInitializer: done ===");
    }

    private User seedAdmin() {
        String adminUsername = "admin";
        String adminPassword = "AdminPass123";
        try {
            User admin;
            // Check if already exists
            try {
                admin = userService.findByUsername(adminUsername);
            } catch (IllegalArgumentException ignored) {
                // Register the admin user first
                admin = userService.registerUser(adminUsername, adminPassword);
                admin.setRole(Role.ADMIN);
                userService.updateUser(admin);
                LOG.info("Created ADMIN user: " + adminUsername + " (password: " + adminPassword + ")");
            }
            
            // Add to whitelist if not already present
            if (!whitelistService.isUserWhitelisted(adminUsername)) {
                whitelistService.addEntry(admin, adminUsername, "Admin User", "admin@company.com");
            }

            return admin;
        } catch (Exception e) {
            LOG.warning("Failed to create ADMIN user: " + e.getMessage());
            return null;
        }
    }

    private void seedNormalUsers(User adminUser) {
        String userPassword = "UserPass123";
        for (String name : new String[]{
                "MessiFan2026", "RonaldoCR7", "NeymarJr11",
                "MbappeStar", "Modric10", "Lewandowski9",
                "ViniciusJr", "Bellingham22", "qli399@Company.com"}) {
            try {
                // Check if already exists
                try {
                    userService.findByUsername(name);
                } catch (IllegalArgumentException ignored) {
                    userService.registerUser(name, userPassword);
                    LOG.info("Created user: " + name);
                }

                // Add to whitelist if not already present
                if (!whitelistService.isUserWhitelisted(name)) {
                    String email = name.contains("@") ? name : name + "@company.com";
                    whitelistService.addEntry(adminUser, name, name + " Employee", email);
                    LOG.info("Added user " + name + " to whitelist");
                }
            } catch (Exception e) {
                LOG.warning("Skipped user '" + name + "': " + e.getMessage());
            }
        }
    }

    private void seedTeams(User adminUser) {
        String[] teamNames = {
            "USA", "Canada", "Mexico", "Argentina", "Brazil", "France", "England", "Spain",
            "Germany", "Portugal", "Italy", "Netherlands", "Belgium", "Croatia", "Uruguay", "Colombia",
            "Senegal", "Morocco", "Japan", "South Korea", "Iran", "Australia", "Saudi Arabia", "Qatar",
            "Nigeria", "Egypt", "Algeria", "Ivory Coast", "Ghana", "Cameroon", "Tunisia", "Mali",
            "Burkina Faso", "South Africa", "Ecuador", "Peru", "Chile", "Paraguay", "Venezuela", "Costa Rica",
            "Panama", "Jamaica", "Honduras", "El Salvador", "New Zealand", "Wales", "Poland", "Switzerland"
        };

        // Always re-fetch the latest snapshot so shortcode collision detection is accurate
        List<Team> existing = teamService.getAllTeams();

        // Collect both existing names AND existing shortcodes to avoid any collision
        Set<String> existingNames = new HashSet<>();
        Set<String> usedShortCodes = new HashSet<>();
        for (Team t : existing) {
            existingNames.add(t.getName().toLowerCase());
            if (t.getShortCode() != null) {
                usedShortCodes.add(t.getShortCode().toUpperCase());
            }
        }

        int created = 0;
        for (String tName : teamNames) {
            // Skip teams that already exist by name (case-insensitive)
            if (existingNames.contains(tName.toLowerCase())) {
                continue;
            }
            try {
                String shortCode = generateUniqueShortCode(tName, usedShortCodes);
                Team t = new Team();
                t.setName(tName);
                t.setShortCode(shortCode);
                teamService.createTeam(adminUser, t);
                usedShortCodes.add(shortCode);
                existingNames.add(tName.toLowerCase());
                LOG.info("Created team: " + tName + " (" + shortCode + ")");
                created++;
            } catch (Exception e) {
                LOG.warning("Skipped team '" + tName + "': " + e.getMessage());
            }
        }

        if (created == 0 && existing.size() >= teamNames.length) {
            LOG.info("All " + teamNames.length + " teams already present, skipping seed.");
        }
    }

    /** Builds a unique 3-letter shortcode; avoids collisions like SOU for South Korea vs South Africa. */
    private static String generateUniqueShortCode(String teamName, Set<String> usedShortCodes) {
        String preferred = KNOWN_SHORT_CODES.get(teamName);
        if (preferred != null && !usedShortCodes.contains(preferred)) {
            return preferred;
        }

        String[] parts = teamName.trim().split("\\s+");
        String candidate;
        if (parts.length >= 2) {
            StringBuilder initials = new StringBuilder();
            for (String part : parts) {
                if (!part.isEmpty() && initials.length() < 3) {
                    initials.append(Character.toUpperCase(part.charAt(0)));
                }
            }
            candidate = initials.toString();
            while (candidate.length() < 3) {
                candidate += "X";
            }
        } else {
            candidate = teamName.substring(0, Math.min(3, teamName.length())).toUpperCase();
        }

        if (!usedShortCodes.contains(candidate)) {
            return candidate;
        }

        for (int i = 1; i <= 9; i++) {
            String suffix = candidate.substring(0, 2) + i;
            if (!usedShortCodes.contains(suffix)) {
                return suffix;
            }
        }

        throw new IllegalStateException("Could not generate unique shortcode for: " + teamName);
    }

    private static final Map<String, String> KNOWN_SHORT_CODES = Map.ofEntries(
            Map.entry("South Korea", "KOR"),
            Map.entry("South Africa", "RSA"),
            Map.entry("Ivory Coast", "CIV"),
            Map.entry("Costa Rica", "CRC"),
            Map.entry("Saudi Arabia", "KSA"),
            Map.entry("New Zealand", "NZL"),
            Map.entry("El Salvador", "SLV"),
            Map.entry("Burkina Faso", "BFA")
    );
}
