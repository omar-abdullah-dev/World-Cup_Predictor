-- ================================================================
-- Migration v4b — Fix round deadline + correct all kickoff times
--                 + add new results from GitHub live data
--
-- ROOT CAUSE of locked predictions:
--   GROUP_STAGE round had predictiondeadline = '2026-06-11 18:55:00'
--   That date is now in the past → round.isPredictionsAllowed() = false
--   → ALL matches show "No upcoming predictions" regardless of kickoff.
--
-- FIX: extend predictiondeadline to cover the full group stage.
--
-- KICKOFF TIME CORRECTION:
--   All times stored in UTC. Egypt (Africa/Cairo) = UTC+3 in summer.
--   Formula: UTC = local_time_shown_in_JSON + abs(UTC_offset)
--   e.g. "13:00 UTC-6" → 13+6 = 19:00 UTC → 22:00 Cairo
--
-- NEW RESULTS added from openfootball/worldcup.json (2026 folder):
--   Belgium 1-1 Egypt  (Group G, Matchday 5)
--   Iran 2-2 New Zealand (Group G, Matchday 5)
--   Spain 0-0 Cape Verde (Group H, Matchday 5)
--   Saudi Arabia 1-1 Uruguay (Group H, Matchday 5)
-- ================================================================

BEGIN;

-- ──────────────────────────────────────────────────────────────
-- STEP 1: Fix GROUP_STAGE prediction deadline
--         Set to last group match kickoff - 5 min
--         Last group match: 2026-06-27 02:00 UTC (Jordan vs Argentina)
-- ──────────────────────────────────────────────────────────────
UPDATE tournament_rounds
SET    predictiondeadline = '2026-06-28 01:55:00'  -- 5 min before last group match
WHERE  stage = 'GROUP_STAGE';

