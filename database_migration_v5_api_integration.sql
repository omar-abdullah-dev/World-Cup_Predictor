-- ================================================================
-- Migration v5 — WorldCupAPI Integration
-- Adds API sync columns to the matches table.
-- Safe to run multiple times (IF NOT EXISTS guards).
-- Run BEFORE deploying the updated application.
-- ================================================================

ALTER TABLE matches ADD COLUMN IF NOT EXISTS external_match_id BIGINT;
ALTER TABLE matches ADD COLUMN IF NOT EXISTS venue              VARCHAR(200);
ALTER TABLE matches ADD COLUMN IF NOT EXISTS home_team_api_id  INTEGER;
ALTER TABLE matches ADD COLUMN IF NOT EXISTS away_team_api_id  INTEGER;
ALTER TABLE matches ADD COLUMN IF NOT EXISTS home_team_logo    VARCHAR(500);
ALTER TABLE matches ADD COLUMN IF NOT EXISTS away_team_logo    VARCHAR(500);

-- Unique constraint on external_match_id (allows NULL for legacy rows)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uq_matches_external_match_id'
    ) THEN
        ALTER TABLE matches
            ADD CONSTRAINT uq_matches_external_match_id
            UNIQUE (external_match_id);
    END IF;
END $$;

-- Indexes for fast API sync lookups
CREATE INDEX IF NOT EXISTS idx_matches_external_id
    ON matches (external_match_id)
    WHERE external_match_id IS NOT NULL;

-- ================================================================
-- Verification
-- SELECT column_name FROM information_schema.columns
-- WHERE table_name = 'matches' ORDER BY ordinal_position;
-- ================================================================
