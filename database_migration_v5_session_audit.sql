-- ============================================================
-- Migration v5 — User Session Tracking & Activity Audit
--
-- Adds:
--   1. user_sessions table         (Part 2 – Active Session Tracking)
--   2. Extended columns on system_activity_log (Parts 1, 3, 4)
--
-- Safe to run multiple times (uses IF NOT EXISTS / ADD COLUMN IF NOT EXISTS).
-- ============================================================

BEGIN;

-- ──────────────────────────────────────────────────────────────
-- 1. user_sessions table
--    Tracks every login session with lifecycle status.
--    Status: ACTIVE | TERMINATED | EXPIRED | DISPLACED
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS user_sessions (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT        NOT NULL,
    username            VARCHAR(100)  NOT NULL,
    session_id          VARCHAR(255)  NOT NULL,
    browser_token       VARCHAR(128),
    ip_address          VARCHAR(64),
    user_agent          VARCHAR(512),
    login_time          TIMESTAMP     NOT NULL DEFAULT NOW(),
    last_activity_time  TIMESTAMP,
    logout_time         TIMESTAMP,
    status              VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT fk_us_user    FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_us_status CHECK (status IN ('ACTIVE','TERMINATED','EXPIRED','DISPLACED'))
);

CREATE INDEX IF NOT EXISTS idx_us_user_id    ON user_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_us_session_id ON user_sessions(session_id);
CREATE INDEX IF NOT EXISTS idx_us_status     ON user_sessions(status);

-- ──────────────────────────────────────────────────────────────
-- 2. Extend system_activity_log with session-aware audit columns
--    All columns are nullable so existing rows are unaffected.
-- ──────────────────────────────────────────────────────────────
ALTER TABLE system_activity_log
    ADD COLUMN IF NOT EXISTS user_id    BIGINT,
    ADD COLUMN IF NOT EXISTS session_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS ip_address VARCHAR(64),
    ADD COLUMN IF NOT EXISTS user_agent VARCHAR(512),
    ADD COLUMN IF NOT EXISTS match_id   BIGINT,
    ADD COLUMN IF NOT EXISTS old_value  VARCHAR(100),
    ADD COLUMN IF NOT EXISTS new_value  VARCHAR(100);

-- Indexes for fast audit queries
CREATE INDEX IF NOT EXISTS idx_sal_user_id    ON system_activity_log(user_id);
CREATE INDEX IF NOT EXISTS idx_sal_session_id ON system_activity_log(session_id);

COMMIT;
