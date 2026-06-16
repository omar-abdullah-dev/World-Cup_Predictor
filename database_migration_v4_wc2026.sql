-- ================================================================
-- World Cup Predictor — Migration v4 CORRECTED (COMPLETE)
-- WC 2026 Official Data Seed
--
-- Schema verified against JPA entity annotations (Hibernate 6):
--   teams         : id, name, shortcode, flagemoji, logopath
--   tournament_rounds : id, stage, status, openedat, predictiondeadline, lockedat, closedat
--   groups_table  : id, name, status, round_id
--   group_teams   : group_id, team_id
--   matches       : id, home_team, away_team, kickoff_date, status, stage,
--                   home_score, away_score, result_entered_at, result_locked_at,
--                   match_number, prediction_deadline,
--                   home_team_id, away_team_id, round_id, group_id
--
-- All kickoff times converted to UTC.
-- "Curaçao" stored as 'Curacao' (no accent) to avoid encoding issues.
--
-- HOW TO RUN:
--   psql -U <user> -d <db> -f database_migration_v4_wc2026.sql
-- ================================================================
--
-- SELECT COUNT(*) FROM teams;
-- SELECT COUNT(*) FROM groups_table;
-- SELECT COUNT(*) FROM group_teams;
-- SELECT COUNT(*) FROM tournament_rounds;
-- SELECT COUNT(*) FROM matches;
-- SELECT stage, COUNT(*)
-- FROM matches
-- GROUP BY stage
-- ORDER BY stage;


BEGIN;

-- ──────────────────────────────────────────────
-- 1. WIPE existing game data (keep users / whitelist)
-- ──────────────────────────────────────────────
TRUNCATE TABLE
    predictions,
    matches,
    group_teams,
    groups_table,
    tournament_rounds,
    teams
    RESTART IDENTITY CASCADE;

UPDATE users SET total_points = 0;

-- ──────────────────────────────────────────────
-- 2. TEAMS  (48 teams; Curaçao stored without accent)
-- ──────────────────────────────────────────────
INSERT INTO teams (name, shortcode, flagemoji) VALUES
('Mexico',               'MEX', '🇲🇽'),
('South Africa',         'RSA', '🇿🇦'),
('South Korea',          'KOR', '🇰🇷'),
('Czech Republic',       'CZE', '🇨🇿'),
('Canada',               'CAN', '🇨🇦'),
('Bosnia & Herzegovina', 'BIH', '🇧🇦'),
('Qatar',                'QAT', '🇶🇦'),
('Switzerland',          'SUI', '🇨🇭'),
('Brazil',               'BRA', '🇧🇷'),
('Morocco',              'MAR', '🇲🇦'),
('Haiti',                'HAI', '🇭🇹'),
('Scotland',             'SCO', '🏴');

INSERT INTO teams (name, shortcode, flagemoji) VALUES
('USA',                  'USA', '🇺🇸'),
('Paraguay',             'PAR', '🇵🇾'),
('Australia',            'AUS', '🇦🇺'),
('Turkey',               'TUR', '🇹🇷'),
('Germany',              'GER', '🇩🇪'),
('Curacao',              'CUW', '🇨🇼'),
('Ivory Coast',          'CIV', '🇨🇮'),
('Ecuador',              'ECU', '🇪🇨'),
('Netherlands',          'NED', '🇳🇱'),
('Japan',                'JPN', '🇯🇵'),
('Sweden',               'SWE', '🇸🇪'),
('Tunisia',              'TUN', '🇹🇳'),
('Belgium',              'BEL', '🇧🇪'),
('Egypt',                'EGY', '🇪🇬'),
('Iran',                 'IRN', '🇮🇷'),
('New Zealand',          'NZL', '🇳🇿'),
('Spain',                'ESP', '🇪🇸'),
('Cape Verde',           'CPV', '🇨🇻'),
('Saudi Arabia',         'KSA', '🇸🇦'),
('Uruguay',              'URU', '🇺🇾'),
('France',               'FRA', '🇫🇷'),
('Senegal',              'SEN', '🇸🇳'),
('Iraq',                 'IRQ', '🇮🇶'),
('Norway',               'NOR', '🇳🇴'),
('Argentina',            'ARG', '🇦🇷'),
('Algeria',              'ALG', '🇩🇿'),
('Austria',              'AUT', '🇦🇹'),
('Jordan',               'JOR', '🇯🇴'),
('Portugal',             'POR', '🇵🇹'),
('DR Congo',             'COD', '🇨🇩'),
('Uzbekistan',           'UZB', '🇺🇿'),
('Colombia',             'COL', '🇨🇴'),
('England',              'ENG', '🏴'),
('Croatia',              'CRO', '🇭🇷'),
('Ghana',                'GHA', '🇬🇭'),
('Panama',               'PAN', '🇵🇦');