-- ──────────────────────────────────────────────────────────────
-- STEP 2: Correct ALL group stage kickoff times (UTC)
--
-- Conversion formula: UTC = HH:MM + abs(UTC-N)
-- Egypt display = UTC + 3h
--
-- Group A
--   Mexico vs South Africa:     2026-06-11 13:00 UTC-6  → 19:00 UTC → 22:00 Cairo  ✓ already correct
--   South Korea vs Czech Rep:   2026-06-11 20:00 UTC-6  → 26:00 = 2026-06-12 02:00 UTC → 05:00 Cairo ✓
--   Czech Rep vs South Africa:  2026-06-18 12:00 UTC-4  → 16:00 UTC → 19:00 Cairo
--   Mexico vs South Korea:      2026-06-18 19:00 UTC-6  → 25:00 = 2026-06-19 01:00 UTC → 04:00 Cairo
--   Czech Rep vs Mexico:        2026-06-24 19:00 UTC-6  → 25:00 = 2026-06-25 01:00 UTC → 04:00 Cairo
--   South Africa vs South Korea:2026-06-24 19:00 UTC-6  → 25:00 = 2026-06-25 01:00 UTC → 04:00 Cairo
--
-- Group B
--   Canada vs Bosnia:           2026-06-12 15:00 UTC-4  → 19:00 UTC → 22:00 Cairo
--   Qatar vs Switzerland:       2026-06-13 12:00 UTC-7  → 19:00 UTC → 22:00 Cairo
--   Switzerland vs Bosnia:      2026-06-18 12:00 UTC-7  → 19:00 UTC → 22:00 Cairo
--   Canada vs Qatar:            2026-06-18 15:00 UTC-7  → 22:00 UTC → 01:00+1 Cairo
--   Switzerland vs Canada:      2026-06-24 12:00 UTC-7  → 19:00 UTC → 22:00 Cairo
--   Bosnia vs Qatar:            2026-06-24 12:00 UTC-7  → 19:00 UTC → 22:00 Cairo
--
-- Group C
--   Brazil vs Morocco:          2026-06-13 18:00 UTC-4  → 22:00 UTC → 01:00+1 Cairo
--   Haiti vs Scotland:          2026-06-13 21:00 UTC-4  → 25:00 = 2026-06-14 01:00 UTC → 04:00 Cairo
--   Scotland vs Morocco:        2026-06-19 18:00 UTC-4  → 22:00 UTC → 01:00+1 Cairo
--   Brazil vs Haiti:            2026-06-19 20:30 UTC-4  → 24:30 = 2026-06-20 00:30 UTC → 03:30 Cairo
--   Scotland vs Brazil:         2026-06-24 18:00 UTC-4  → 22:00 UTC → 01:00+1 Cairo
--   Morocco vs Haiti:           2026-06-24 18:00 UTC-4  → 22:00 UTC → 01:00+1 Cairo
--
-- Group D
--   USA vs Paraguay:            2026-06-12 18:00 UTC-7  → 25:00 = 2026-06-13 01:00 UTC → 04:00 Cairo
--   Australia vs Turkey:        2026-06-13 21:00 UTC-7  → 28:00 = 2026-06-14 04:00 UTC → 07:00 Cairo
--   USA vs Australia:           2026-06-19 12:00 UTC-7  → 19:00 UTC → 22:00 Cairo
--   Turkey vs Paraguay:         2026-06-19 20:00 UTC-7  → 27:00 = 2026-06-20 03:00 UTC → 06:00 Cairo
--   Turkey vs USA:              2026-06-25 19:00 UTC-7  → 26:00 = 2026-06-26 02:00 UTC → 05:00 Cairo
--   Paraguay vs Australia:      2026-06-25 19:00 UTC-7  → 26:00 = 2026-06-26 02:00 UTC → 05:00 Cairo
--
-- Group E
--   Germany vs Curacao:         2026-06-14 12:00 UTC-5  → 17:00 UTC → 20:00 Cairo
--   Ivory Coast vs Ecuador:     2026-06-14 19:00 UTC-4  → 23:00 UTC → 02:00+1 Cairo
--   Germany vs Ivory Coast:     2026-06-20 16:00 UTC-4  → 20:00 UTC → 23:00 Cairo
--   Ecuador vs Curacao:         2026-06-20 19:00 UTC-5  → 24:00 = 2026-06-21 00:00 UTC → 03:00 Cairo
--   Curacao vs Ivory Coast:     2026-06-25 16:00 UTC-4  → 20:00 UTC → 23:00 Cairo
--   Ecuador vs Germany:         2026-06-25 16:00 UTC-4  → 20:00 UTC → 23:00 Cairo
--
-- Group F
--   Netherlands vs Japan:       2026-06-14 15:00 UTC-5  → 20:00 UTC → 23:00 Cairo
--   Sweden vs Tunisia:          2026-06-14 20:00 UTC-6  → 26:00 = 2026-06-15 02:00 UTC → 05:00 Cairo
--   Netherlands vs Sweden:      2026-06-20 12:00 UTC-5  → 17:00 UTC → 20:00 Cairo
--   Tunisia vs Japan:           2026-06-20 22:00 UTC-6  → 28:00 = 2026-06-21 04:00 UTC → 07:00 Cairo
--   Japan vs Sweden:            2026-06-25 18:00 UTC-5  → 23:00 UTC → 02:00+1 Cairo
--   Tunisia vs Netherlands:     2026-06-25 18:00 UTC-5  → 23:00 UTC → 02:00+1 Cairo
--
-- Group G
--   Belgium vs Egypt:           2026-06-15 12:00 UTC-7  → 19:00 UTC → 22:00 Cairo
--   Iran vs New Zealand:        2026-06-15 18:00 UTC-7  → 25:00 = 2026-06-16 01:00 UTC → 04:00 Cairo
--   Belgium vs Iran:            2026-06-21 12:00 UTC-7  → 19:00 UTC → 22:00 Cairo
--   New Zealand vs Egypt:       2026-06-21 18:00 UTC-7  → 25:00 = 2026-06-22 01:00 UTC → 04:00 Cairo
--   Egypt vs Iran:              2026-06-26 20:00 UTC-7  → 27:00 = 2026-06-27 03:00 UTC → 06:00 Cairo
--   New Zealand vs Belgium:     2026-06-26 20:00 UTC-7  → 27:00 = 2026-06-27 03:00 UTC → 06:00 Cairo
--
-- Group H
--   Spain vs Cape Verde:        2026-06-15 12:00 UTC-4  → 16:00 UTC → 19:00 Cairo
--   Saudi Arabia vs Uruguay:    2026-06-15 18:00 UTC-4  → 22:00 UTC → 01:00+1 Cairo
--   Spain vs Saudi Arabia:      2026-06-21 12:00 UTC-4  → 16:00 UTC → 19:00 Cairo
--   Uruguay vs Cape Verde:      2026-06-21 18:00 UTC-4  → 22:00 UTC → 01:00+1 Cairo
--   Cape Verde vs Saudi Arabia: 2026-06-26 19:00 UTC-5  → 24:00 = 2026-06-27 00:00 UTC → 03:00 Cairo
--   Uruguay vs Spain:           2026-06-26 18:00 UTC-6  → 24:00 = 2026-06-27 00:00 UTC → 03:00 Cairo
--
-- Group I
--   France vs Senegal:          2026-06-16 15:00 UTC-4  → 19:00 UTC → 22:00 Cairo
--   Iraq vs Norway:             2026-06-16 18:00 UTC-4  → 22:00 UTC → 01:00+1 Cairo
--   France vs Iraq:             2026-06-22 17:00 UTC-4  → 21:00 UTC → 00:00+1 Cairo
--   Norway vs Senegal:          2026-06-22 20:00 UTC-4  → 24:00 = 2026-06-23 00:00 UTC → 03:00 Cairo
--   Norway vs France:           2026-06-26 15:00 UTC-4  → 19:00 UTC → 22:00 Cairo
--   Senegal vs Iraq:            2026-06-26 15:00 UTC-4  → 19:00 UTC → 22:00 Cairo
--
-- Group J
--   Argentina vs Algeria:       2026-06-16 20:00 UTC-5  → 25:00 = 2026-06-17 01:00 UTC → 04:00 Cairo
--   Austria vs Jordan:          2026-06-16 21:00 UTC-7  → 28:00 = 2026-06-17 04:00 UTC → 07:00 Cairo
--   Argentina vs Austria:       2026-06-22 12:00 UTC-5  → 17:00 UTC → 20:00 Cairo
--   Jordan vs Algeria:          2026-06-22 20:00 UTC-7  → 27:00 = 2026-06-23 03:00 UTC → 06:00 Cairo
--   Algeria vs Austria:         2026-06-27 21:00 UTC-5  → 26:00 = 2026-06-28 02:00 UTC → 05:00 Cairo
--   Jordan vs Argentina:        2026-06-27 21:00 UTC-5  → 26:00 = 2026-06-28 02:00 UTC → 05:00 Cairo
--
-- Group K
--   Portugal vs DR Congo:       2026-06-17 12:00 UTC-5  → 17:00 UTC → 20:00 Cairo
--   Uzbekistan vs Colombia:     2026-06-17 20:00 UTC-6  → 26:00 = 2026-06-18 02:00 UTC → 05:00 Cairo
--   Portugal vs Uzbekistan:     2026-06-23 12:00 UTC-5  → 17:00 UTC → 20:00 Cairo
--   Colombia vs DR Congo:       2026-06-23 20:00 UTC-6  → 26:00 = 2026-06-24 02:00 UTC → 05:00 Cairo
--   Colombia vs Portugal:       2026-06-27 19:30 UTC-4  → 23:30 UTC → 02:30+1 Cairo
--   DR Congo vs Uzbekistan:     2026-06-27 19:30 UTC-4  → 23:30 UTC → 02:30+1 Cairo
--
-- Group L
--   England vs Croatia:         2026-06-17 15:00 UTC-5  → 20:00 UTC → 23:00 Cairo
--   Ghana vs Panama:            2026-06-17 19:00 UTC-4  → 23:00 UTC → 02:00+1 Cairo
--   England vs Ghana:           2026-06-23 16:00 UTC-4  → 20:00 UTC → 23:00 Cairo
--   Panama vs Croatia:          2026-06-23 19:00 UTC-4  → 23:00 UTC → 02:00+1 Cairo
--   Panama vs England:          2026-06-27 17:00 UTC-4  → 21:00 UTC → 00:00+1 Cairo
--   Croatia vs Ghana:           2026-06-27 17:00 UTC-4  → 21:00 UTC → 00:00+1 Cairo
-- ──────────────────────────────────────────────────────────────
UPDATE matches SET kickoff_date = '2026-06-18 16:00:00' WHERE home_team='Czech Republic' AND away_team='South Africa';
UPDATE matches SET kickoff_date = '2026-06-19 01:00:00' WHERE home_team='Mexico'         AND away_team='South Korea';
UPDATE matches SET kickoff_date = '2026-06-25 01:00:00' WHERE home_team='Czech Republic' AND away_team='Mexico';
UPDATE matches SET kickoff_date = '2026-06-25 01:00:00' WHERE home_team='South Africa'   AND away_team='South Korea';

