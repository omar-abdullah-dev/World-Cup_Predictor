-- ================================================================
-- PREDICTION & SCORING TEST SCRIPT  (Option A)
-- Match: France vs Senegal  (id = 51)
--
-- STATUS: Match is already OPEN (SCHEDULED, kickoff in ~2h)
-- You can skip SECTION 1 and go straight to browser testing.
-- ================================================================


-- ================================================================
-- SECTION 1 — SETUP  (only needed if match needs reopening again)
-- ================================================================
/*
ROLLBACK;
DROP TABLE IF EXISTS test_match_backup;

BEGIN;

CREATE TEMP TABLE test_match_backup AS
SELECT id, status, home_score, away_score,
       kickoff_date, result_entered_at, result_locked_at
FROM   matches
WHERE  id = 51;

UPDATE matches
SET    status            = 'SCHEDULED',
       home_score        = NULL,
       away_score        = NULL,
       result_entered_at = NULL,
       result_locked_at  = NULL,
       kickoff_date      = NOW() + INTERVAL '2 hours'
WHERE  id = 51;

DELETE FROM predictions
WHERE  match_id = 51;

COMMIT;
*/


-- ================================================================
-- SECTION 2 — VERIFY: confirm match is open and check predictions
-- Run this at any time to see current state
-- ================================================================

-- Match status
SELECT id, home_team, away_team,
       kickoff_date                              AS kickoff_utc,
       kickoff_date + INTERVAL '3 hours'         AS kickoff_cairo,
       status, home_score, away_score
FROM   matches
WHERE  id = 51;

-- Current predictions for this match
SELECT u.username,
       p.predictedhomescore AS pred_home,
       p.predictedawayscore AS pred_away,
       p.earnedpoints        AS pts
FROM   predictions p
JOIN   users u ON u.id = p.user_id
WHERE  p.match_id = 51
ORDER  BY p.earnedpoints DESC;


-- ================================================================
-- SECTION 3 — RESULTS CHECK
-- Run after admin records the final score
-- ================================================================

SELECT u.username,
       m.home_team || ' ' || COALESCE(m.home_score::text,'?')
           || ' - ' || COALESCE(m.away_score::text,'?')
           || ' ' || m.away_team                         AS final_result,
       p.predictedhomescore || ' - ' || p.predictedawayscore AS your_prediction,
       p.earnedpoints                                     AS pts_earned,
       u.total_points                                     AS user_total,
       CASE p.earnedpoints
           WHEN 2 THEN '✓ EXACT SCORE  (+2)'
           WHEN 1 THEN '~ CORRECT OUTCOME (+1)'
           ELSE        '✗ WRONG (0)'
       END AS verdict
FROM   predictions p
JOIN   users u ON u.id = p.user_id
JOIN   matches  m ON m.id = p.match_id
WHERE  m.id = 51
ORDER  BY p.earnedpoints DESC;


-- ================================================================
-- SECTION 4 — RESET  (run when done testing)
-- Restores original kickoff, removes test predictions,
-- recalculates all user totals.
-- ================================================================
/*
BEGIN;

UPDATE matches
SET    status            = 'SCHEDULED',
       home_score        = NULL,
       away_score        = NULL,
       result_entered_at = NULL,
       result_locked_at  = NULL,
       kickoff_date      = '2026-06-16 19:00:00'   -- original UTC kickoff
WHERE  id = 51;

DELETE FROM predictions
WHERE  match_id = 51;

UPDATE users
SET    total_points = (
    SELECT COALESCE(SUM(p.earnedpoints), 0)
    FROM   predictions p
    WHERE  p.user_id = users.id
);

COMMIT;

SELECT 'Reset complete' AS status,
       id, home_team, away_team, kickoff_date, status
FROM   matches WHERE id = 51;
*/