-- ──────────────────────────────────────────────
-- 3. TOURNAMENT ROUNDS  (7 rows, one per TournamentStage)
--    ALL columns: stage, status, openedat, predictiondeadline, lockedat, closedat
--    GROUP_STAGE is OPEN; all others UPCOMING.
--    predictiondeadline = 5 min before first match of that stage (UTC).
-- ──────────────────────────────────────────────
INSERT INTO tournament_rounds (stage, status, openedat, predictiondeadline, lockedat, closedat)
VALUES
  ('GROUP_STAGE',  'OPEN',     NOW(), '2026-06-11 18:55:00', NULL, NULL),
  ('ROUND_OF_32',  'UPCOMING', NULL,  '2026-06-28 18:55:00', NULL, NULL),
  ('ROUND_OF_16',  'UPCOMING', NULL,  '2026-07-04 20:55:00', NULL, NULL),
  ('QUARTER_FINAL','UPCOMING', NULL,  '2026-07-09 19:55:00', NULL, NULL),
  ('SEMI_FINAL',   'UPCOMING', NULL,  '2026-07-14 18:55:00', NULL, NULL),
  ('THIRD_PLACE',  'UPCOMING', NULL,  '2026-07-18 20:55:00', NULL, NULL),
  ('FINAL',        'UPCOMING', NULL,  '2026-07-19 18:55:00', NULL, NULL);

-- ──────────────────────────────────────────────
-- 4. GROUPS  (12 groups linked to GROUP_STAGE round)
-- ──────────────────────────────────────────────
INSERT INTO groups_table (name, status, round_id)
SELECT g.name, 'OPEN', r.id
FROM (VALUES
  ('Group A'),('Group B'),('Group C'),('Group D'),
  ('Group E'),('Group F'),('Group G'),('Group H'),
  ('Group I'),('Group J'),('Group K'),('Group L')
) AS g(name)
CROSS JOIN tournament_rounds r
WHERE r.stage = 'GROUP_STAGE';

-- ──────────────────────────────────────────────
-- 5. GROUP_TEAMS  (4 teams × 12 groups = 48 rows)
-- ──────────────────────────────────────────────
INSERT INTO group_teams (group_id, team_id)
SELECT g.id, t.id
FROM groups_table g
JOIN teams t ON
  (g.name='Group A' AND t.name IN ('Mexico','South Africa','South Korea','Czech Republic'))
  OR (g.name='Group B' AND t.name IN ('Canada','Bosnia & Herzegovina','Qatar','Switzerland'))
  OR (g.name='Group C' AND t.name IN ('Brazil','Morocco','Haiti','Scotland'))
  OR (g.name='Group D' AND t.name IN ('USA','Paraguay','Australia','Turkey'))
  OR (g.name='Group E' AND t.name IN ('Germany','Curacao','Ivory Coast','Ecuador'))
  OR (g.name='Group F' AND t.name IN ('Netherlands','Japan','Sweden','Tunisia'))
  OR (g.name='Group G' AND t.name IN ('Belgium','Egypt','Iran','New Zealand'))
  OR (g.name='Group H' AND t.name IN ('Spain','Cape Verde','Saudi Arabia','Uruguay'))
  OR (g.name='Group I' AND t.name IN ('France','Senegal','Iraq','Norway'))
  OR (g.name='Group J' AND t.name IN ('Argentina','Algeria','Austria','Jordan'))
  OR (g.name='Group K' AND t.name IN ('Portugal','DR Congo','Uzbekistan','Colombia'))
  OR (g.name='Group L' AND t.name IN ('England','Croatia','Ghana','Panama'));

-- ──────────────────────────────────────────────
-- 6. GROUP STAGE MATCHES
-- All kickoff_date values are in UTC.
-- UTC conversion: "HH:MM UTC-N" → add N hours.
-- Finished matches have home_score, away_score, result_entered_at set.
-- ──────────────────────────────────────────────

-- ── GROUP A ──
-- Mexico vs South Africa: 2026-06-11 13:00 UTC-6 → 19:00 UTC (FINISHED 2-0)
-- South Korea vs Czech Republic: 2026-06-11 20:00 UTC-6 → 02:00+1 = 2026-06-12 02:00 UTC (FINISHED 2-1)
-- Czech Republic vs South Africa: 2026-06-18 12:00 UTC-4 → 16:00 UTC
-- Mexico vs South Korea: 2026-06-18 19:00 UTC-6 → 2026-06-19 01:00 UTC
-- Czech Republic vs Mexico: 2026-06-24 19:00 UTC-6 → 2026-06-25 01:00 UTC
-- South Africa vs South Korea: 2026-06-24 19:00 UTC-6 → 2026-06-25 01:00 UTC
INSERT INTO matches
  (home_team, away_team, kickoff_date, status, stage,
   home_score, away_score, result_entered_at,
   home_team_id, away_team_id, round_id, group_id)
