-- ============================================================
-- World Cup Predictor - Database Migration v3
-- System Activity Log Table
-- ============================================================
-- Run this script in PostgreSQL ONCE to create the log table.
-- NOTE: If you deploy the app first, Hibernate (hbm2ddl.auto=update)
--       will create the table automatically. Running this script after
--       that is safe because of the IF NOT EXISTS guards.
-- ============================================================

CREATE TABLE IF NOT EXISTS system_activity_log (
    id          BIGSERIAL     PRIMARY KEY,
    opmaj       VARCHAR(100)  NOT NULL,               -- operation type  (LOGIN, LOGOUT, RESULT_SAVED, ...)
    datemaj     TIMESTAMP     NOT NULL DEFAULT NOW(),  -- timestamp of the action
    transmaj    VARCHAR(500),                          -- detail / description
    profilemaj  VARCHAR(100)  NOT NULL                -- username of the actor
);

-- Indexes for fast audit queries
CREATE INDEX IF NOT EXISTS idx_sal_profilemaj ON system_activity_log (profilemaj);
CREATE INDEX IF NOT EXISTS idx_sal_datemaj    ON system_activity_log (datemaj DESC);
CREATE INDEX IF NOT EXISTS idx_sal_opmaj      ON system_activity_log (opmaj);

-- ============================================================
-- What gets logged automatically after deploying the app:
--
--   opmaj            | who logs it
--   -----------------+-----------------------------------
--   LOGIN            | AuthBean.login()
--   LOGOUT           | AuthBean.logout()
--   MATCH_CREATED    | AdminMatchBean.createMatch()
--   RESULT_SAVED     | AdminMatchBean.forceSubmitResult()
--   MATCH_DELETED    | AdminMatchBean.deleteMatch()
--
-- Example query — see all actions by a user:
--   SELECT * FROM system_activity_log
--   WHERE profilemaj = 'admin'
--   ORDER BY datemaj DESC;
--
-- Example query — see all logins today:
--   SELECT * FROM system_activity_log
--   WHERE opmaj = 'LOGIN'
--     AND datemaj >= CURRENT_DATE
--   ORDER BY datemaj DESC;
-- ============================================================
