-- World Cup Predictor Database Initialization Script
-- PostgreSQL Database Setup

-- Create Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'NORMAL_USER',
    is_approved BOOLEAN NOT NULL DEFAULT FALSE,
    total_points INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP,
    CONSTRAINT check_role CHECK (role IN ('NORMAL_USER', 'ADMIN'))
);

-- Create Matches table
CREATE TABLE IF NOT EXISTS matches (
    id BIGSERIAL PRIMARY KEY,
    home_team VARCHAR(100) NOT NULL,
    away_team VARCHAR(100) NOT NULL,
    kickoff_date TIMESTAMP NOT NULL,
    home_score INTEGER,
    away_score INTEGER,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    CONSTRAINT check_status CHECK (status IN ('SCHEDULED', 'IN_PROGRESS', 'FINISHED'))
);

-- Create Predictions table
CREATE TABLE IF NOT EXISTS predictions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    match_id BIGINT NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    predicted_home_score INTEGER NOT NULL,
    predicted_away_score INTEGER NOT NULL,
    earned_points INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, match_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (match_id) REFERENCES matches(id)
);

-- Create indexes for better performance
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_created_at ON users(created_at);
CREATE INDEX idx_matches_kickoff_date ON matches(kickoff_date);
CREATE INDEX idx_matches_status ON matches(status);
CREATE INDEX idx_predictions_user_id ON predictions(user_id);
CREATE INDEX idx_predictions_match_id ON predictions(match_id);
CREATE INDEX idx_predictions_user_match ON predictions(user_id, match_id);

-- Insert default admin and a sample normal user
-- Passwords are PBKDF2-SHA256 hashes compatible with PasswordService
-- Admin: username='admin' password='admin123'
-- User:  username='user'  password='user123'
INSERT INTO users (username, password_hash, role, is_approved, total_points)
VALUES ('admin', '65536:tcTdRNEDTiZEg0yXZO/MOw==:AFefJ0SCNwpXR0FS/H/A3suzsMYl3sQq7SfU8apZ6f4=', 'ADMIN', TRUE, 0)
ON CONFLICT (username) DO NOTHING;

INSERT INTO users (username, password_hash, role, is_approved, total_points)
VALUES ('user', '65536:zxeM9WtnGflhCHcZmGDZ8g==:p8HtUIODa0bSBCYuFon2VVcZFslPCZelCoEzI+nooLU=', 'NORMAL_USER', TRUE, 0)
ON CONFLICT (username) DO NOTHING;

-- Insert sample matches for 2026 World Cup (Qatar/USA)
INSERT INTO matches (home_team, away_team, kickoff_date, status)
VALUES
    ('Argentina', 'Saudi Arabia', '2026-06-11 18:00:00', 'FINISHED'),
    ('France', 'Netherlands', '2026-06-11 22:00:00', 'SCHEDULED'),
    ('Germany', 'Poland', '2026-06-12 18:00:00', 'SCHEDULED'),
    ('Spain', 'Morocco', '2026-06-12 22:00:00', 'SCHEDULED')
ON CONFLICT DO NOTHING;

-- Update admin's last login
UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE username = 'admin';