SELECT v.ht,
       v.at,
       v.ko::TIMESTAMP,
       v.st::VARCHAR,
       'GROUP_STAGE',
       v.hs,
       v.as2, CASE WHEN v.st='FINISHED' THEN v.ko::TIMESTAMP END,
       ht.id, at.id, r.id, grp.id
FROM (VALUES
  ('Mexico',       'South Africa',  '2026-06-11 19:00:00','FINISHED', 2,   0  ),
  ('South Korea',  'Czech Republic','2026-06-12 02:00:00','FINISHED', 2,   1  ),
  ('Czech Republic','South Africa', '2026-06-18 16:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('Mexico',       'South Korea',   '2026-06-19 01:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('Czech Republic','Mexico',        '2026-06-25 01:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('South Africa', 'South Korea',   '2026-06-25 01:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER)
) AS v(
    ht,
    at,
    ko,
    st,
    hs,
    as2
)
JOIN teams ht ON ht.name = v.ht
JOIN teams at ON at.name = v.at
JOIN tournament_rounds r ON r.stage = 'GROUP_STAGE'
JOIN groups_table grp ON grp.name = 'Group A';

-- ── GROUP B ──
-- Canada vs Bosnia: 2026-06-12 15:00 UTC-4 → 19:00 UTC (FINISHED 1-1)
-- Qatar vs Switzerland: 2026-06-13 12:00 UTC-7 → 19:00 UTC (FINISHED 1-1)
-- Switzerland vs Bosnia: 2026-06-18 12:00 UTC-7 → 19:00 UTC
-- Canada vs Qatar: 2026-06-18 15:00 UTC-7 → 22:00 UTC
-- Switzerland vs Canada: 2026-06-24 12:00 UTC-7 → 19:00 UTC
-- Bosnia vs Qatar: 2026-06-24 12:00 UTC-7 → 19:00 UTC
INSERT INTO matches
  (home_team, away_team, kickoff_date, status, stage,
   home_score, away_score, result_entered_at,
   home_team_id, away_team_id, round_id, group_id)
SELECT v.ht,
       v.at,
       v.ko::TIMESTAMP,
       v.st::VARCHAR,
       'GROUP_STAGE',
       v.hs,
       v.as2, CASE WHEN v.st='FINISHED' THEN v.ko::TIMESTAMP END,
       ht.id, at.id, r.id, grp.id
FROM (VALUES
  ('Canada',               'Bosnia & Herzegovina','2026-06-12 19:00:00','FINISHED', 1,   1  ),
  ('Qatar',                'Switzerland',         '2026-06-13 19:00:00','FINISHED', 1,   1  ),
  ('Switzerland',          'Bosnia & Herzegovina','2026-06-18 19:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('Canada',               'Qatar',               '2026-06-18 22:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('Switzerland',          'Canada',              '2026-06-24 19:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('Bosnia & Herzegovina', 'Qatar',               '2026-06-24 19:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER)
) AS v(
    ht,
    at,
    ko,
    st,
    hs,
    as2
)
JOIN teams ht ON ht.name = v.ht
JOIN teams at ON at.name = v.at
JOIN tournament_rounds r ON r.stage = 'GROUP_STAGE'
JOIN groups_table grp ON grp.name = 'Group B';

-- ── GROUP C ──
-- Brazil vs Morocco: 2026-06-13 18:00 UTC-4 → 22:00 UTC (FINISHED 1-1)
-- Haiti vs Scotland: 2026-06-13 21:00 UTC-4 → 2026-06-14 01:00 UTC (FINISHED 0-1)
-- Scotland vs Morocco: 2026-06-19 18:00 UTC-4 → 22:00 UTC
-- Brazil vs Haiti: 2026-06-19 20:30 UTC-4 → 2026-06-20 00:30 UTC
-- Scotland vs Brazil: 2026-06-24 18:00 UTC-4 → 22:00 UTC
-- Morocco vs Haiti: 2026-06-24 18:00 UTC-4 → 22:00 UTC
INSERT INTO matches
  (home_team, away_team, kickoff_date, status, stage,
   home_score, away_score, result_entered_at,
   home_team_id, away_team_id, round_id, group_id)
SELECT v.ht,
       v.at,
       v.ko::TIMESTAMP,
       v.st::VARCHAR,
       'GROUP_STAGE',
       v.hs,
       v.as2, CASE WHEN v.st='FINISHED' THEN v.ko::TIMESTAMP END,
       ht.id, at.id, r.id, grp.id
FROM (VALUES
  ('Brazil',   'Morocco',  '2026-06-13 22:00:00','FINISHED', 1,   1  ),
  ('Haiti',    'Scotland', '2026-06-14 01:00:00','FINISHED', 0,   1  ),
  ('Scotland', 'Morocco',  '2026-06-19 22:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('Brazil',   'Haiti',    '2026-06-20 00:30:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('Scotland', 'Brazil',   '2026-06-24 22:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('Morocco',  'Haiti',    '2026-06-24 22:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER)
) AS v(
    ht,
    at,
    ko,
    st,
    hs,
    as2
)
JOIN teams ht ON ht.name = v.ht
JOIN teams at ON at.name = v.at
JOIN tournament_rounds r ON r.stage = 'GROUP_STAGE'
JOIN groups_table grp ON grp.name = 'Group C';

-- ── GROUP D ──
-- USA vs Paraguay: 2026-06-12 18:00 UTC-7 → 2026-06-13 01:00 UTC (FINISHED 4-1)
-- Australia vs Turkey: 2026-06-13 21:00 UTC-7 → 2026-06-14 04:00 UTC (FINISHED 2-0)
-- USA vs Australia: 2026-06-19 12:00 UTC-7 → 19:00 UTC
-- Turkey vs Paraguay: 2026-06-19 20:00 UTC-7 → 2026-06-20 03:00 UTC
-- Turkey vs USA: 2026-06-25 19:00 UTC-7 → 2026-06-26 02:00 UTC
-- Paraguay vs Australia: 2026-06-25 19:00 UTC-7 → 2026-06-26 02:00 UTC
INSERT INTO matches
  (home_team, away_team, kickoff_date, status, stage,
   home_score, away_score, result_entered_at,
   home_team_id, away_team_id, round_id, group_id)
SELECT v.ht,
       v.at,
       v.ko::TIMESTAMP,
       v.st::VARCHAR,
       'GROUP_STAGE',
       v.hs,
       v.as2, CASE WHEN v.st='FINISHED' THEN v.ko::TIMESTAMP END,
       ht.id, at.id, r.id, grp.id
FROM (VALUES
  ('USA',       'Paraguay',  '2026-06-13 01:00:00','FINISHED', 4,   1  ),
  ('Australia', 'Turkey',    '2026-06-14 04:00:00','FINISHED', 2,   0  ),
  ('USA',       'Australia', '2026-06-19 19:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('Turkey',    'Paraguay',  '2026-06-20 03:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('Turkey',    'USA',       '2026-06-26 02:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('Paraguay',  'Australia', '2026-06-26 02:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER)
) AS v(
    ht,
    at,
    ko,
    st,
    hs,
    as2
)
JOIN teams ht ON ht.name = v.ht
JOIN teams at ON at.name = v.at
JOIN tournament_rounds r ON r.stage = 'GROUP_STAGE'
JOIN groups_table grp ON grp.name = 'Group D';

-- ── GROUP E ──
-- Germany vs Curacao: 2026-06-14 12:00 UTC-5 → 17:00 UTC (FINISHED 7-1)
-- Ivory Coast vs Ecuador: 2026-06-14 19:00 UTC-4 → 23:00 UTC (FINISHED 1-0)
-- Germany vs Ivory Coast: 2026-06-20 16:00 UTC-4 → 20:00 UTC
-- Ecuador vs Curacao: 2026-06-20 19:00 UTC-5 → 2026-06-21 00:00 UTC
-- Curacao vs Ivory Coast: 2026-06-25 16:00 UTC-4 → 20:00 UTC
-- Ecuador vs Germany: 2026-06-25 16:00 UTC-4 → 20:00 UTC
INSERT INTO matches
  (home_team, away_team, kickoff_date, status, stage,
   home_score, away_score, result_entered_at,
   home_team_id, away_team_id, round_id, group_id)
SELECT v.ht,
       v.at,
       v.ko::TIMESTAMP,
       v.st::VARCHAR,
       'GROUP_STAGE',
       v.hs,
       v.as2, CASE WHEN v.st='FINISHED' THEN v.ko::TIMESTAMP END,
       ht.id, at.id, r.id, grp.id
FROM (VALUES
  ('Germany',     'Curacao',      '2026-06-14 17:00:00','FINISHED', 7,   1  ),
  ('Ivory Coast', 'Ecuador',      '2026-06-14 23:00:00','FINISHED', 1,   0  ),
  ('Germany',     'Ivory Coast',  '2026-06-20 20:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('Ecuador',     'Curacao',      '2026-06-21 00:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('Curacao',     'Ivory Coast',  '2026-06-25 20:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('Ecuador',     'Germany',      '2026-06-25 20:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER)
) AS v(
    ht,
    at,
    ko,
    st,
    hs,
    as2
)
JOIN teams ht ON ht.name = v.ht
JOIN teams at ON at.name = v.at
JOIN tournament_rounds r ON r.stage = 'GROUP_STAGE'
JOIN groups_table grp ON grp.name = 'Group E';

-- ── GROUP F ──
-- Netherlands vs Japan: 2026-06-14 15:00 UTC-5 → 20:00 UTC (FINISHED 2-2)
-- Sweden vs Tunisia: 2026-06-14 20:00 UTC-6 → 2026-06-15 02:00 UTC (FINISHED 5-1)
-- Netherlands vs Sweden: 2026-06-20 12:00 UTC-5 → 17:00 UTC
-- Tunisia vs Japan: 2026-06-20 22:00 UTC-6 → 2026-06-21 04:00 UTC
-- Japan vs Sweden: 2026-06-25 18:00 UTC-5 → 23:00 UTC
-- Tunisia vs Netherlands: 2026-06-25 18:00 UTC-5 → 23:00 UTC
INSERT INTO matches
  (home_team, away_team, kickoff_date, status, stage,
   home_score, away_score, result_entered_at,
   home_team_id, away_team_id, round_id, group_id)
SELECT v.ht,
       v.at,
       v.ko::TIMESTAMP,
       v.st::VARCHAR,
       'GROUP_STAGE',
       v.hs,
       v.as2, CASE WHEN v.st='FINISHED' THEN v.ko::TIMESTAMP END,
       ht.id, at.id, r.id, grp.id
FROM (VALUES
  ('Netherlands','Japan',       '2026-06-14 20:00:00','FINISHED', 2,   2  ),
  ('Sweden',     'Tunisia',     '2026-06-15 02:00:00','FINISHED', 5,   1  ),
  ('Netherlands','Sweden',      '2026-06-20 17:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('Tunisia',    'Japan',       '2026-06-21 04:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('Japan',      'Sweden',      '2026-06-25 23:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('Tunisia',    'Netherlands', '2026-06-25 23:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER)
) AS v(
    ht,
    at,
    ko,
    st,
    hs,
    as2
)
JOIN teams ht ON ht.name = v.ht
JOIN teams at ON at.name = v.at
JOIN tournament_rounds r ON r.stage = 'GROUP_STAGE'
JOIN groups_table grp ON grp.name = 'Group F';

-- ── GROUP G ──
-- Belgium vs Egypt: 2026-06-15 12:00 UTC-7 → 19:00 UTC (SCHEDULED)
-- Iran vs New Zealand: 2026-06-15 18:00 UTC-7 → 2026-06-16 01:00 UTC
-- Belgium vs Iran: 2026-06-21 12:00 UTC-7 → 19:00 UTC
-- New Zealand vs Egypt: 2026-06-21 18:00 UTC-7 → 2026-06-22 01:00 UTC
-- Egypt vs Iran: 2026-06-26 20:00 UTC-7 → 2026-06-27 03:00 UTC
-- New Zealand vs Belgium: 2026-06-26 20:00 UTC-7 → 2026-06-27 03:00 UTC
INSERT INTO matches
  (home_team, away_team, kickoff_date, status, stage,
   home_score, away_score, result_entered_at,
   home_team_id, away_team_id, round_id, group_id)
SELECT v.ht,
       v.at,
       v.ko::TIMESTAMP,
       v.st::VARCHAR,
       'GROUP_STAGE',
       v.hs,
       v.as2, CASE WHEN v.st='FINISHED' THEN v.ko::TIMESTAMP END,
       ht.id, at.id, r.id, grp.id
FROM (VALUES
          ('Belgium',     'Egypt',       '2026-06-15 19:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
          ('Iran',        'New Zealand', '2026-06-16 01:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
          ('Belgium',     'Iran',        '2026-06-21 19:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
          ('New Zealand', 'Egypt',       '2026-06-22 01:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
          ('Egypt',       'Iran',        '2026-06-27 03:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
          ('New Zealand', 'Belgium',     '2026-06-27 03:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER)
     ) AS v(ht, at, ko, st, hs, as2)
JOIN teams ht ON ht.name = v.ht
JOIN teams at ON at.name = v.at
JOIN tournament_rounds r ON r.stage = 'GROUP_STAGE'
JOIN groups_table grp ON grp.name = 'Group G';

-- ── GROUP H ──
-- Spain vs Cape Verde: 2026-06-15 12:00 UTC-4 → 16:00 UTC
-- Saudi Arabia vs Uruguay: 2026-06-15 18:00 UTC-4 → 22:00 UTC
-- Spain vs Saudi Arabia: 2026-06-21 12:00 UTC-4 → 16:00 UTC
-- Uruguay vs Cape Verde: 2026-06-21 18:00 UTC-4 → 22:00 UTC
-- Cape Verde vs Saudi Arabia: 2026-06-26 19:00 UTC-5 → 2026-06-27 00:00 UTC
-- Uruguay vs Spain: 2026-06-26 18:00 UTC-6 → 2026-06-27 00:00 UTC
INSERT INTO matches
  (home_team, away_team, kickoff_date, status, stage,
   home_score, away_score, result_entered_at,
   home_team_id, away_team_id, round_id, group_id)
SELECT v.ht,
       v.at,
       v.ko::TIMESTAMP,
       v.st::VARCHAR,
       'GROUP_STAGE',
       v.hs,
       v.as2, CASE WHEN v.st='FINISHED' THEN v.ko::TIMESTAMP END,
       ht.id, at.id, r.id, grp.id
FROM (VALUES
  ('Spain',        'Cape Verde',   '2026-06-15 19:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('Saudi Arabia', 'Uruguay',      '2026-06-15 22:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('Spain',        'Saudi Arabia', '2026-06-21 16:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('Uruguay',      'Cape Verde',   '2026-06-21 22:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('Cape Verde',   'Saudi Arabia', '2026-06-27 00:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('Uruguay',      'Spain',        '2026-06-27 00:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER)
) AS v(
    ht,
    at,
    ko,
    st,
    hs,
    as2
)
JOIN teams ht ON ht.name = v.ht
JOIN teams at ON at.name = v.at
JOIN tournament_rounds r ON r.stage = 'GROUP_STAGE'
JOIN groups_table grp ON grp.name = 'Group H';

-- ── GROUP I ──
-- France vs Senegal: 2026-06-16 15:00 UTC-4 → 19:00 UTC
-- Iraq vs Norway: 2026-06-16 18:00 UTC-4 → 22:00 UTC
-- France vs Iraq: 2026-06-22 17:00 UTC-4 → 21:00 UTC
-- Norway vs Senegal: 2026-06-22 20:00 UTC-4 → 2026-06-23 00:00 UTC
-- Norway vs France: 2026-06-26 15:00 UTC-4 → 19:00 UTC
-- Senegal vs Iraq: 2026-06-26 15:00 UTC-4 → 19:00 UTC
INSERT INTO matches
  (home_team, away_team, kickoff_date, status, stage,
   home_score, away_score, result_entered_at,
   home_team_id, away_team_id, round_id, group_id)
SELECT v.ht,
       v.at,
       v.ko::TIMESTAMP,
       v.st::VARCHAR,
       'GROUP_STAGE',
       v.hs,
       v.as2, CASE WHEN v.st='FINISHED' THEN v.ko::TIMESTAMP END,
       ht.id, at.id, r.id, grp.id
FROM (VALUES
  ('France',  'Senegal', '2026-06-16 19:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('Iraq',    'Norway',  '2026-06-16 22:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('France',  'Iraq',    '2026-06-22 21:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('Norway',  'Senegal', '2026-06-23 00:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('Norway',  'France',  '2026-06-26 19:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('Senegal', 'Iraq',    '2026-06-26 19:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER)
) AS v(
    ht,
    at,
    ko,
    st,
    hs,
    as2
)
JOIN teams ht ON ht.name = v.ht
JOIN teams at ON at.name = v.at
JOIN tournament_rounds r ON r.stage = 'GROUP_STAGE'
JOIN groups_table grp ON grp.name = 'Group I';

-- ── GROUP J ──
-- Argentina vs Algeria: 2026-06-16 20:00 UTC-5 → 2026-06-17 01:00 UTC
-- Austria vs Jordan: 2026-06-16 21:00 UTC-7 → 2026-06-17 04:00 UTC
-- Argentina vs Austria: 2026-06-22 12:00 UTC-5 → 17:00 UTC
-- Jordan vs Algeria: 2026-06-22 20:00 UTC-7 → 2026-06-23 03:00 UTC
-- Algeria vs Austria: 2026-06-27 21:00 UTC-5 → 2026-06-28 02:00 UTC
-- Jordan vs Argentina: 2026-06-27 21:00 UTC-5 → 2026-06-28 02:00 UTC
INSERT INTO matches
  (home_team, away_team, kickoff_date, status, stage,
   home_score, away_score, result_entered_at,
   home_team_id, away_team_id, round_id, group_id)
SELECT v.ht,
       v.at,
       v.ko::TIMESTAMP,
       v.st::VARCHAR,
       'GROUP_STAGE',
       v.hs,
       v.as2, CASE WHEN v.st='FINISHED' THEN v.ko::TIMESTAMP END,
       ht.id, at.id, r.id, grp.id
FROM (VALUES
  ('Argentina', 'Algeria',   '2026-06-17 01:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('Austria',   'Jordan',    '2026-06-17 04:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('Argentina', 'Austria',   '2026-06-22 17:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('Jordan',    'Algeria',   '2026-06-23 03:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('Algeria',   'Austria',   '2026-06-28 02:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('Jordan',    'Argentina', '2026-06-28 02:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER)
) AS v(
    ht,
    at,
    ko,
    st,
    hs,
    as2
)
JOIN teams ht ON ht.name = v.ht
JOIN teams at ON at.name = v.at
JOIN tournament_rounds r ON r.stage = 'GROUP_STAGE'
JOIN groups_table grp ON grp.name = 'Group J';

-- ── GROUP K ──
-- Portugal vs DR Congo: 2026-06-17 12:00 UTC-5 → 17:00 UTC
-- Uzbekistan vs Colombia: 2026-06-17 20:00 UTC-6 → 2026-06-18 02:00 UTC
-- Portugal vs Uzbekistan: 2026-06-23 12:00 UTC-5 → 17:00 UTC
-- Colombia vs DR Congo: 2026-06-23 20:00 UTC-6 → 2026-06-24 02:00 UTC
-- Colombia vs Portugal: 2026-06-27 19:30 UTC-4 → 23:30 UTC
-- DR Congo vs Uzbekistan: 2026-06-27 19:30 UTC-4 → 23:30 UTC
INSERT INTO matches
  (home_team, away_team, kickoff_date, status, stage,
   home_score, away_score, result_entered_at,
   home_team_id, away_team_id, round_id, group_id)
SELECT v.ht,
       v.at,
       v.ko::TIMESTAMP,
       v.st::VARCHAR,
       'GROUP_STAGE',
       v.hs,
       v.as2, CASE WHEN v.st='FINISHED' THEN v.ko::TIMESTAMP END,
       ht.id, at.id, r.id, grp.id
FROM (VALUES
  ('Portugal',   'DR Congo',   '2026-06-17 17:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('Uzbekistan', 'Colombia',   '2026-06-18 02:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('Portugal',   'Uzbekistan', '2026-06-23 17:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('Colombia',   'DR Congo',   '2026-06-24 02:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('Colombia',   'Portugal',   '2026-06-27 23:30:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('DR Congo',   'Uzbekistan', '2026-06-27 23:30:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER)
) AS v(
    ht,
    at,
    ko,
    st,
    hs,
    as2
)
JOIN teams ht ON ht.name = v.ht
JOIN teams at ON at.name = v.at
JOIN tournament_rounds r ON r.stage = 'GROUP_STAGE'
JOIN groups_table grp ON grp.name = 'Group K';

-- ── GROUP L ──
-- England vs Croatia: 2026-06-17 15:00 UTC-5 → 20:00 UTC
-- Ghana vs Panama: 2026-06-17 19:00 UTC-4 → 23:00 UTC
-- England vs Ghana: 2026-06-23 16:00 UTC-4 → 20:00 UTC
-- Panama vs Croatia: 2026-06-23 19:00 UTC-4 → 23:00 UTC
-- Panama vs England: 2026-06-27 17:00 UTC-4 → 21:00 UTC
-- Croatia vs Ghana: 2026-06-27 17:00 UTC-4 → 21:00 UTC
INSERT INTO matches
  (home_team, away_team, kickoff_date, status, stage,
   home_score, away_score, result_entered_at,
   home_team_id, away_team_id, round_id, group_id)
SELECT v.ht,
       v.at,
       v.ko::TIMESTAMP,
       v.st::VARCHAR,
       'GROUP_STAGE',
       v.hs,
       v.as2, CASE WHEN v.st='FINISHED' THEN v.ko::TIMESTAMP END,
       ht.id, at.id, r.id, grp.id
FROM (VALUES
  ('England', 'Croatia', '2026-06-17 20:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('Ghana',   'Panama',  '2026-06-17 23:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('England', 'Ghana',   '2026-06-23 20:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('Panama',  'Croatia', '2026-06-23 23:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('Panama',  'England', '2026-06-27 21:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER),
  ('Croatia', 'Ghana',   '2026-06-27 21:00:00','SCHEDULED',NULL::INTEGER,NULL::INTEGER)
) AS v(
    ht,
    at,
    ko,
    st,
    hs,
    as2
)
JOIN teams ht ON ht.name = v.ht
JOIN teams at ON at.name = v.at
JOIN tournament_rounds r ON r.stage = 'GROUP_STAGE'
JOIN groups_table grp ON grp.name = 'Group L';

-- ──────────────────────────────────────────────
-- 7. ROUND OF 32 (16 matches — no team FKs; TBD placeholders)
--    home_team / away_team = descriptor string (e.g. '1A', '2B')
--    All kickoff_date values in UTC.
-- ──────────────────────────────────────────────
INSERT INTO matches
  (home_team, away_team, kickoff_date, status, stage, match_number,
   home_team_id, away_team_id, round_id, group_id)
SELECT v.ht, v.at, v.ko::TIMESTAMP, 'SCHEDULED', 'ROUND_OF_32', v.mn,
       NULL, NULL, r.id, NULL
FROM (VALUES
  (73, '2A',    '2B',          '2026-06-28 19:00:00'),
  (74, '1E',    '3A/B/C/D/F',  '2026-06-29 20:30:00'),
  (75, '1F',    '2C',          '2026-06-30 01:00:00'),
  (76, '1C',    '2F',          '2026-06-29 17:00:00'),
  (77, '1I',    '3C/D/F/G/H',  '2026-06-30 21:00:00'),
  (78, '2E',    '2I',          '2026-06-30 17:00:00'),
  (79, '1A',    '3C/E/F/H/I',  '2026-07-01 01:00:00'),
  (80, '1L',    '3E/H/I/J/K',  '2026-07-01 16:00:00'),
  (81, '1D',    '3B/E/F/I/J',  '2026-07-02 00:00:00'),
  (82, '1G',    '3A/E/H/I/J',  '2026-07-01 20:00:00'),
  (83, '2K',    '2L',          '2026-07-02 23:00:00'),
  (84, '1H',    '2J',          '2026-07-02 19:00:00'),
  (85, '1B',    '3E/F/G/I/J',  '2026-07-03 03:00:00'),
  (86, '1J',    '2H',          '2026-07-03 22:00:00'),
  (87, '1K',    '3D/E/I/J/L',  '2026-07-04 01:30:00'),
  (88, '2D',    '2G',          '2026-07-03 18:00:00')
) AS v(mn, ht, at, ko)
JOIN tournament_rounds r ON r.stage = 'ROUND_OF_32';

-- ──────────────────────────────────────────────
-- 8. ROUND OF 16 (8 matches)
--    W73..W88 = winner of match N
--    UTC: e.g. 17:00 UTC-4 → 21:00 UTC; 12:00 UTC-5 → 17:00 UTC
-- ──────────────────────────────────────────────
INSERT INTO matches
  (home_team, away_team, kickoff_date, status, stage, match_number,
   home_team_id, away_team_id, round_id, group_id)
SELECT v.ht, v.at, v.ko::TIMESTAMP, 'SCHEDULED', 'ROUND_OF_16', v.mn,
       NULL, NULL, r.id, NULL
FROM (VALUES
  (89, 'W74', 'W77', '2026-07-04 21:00:00'),
  (90, 'W73', 'W75', '2026-07-04 17:00:00'),
  (91, 'W76', 'W78', '2026-07-05 20:00:00'),
  (92, 'W79', 'W80', '2026-07-06 00:00:00'),
  (93, 'W83', 'W84', '2026-07-06 19:00:00'),
  (94, 'W81', 'W82', '2026-07-07 00:00:00'),
  (95, 'W86', 'W88', '2026-07-07 16:00:00'),
  (96, 'W85', 'W87', '2026-07-07 20:00:00')
) AS v(mn, ht, at, ko)
JOIN tournament_rounds r ON r.stage = 'ROUND_OF_16';

-- ──────────────────────────────────────────────
-- 9. QUARTER-FINALS (4 matches)
-- ──────────────────────────────────────────────
INSERT INTO matches
  (home_team, away_team, kickoff_date, status, stage, match_number,
   home_team_id, away_team_id, round_id, group_id)
SELECT v.ht, v.at, v.ko::TIMESTAMP, 'SCHEDULED', 'QUARTER_FINAL', v.mn,
       NULL, NULL, r.id, NULL
FROM (VALUES
  (97,  'W89', 'W90', '2026-07-09 20:00:00'),
  (98,  'W93', 'W94', '2026-07-10 19:00:00'),
  (99,  'W91', 'W92', '2026-07-11 21:00:00'),
  (100, 'W95', 'W96', '2026-07-12 01:00:00')
) AS v(mn, ht, at, ko)
JOIN tournament_rounds r ON r.stage = 'QUARTER_FINAL';

-- ──────────────────────────────────────────────
-- 10. SEMI-FINALS (2 matches)
-- ──────────────────────────────────────────────
INSERT INTO matches
  (home_team, away_team, kickoff_date, status, stage, match_number,
   home_team_id, away_team_id, round_id, group_id)
SELECT v.ht, v.at, v.ko::TIMESTAMP, 'SCHEDULED', 'SEMI_FINAL', v.mn,
       NULL, NULL, r.id, NULL
FROM (VALUES
  (101, 'W97',  'W98',  '2026-07-14 19:00:00'),
  (102, 'W99',  'W100', '2026-07-15 19:00:00')
) AS v(mn, ht, at, ko)
JOIN tournament_rounds r ON r.stage = 'SEMI_FINAL';

-- ──────────────────────────────────────────────
-- 11. THIRD PLACE (1 match)
-- ──────────────────────────────────────────────
INSERT INTO matches
  (home_team, away_team, kickoff_date, status, stage, match_number,
   home_team_id, away_team_id, round_id, group_id)
SELECT 'L101', 'L102', '2026-07-18 21:00:00'::TIMESTAMP,
       'SCHEDULED', 'THIRD_PLACE', 103,
       NULL, NULL, r.id, NULL
FROM tournament_rounds r WHERE r.stage = 'THIRD_PLACE';

-- ──────────────────────────────────────────────
-- 12. FINAL (1 match)
-- ──────────────────────────────────────────────
INSERT INTO matches
  (home_team, away_team, kickoff_date, status, stage, match_number,
   home_team_id, away_team_id, round_id, group_id)
SELECT 'W101', 'W102', '2026-07-19 19:00:00'::TIMESTAMP,
       'SCHEDULED', 'FINAL', 104,
       NULL, NULL, r.id, NULL
FROM tournament_rounds r WHERE r.stage = 'FINAL';

COMMIT;