UPDATE matches SET kickoff_date = '2026-06-12 19:00:00' WHERE home_team='Canada'               AND away_team='Bosnia & Herzegovina';
UPDATE matches SET kickoff_date = '2026-06-13 19:00:00' WHERE home_team='Qatar'                AND away_team='Switzerland';
UPDATE matches SET kickoff_date = '2026-06-18 19:00:00' WHERE home_team='Switzerland'          AND away_team='Bosnia & Herzegovina';
UPDATE matches SET kickoff_date = '2026-06-18 22:00:00' WHERE home_team='Canada'               AND away_team='Qatar';
UPDATE matches SET kickoff_date = '2026-06-24 19:00:00' WHERE home_team='Switzerland'          AND away_team='Canada';
UPDATE matches SET kickoff_date = '2026-06-24 19:00:00' WHERE home_team='Bosnia & Herzegovina' AND away_team='Qatar';

UPDATE matches SET kickoff_date = '2026-06-13 22:00:00' WHERE home_team='Brazil'   AND away_team='Morocco';
UPDATE matches SET kickoff_date = '2026-06-14 01:00:00' WHERE home_team='Haiti'    AND away_team='Scotland';
UPDATE matches SET kickoff_date = '2026-06-19 22:00:00' WHERE home_team='Scotland' AND away_team='Morocco';
UPDATE matches SET kickoff_date = '2026-06-20 00:30:00' WHERE home_team='Brazil'   AND away_team='Haiti';
UPDATE matches SET kickoff_date = '2026-06-24 22:00:00' WHERE home_team='Scotland' AND away_team='Brazil';
UPDATE matches SET kickoff_date = '2026-06-24 22:00:00' WHERE home_team='Morocco'  AND away_team='Haiti';

