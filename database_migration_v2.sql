-- World Cup Predictor - Major Refactoring Database Migration
-- Migration Script v2
-- Apply this script to update the PostgreSQL database schema.

-- 1. Create teams table
CREATE TABLE IF NOT EXISTS teams (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    short_code VARCHAR(3) UNIQUE,
    logo_path VARCHAR(255),
    flag_emoji VARCHAR(10)
);

-- 2. Create tournament_rounds table
CREATE TABLE IF NOT EXISTS tournament_rounds (
    id BIGSERIAL PRIMARY KEY,
    stage VARCHAR(50) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL,
    opened_at TIMESTAMP,
    prediction_deadline TIMESTAMP,
    locked_at TIMESTAMP,
    closed_at TIMESTAMP
);

-- 3. Create groups_table table
CREATE TABLE IF NOT EXISTS groups_table (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL
);

-- 4. Create group_teams join table
CREATE TABLE IF NOT EXISTS group_teams (
    group_id BIGINT NOT NULL,
    team_id BIGINT NOT NULL,
    PRIMARY KEY (group_id, team_id),
    CONSTRAINT fk_group FOREIGN KEY (group_id) REFERENCES groups_table(id),
    CONSTRAINT fk_team FOREIGN KEY (team_id) REFERENCES teams(id)
);

-- 5. Create whitelist table
CREATE TABLE IF NOT EXISTS whitelist (
    id BIGSERIAL PRIMARY KEY,
    ad_username VARCHAR(255) NOT NULL UNIQUE,
    employee_name VARCHAR(255),
    email VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    added_at TIMESTAMP,
    added_by_user_id BIGINT
);

-- 6. Alter matches table
ALTER TABLE matches
    ADD COLUMN home_team_id BIGINT,
    ADD COLUMN away_team_id BIGINT,
    ADD COLUMN round_id BIGINT,
    ADD COLUMN group_id BIGINT,
    ADD COLUMN stage VARCHAR(50),
    ADD COLUMN match_number INTEGER,
    ADD COLUMN prediction_deadline TIMESTAMP,
    ADD COLUMN result_entered_at TIMESTAMP,
    ADD COLUMN result_locked_at TIMESTAMP;

ALTER TABLE matches
    ADD CONSTRAINT fk_match_home_team FOREIGN KEY (home_team_id) REFERENCES teams(id),
    ADD CONSTRAINT fk_match_away_team FOREIGN KEY (away_team_id) REFERENCES teams(id),
    ADD CONSTRAINT fk_match_round FOREIGN KEY (round_id) REFERENCES tournament_rounds(id),
    ADD CONSTRAINT fk_match_group FOREIGN KEY (group_id) REFERENCES groups_table(id);

-- 7. Alter users table
ALTER TABLE users
    ADD COLUMN ad_username VARCHAR(100) UNIQUE,
    ADD COLUMN employee_id VARCHAR(50),
    ADD COLUMN email VARCHAR(100),
    ADD COLUMN display_name VARCHAR(100);

-- Make password_hash nullable (AD users won't have local passwords)
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;

-- Remove not-null constraint from deprecated is_approved column
ALTER TABLE users ALTER COLUMN is_approved DROP NOT NULL;

-- 8. Alter predictions table
ALTER TABLE predictions
    ADD COLUMN updated_at TIMESTAMP;
UPDATE predictions SET updated_at = created_at WHERE updated_at IS NULL;

-- 9. Create indexes
CREATE INDEX idx_match_round ON matches(round_id);
CREATE INDEX idx_match_group ON matches(group_id);
CREATE INDEX idx_match_stage ON matches(stage);
CREATE INDEX idx_prediction_updated ON predictions(updated_at);
CREATE INDEX idx_user_ad_username ON users(ad_username);

