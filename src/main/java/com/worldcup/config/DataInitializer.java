package com.worldcup.config;

import com.worldcup.model.User;
import com.worldcup.security.Role;
import com.worldcup.service.MatchService;
import com.worldcup.service.UserService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.logging.Logger;

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

    protected DataInitializer() {}

    public void onStart(@Observes @Initialized(ApplicationScoped.class) Object init) {
        LOG.info("=== DataInitializer: seeding sample data ===");
        User adminUser = seedAdmin();
        if (adminUser != null) {
            seedNormalUsers(adminUser);
            seedMatches(adminUser);
        }
        LOG.info("=== DataInitializer: done ===");
    }

    private User seedAdmin() {
        String adminUsername = "admin";
        String adminPassword = "AdminPass123";
        try {
            // Check if already exists
            try {
                return userService.findByUsername(adminUsername);
            } catch (IllegalArgumentException ignored) {}

            // Register the admin user first (gets NORMAL_USER, unapproved)
            User admin = userService.registerUser(adminUsername, adminPassword);

            // Directly set role and approval without requiring an existing admin
            admin.setRole(Role.ADMIN);
            admin.setApproved(true);
            userService.updateUser(admin);

            LOG.info("Created ADMIN user: " + adminUsername + " (password: " + adminPassword + ")");
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
                "ViniciusJr", "Bellingham22"}) {
            try {
                // Check if already exists
                try {
                    userService.findByUsername(name);
                    continue; // skip if exists
                } catch (IllegalArgumentException ignored) {}

                User user = userService.registerUser(name, userPassword);
                userService.approveUser(adminUser, user.getId());
                LOG.info("Created user: " + name + " (approved=true)");
            } catch (Exception e) {
                LOG.warning("Skipped user '" + name + "': " + e.getMessage());
            }
        }
    }

    private void seedMatches(User adminUser) {
        // Group A
        createMatch(adminUser, "Qatar",       "Ecuador",     LocalDateTime.of(2027, 6, 11, 18, 0));
        createMatch(adminUser, "Senegal",     "Netherlands", LocalDateTime.of(2027, 6, 11, 21, 0));
        createMatch(adminUser, "Qatar",       "Senegal",     LocalDateTime.of(2027, 6, 15, 18, 0));
        createMatch(adminUser, "Netherlands", "Ecuador",     LocalDateTime.of(2027, 6, 15, 21, 0));
        // Group B
        createMatch(adminUser, "England",     "Iran",        LocalDateTime.of(2027, 6, 12, 15, 0));
        createMatch(adminUser, "USA",         "Wales",       LocalDateTime.of(2027, 6, 12, 18, 0));
        createMatch(adminUser, "England",     "USA",         LocalDateTime.of(2027, 6, 16, 21, 0));
        createMatch(adminUser, "Wales",       "Iran",        LocalDateTime.of(2027, 6, 16, 18, 0));
        // Group C
        createMatch(adminUser, "Argentina",   "Saudi Arabia", LocalDateTime.of(2027, 6, 13, 12, 0));
        createMatch(adminUser, "Mexico",      "Poland",       LocalDateTime.of(2027, 6, 13, 15, 0));
        createMatch(adminUser, "Argentina",   "Mexico",       LocalDateTime.of(2027, 6, 17, 21, 0));
        createMatch(adminUser, "Poland",      "Saudi Arabia", LocalDateTime.of(2027, 6, 17, 18, 0));
    }

    private void createMatch(User adminUser, String home, String away, LocalDateTime kickoff) {
        try {
            matchService.createMatch(adminUser, home, away, kickoff);
            LOG.info("Created match: " + home + " vs " + away);
        } catch (Exception e) {
            LOG.warning("Skipped match '" + home + " vs " + away + "': " + e.getMessage());
        }
    }
}