UPDATE matches SET kickoff_date = '2026-06-13 01:00:00' WHERE home_team='USA'       AND away_team='Paraguay';
UPDATE matches SET kickoff_date = '2026-06-14 04:00:00' WHERE home_team='Australia' AND away_team='Turkey';
UPDATE matches SET kickoff_date = '2026-06-19 19:00:00' WHERE home_team='USA'       AND away_team='Australia';
UPDATE matches SET kickoff_date = '2026-06-20 03:00:00' WHERE home_team='Turkey'    AND away_team='Paraguay';
UPDATE matches SET kickoff_date = '2026-06-26 02:00:00' WHERE home_team='Turkey'    AND away_team='USA';
UPDATE matches SET kickoff_date = '2026-06-26 02:00:00' WHERE home_team='Paraguay'  AND away_team='Australia';

UPDATE matches SET kickoff_date = '2026-06-14 17:00:00' WHERE home_team='Germany'     AND away_team='Curacao';
UPDATE matches SET kickoff_date = '2026-06-14 23:00:00' WHERE home_team='Ivory Coast' AND away_team='Ecuador';
UPDATE matches SET kickoff_date = '2026-06-20 20:00:00' WHERE home_team='Germany'     AND away_team='Ivory Coast';
UPDATE matches SET kickoff_date = '2026-06-21 00:00:00' WHERE home_team='Ecuador'     AND away_team='Curacao';
UPDATE matches SET kickoff_date = '2026-06-25 20:00:00' WHERE home_team='Curacao'     AND away_team='Ivory Coast';
UPDATE matches SET kickoff_date = '2026-06-25 20:00:00' WHERE home_team='Ecuador'     AND away_team='Germany';

