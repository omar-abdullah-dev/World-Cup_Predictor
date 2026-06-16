package com.worldcup.api;

import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;

import java.util.logging.Logger;

/**
 * EJB timer that drives automatic synchronisation with WorldCupAPI.
 *
 * Live scores  — every 5 minutes (during match days this keeps results current)
 * All fixtures — every 6 hours    (keeps kickoff times and new matches in sync)
 *
 * Both jobs are idempotent: running them multiple times never creates duplicates.
 */
@Singleton
@Startup
public class LiveScoreSyncScheduler {

    private static final Logger LOG = Logger.getLogger(LiveScoreSyncScheduler.class.getName());

    @Inject
    private MatchSyncService matchSyncService;

    /**
     * Poll live scores every 5 minutes.
     * Automatically recalculates predictions when a match finishes.
     */
    @Schedule(minute = "*/5", hour = "*", persistent = false)
    public void pollLiveScores() {
        LOG.fine("[Scheduler] Running live score sync...");
        try {
            matchSyncService.syncLiveScores();
        } catch (Exception e) {
            LOG.warning("[Scheduler] Live score sync failed: " + e.getMessage());
        }
    }

    /**
     * Refresh all fixtures every 6 hours to pick up schedule changes.
     */
    @Schedule(hour = "*/6", persistent = false)
    public void refreshAllFixtures() {
        LOG.info("[Scheduler] Running full fixture sync...");
        try {
            matchSyncService.syncAllFixtures();
        } catch (Exception e) {
            LOG.warning("[Scheduler] Fixture sync failed: " + e.getMessage());
        }
    }
}