UPDATE matches SET kickoff_date = '2026-06-14 20:00:00' WHERE home_team='Netherlands' AND away_team='Japan';
UPDATE matches SET kickoff_date = '2026-06-15 02:00:00' WHERE home_team='Sweden'      AND away_team='Tunisia';
UPDATE matches SET kickoff_date = '2026-06-20 17:00:00' WHERE home_team='Netherlands' AND away_team='Sweden';
UPDATE matches SET kickoff_date = '2026-06-21 04:00:00' WHERE home_team='Tunisia'     AND away_team='Japan';
UPDATE matches SET kickoff_date = '2026-06-25 23:00:00' WHERE home_team='Japan'       AND away_team='Sweden';
UPDATE matches SET kickoff_date = '2026-06-25 23:00:00' WHERE home_team='Tunisia'     AND away_team='Netherlands';

UPDATE matches SET kickoff_date = '2026-06-15 19:00:00' WHERE home_team='Belgium'     AND away_team='Egypt';
UPDATE matches SET kickoff_date = '2026-06-16 01:00:00' WHERE home_team='Iran'        AND away_team='New Zealand';
UPDATE matches SET kickoff_date = '2026-06-21 19:00:00' WHERE home_team='Belgium'     AND away_team='Iran';
UPDATE matches SET kickoff_date = '2026-06-22 01:00:00' WHERE home_team='New Zealand' AND away_team='Egypt';
UPDATE matches SET kickoff_date = '2026-06-27 03:00:00' WHERE home_team='Egypt'       AND away_team='Iran';
UPDATE matches SET kickoff_date = '2026-06-27 03:00:00' WHERE home_team='New Zealand' AND away_team='Belgium';

UPDATE matches SET kickoff_date = '2026-06-15 16:00:00' WHERE home_team='Spain'        AND away_team='Cape Verde';
UPDATE matches SET kickoff_date = '2026-06-15 22:00:00' WHERE home_team='Saudi Arabia' AND away_team='Uruguay';
UPDATE matches SET kickoff_date = '2026-06-21 16:00:00' WHERE home_team='Spain'        AND away_team='Saudi Arabia';
UPDATE matches SET kickoff_date = '2026-06-21 22:00:00' WHERE home_team='Uruguay'      AND away_team='Cape Verde';
UPDATE matches SET kickoff_date = '2026-06-27 00:00:00' WHERE home_team='Cape Verde'   AND away_team='Saudi Arabia';
UPDATE matches SET kickoff_date = '2026-06-27 00:00:00' WHERE home_team='Uruguay'      AND away_team='Spain';

UPDATE matches SET kickoff_date = '2026-06-16 19:00:00' WHERE home_team='France'  AND away_team='Senegal';
UPDATE matches SET kickoff_date = '2026-06-16 22:00:00' WHERE home_team='Iraq'    AND away_team='Norway';
UPDATE matches SET kickoff_date = '2026-06-22 21:00:00' WHERE home_team='France'  AND away_team='Iraq';
UPDATE matches SET kickoff_date = '2026-06-23 00:00:00' WHERE home_team='Norway'  AND away_team='Senegal';
UPDATE matches SET kickoff_date = '2026-06-26 19:00:00' WHERE home_team='Norway'  AND away_team='France';
UPDATE matches SET kickoff_date = '2026-06-26 19:00:00' WHERE home_team='Senegal' AND away_team='Iraq';

UPDATE matches SET kickoff_date = '2026-06-17 01:00:00' WHERE home_team='Argentina' AND away_team='Algeria';
UPDATE matches SET kickoff_date = '2026-06-17 04:00:00' WHERE home_team='Austria'   AND away_team='Jordan';
UPDATE matches SET kickoff_date = '2026-06-22 17:00:00' WHERE home_team='Argentina' AND away_team='Austria';
UPDATE matches SET kickoff_date = '2026-06-23 03:00:00' WHERE home_team='Jordan'    AND away_team='Algeria';
UPDATE matches SET kickoff_date = '2026-06-28 02:00:00' WHERE home_team='Algeria'   AND away_team='Austria';
UPDATE matches SET kickoff_date = '2026-06-28 02:00:00' WHERE home_team='Jordan'    AND away_team='Argentina';

UPDATE matches SET kickoff_date = '2026-06-17 17:00:00' WHERE home_team='Portugal'   AND away_team='DR Congo';
UPDATE matches SET kickoff_date = '2026-06-18 02:00:00' WHERE home_team='Uzbekistan' AND away_team='Colombia';
UPDATE matches SET kickoff_date = '2026-06-23 17:00:00' WHERE home_team='Portugal'   AND away_team='Uzbekistan';
UPDATE matches SET kickoff_date = '2026-06-24 02:00:00' WHERE home_team='Colombia'   AND away_team='DR Congo';
UPDATE matches SET kickoff_date = '2026-06-27 23:30:00' WHERE home_team='Colombia'   AND away_team='Portugal';
UPDATE matches SET kickoff_date = '2026-06-27 23:30:00' WHERE home_team='DR Congo'   AND away_team='Uzbekistan';

UPDATE matches SET kickoff_date = '2026-06-17 20:00:00' WHERE home_team='England' AND away_team='Croatia';
UPDATE matches SET kickoff_date = '2026-06-17 23:00:00' WHERE home_team='Ghana'   AND away_team='Panama';
UPDATE matches SET kickoff_date = '2026-06-23 20:00:00' WHERE home_team='England' AND away_team='Ghana';
UPDATE matches SET kickoff_date = '2026-06-23 23:00:00' WHERE home_team='Panama'  AND away_team='Croatia';
UPDATE matches SET kickoff_date = '2026-06-27 21:00:00' WHERE home_team='Panama'  AND away_team='England';
UPDATE matches SET kickoff_date = '2026-06-27 21:00:00' WHERE home_team='Croatia' AND away_team='Ghana';

-- ──────────────────────────────────────────────────────────────
-- STEP 3: Add new results from live GitHub data (June 15, 2026)
--         Belgium 1-1 Egypt  (Group G)
--         Iran 2-2 New Zealand (Group G)
--         Spain 0-0 Cape Verde (Group H)
--         Saudi Arabia 1-1 Uruguay (Group H)
-- ──────────────────────────────────────────────────────────────
UPDATE matches
SET    status = 'FINISHED', home_score = 1, away_score = 1,
       result_entered_at = NOW()
WHERE  home_team = 'Belgium' AND away_team = 'Egypt'
  AND  status = 'SCHEDULED';

UPDATE matches
SET    status = 'FINISHED', home_score = 2, away_score = 2,
       result_entered_at = NOW()
WHERE  home_team = 'Iran' AND away_team = 'New Zealand'
  AND  status = 'SCHEDULED';

UPDATE matches
SET    status = 'FINISHED', home_score = 0, away_score = 0,
       result_entered_at = NOW()
WHERE  home_team = 'Spain' AND away_team = 'Cape Verde'
  AND  status = 'SCHEDULED';

UPDATE matches
SET    status = 'FINISHED', home_score = 1, away_score = 1,
       result_entered_at = NOW()
WHERE  home_team = 'Saudi Arabia' AND away_team = 'Uruguay'
  AND  status = 'SCHEDULED';

COMMIT;

-- ──────────────────────────────────────────────────────────────
-- VERIFICATION
-- ──────────────────────────────────────────────────────────────
-- Check round deadline is fixed:
-- SELECT stage, status, predictiondeadline FROM tournament_rounds WHERE stage='GROUP_STAGE';
--
-- Check next open matches (should show Group I onwards):
-- SELECT home_team, away_team, kickoff_date, status
-- FROM   matches
-- WHERE  status = 'SCHEDULED'
-- ORDER  BY kickoff_date
-- LIMIT  10;
