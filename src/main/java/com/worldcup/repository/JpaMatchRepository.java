package com.worldcup.repository;

import com.worldcup.model.Group;
import com.worldcup.model.Match;
import com.worldcup.model.MatchStatus;
import com.worldcup.model.Team;
import com.worldcup.model.TournamentRound;
import com.worldcup.model.TournamentStage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Pure JDBC implementation of MatchRepository.
 * All queries use PreparedStatement — no JPA/Hibernate.
 */
@ApplicationScoped
public class JpaMatchRepository implements MatchRepository {

    private static final Logger LOG = Logger.getLogger(JpaMatchRepository.class.getName());

    @Inject private JdbcHelper               jdbc;
    @Inject private JpaTeamRepository        teamRepo;
    @Inject private JpaTournamentRoundRepository roundRepo;
    @Inject private JpaGroupRepository       groupRepo;

    // ── save ─────────────────────────────────────────────────────────────

    @Override
    public Match save(Match m) {
        if (m.getId() == null) {
            return insert(m);
        }
        return merge(m);
    }

    private Match insert(Match m) {
        String sql = "INSERT INTO matches "
                + "(home_team, away_team, kickoff_date, home_score, away_score, status, "
                + " home_team_id, away_team_id, round_id, group_id, stage, match_number, "
                + " prediction_deadline, result_entered_at, result_locked_at, "
                + " extra_time_home_score, extra_time_away_score, "
                + " penalty_home_score, penalty_away_score, match_decided_by) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setMatchParams(ps, m);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) m.setId(keys.getLong(1));
            }
            return m;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "insert(Match) failed", e);
            throw new RuntimeException("Failed to insert match: " + e.getMessage(), e);
        }
    }

    private Match merge(Match m) {
        String sql = "UPDATE matches SET "
                + "home_team=?, away_team=?, kickoff_date=?, home_score=?, away_score=?, status=?, "
                + "home_team_id=?, away_team_id=?, round_id=?, group_id=?, stage=?, match_number=?, "
                + "prediction_deadline=?, result_entered_at=?, result_locked_at=?, "
                + "extra_time_home_score=?, extra_time_away_score=?, "
                + "penalty_home_score=?, penalty_away_score=?, match_decided_by=? "
                + "WHERE id=?";
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            setMatchParams(ps, m);
            ps.setLong(21, m.getId());
            ps.executeUpdate();
            return m;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "merge(Match) failed id=" + m.getId(), e);
            throw new RuntimeException("Failed to update match: " + e.getMessage(), e);
        }
    }

    /** Sets the 20 common params for both INSERT and UPDATE. */
    private void setMatchParams(PreparedStatement ps, Match m) throws SQLException {
        ps.setString(1, m.getHomeTeam());
        ps.setString(2, m.getAwayTeam());
        ps.setTimestamp(3, toTs(m.getKickoffDate()));
        setNullableInt(ps, 4, m.getHomeScore());
        setNullableInt(ps, 5, m.getAwayScore());
        ps.setString(6, m.getStatus() != null ? m.getStatus().name() : MatchStatus.SCHEDULED.name());
        setNullableLong(ps, 7, m.getHomeTeamEntity() != null ? m.getHomeTeamEntity().getId() : null);
        setNullableLong(ps, 8, m.getAwayTeamEntity() != null ? m.getAwayTeamEntity().getId() : null);
        setNullableLong(ps, 9, m.getRound() != null ? m.getRound().getId() : null);
        setNullableLong(ps, 10, m.getGroup() != null ? m.getGroup().getId() : null);
        ps.setString(11, m.getStage() != null ? m.getStage().name() : null);
        setNullableInt(ps, 12, m.getMatchNumber());
        ps.setTimestamp(13, toTs(m.getPredictionDeadline()));
        ps.setTimestamp(14, toTs(m.getResultEnteredAt()));
        ps.setTimestamp(15, toTs(m.getResultLockedAt()));
        setNullableInt(ps, 16, m.getExtraTimeHomeScore());
        setNullableInt(ps, 17, m.getExtraTimeAwayScore());
        setNullableInt(ps, 18, m.getPenaltyHomeScore());
        setNullableInt(ps, 19, m.getPenaltyAwayScore());
        ps.setString(20, m.getMatchDecidedBy());
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Override
    public Optional<Match> findById(Long id) {
        String sql = "SELECT * FROM matches WHERE id = ?";
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapWithRelations(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "findById(Match) failed id=" + id, e);
        }
        return Optional.empty();
    }

    // ── findAll ───────────────────────────────────────────────────────────

    @Override
    public List<Match> findAll() {
        String sql = "SELECT * FROM matches ORDER BY kickoff_date";
        List<Match> list = new ArrayList<Match>();
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapWithRelations(rs));
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "findAll(Match) failed", e);
        }
        return list;
    }

    // ── findByRound ───────────────────────────────────────────────────────

    @Override
    public List<Match> findByRound(Long roundId) {
        String sql = "SELECT * FROM matches WHERE round_id = ? ORDER BY kickoff_date";
        List<Match> list = new ArrayList<Match>();
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, roundId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapWithRelations(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "findByRound failed roundId=" + roundId, e);
        }
        return list;
    }

    // ── findByGroup ───────────────────────────────────────────────────────

    @Override
    public List<Match> findByGroup(Long groupId) {
        String sql = "SELECT * FROM matches WHERE group_id = ? ORDER BY kickoff_date";
        List<Match> list = new ArrayList<Match>();
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapWithRelations(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "findByGroup failed groupId=" + groupId, e);
        }
        return list;
    }

    // ── update ────────────────────────────────────────────────────────────

    @Override
    public Match update(Match match) {
        return merge(match);
    }

    // ── deleteById ────────────────────────────────────────────────────────

    @Override
    public void deleteById(Long id) {
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM matches WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "deleteById(Match) failed id=" + id, e);
        }
    }

    // ── mapping ───────────────────────────────────────────────────────────

    private Match mapWithRelations(ResultSet rs) throws SQLException {
        Match m = new Match();
        m.setId(rs.getLong("id"));
        m.setHomeTeam(rs.getString("home_team"));
        m.setAwayTeam(rs.getString("away_team"));
        m.setKickoffDate(fromTs(rs.getTimestamp("kickoff_date")));
        m.setHomeScore(getNullableInt(rs, "home_score"));
        m.setAwayScore(getNullableInt(rs, "away_score"));
        String status = rs.getString("status");
        m.setStatus(status != null ? MatchStatus.valueOf(status) : MatchStatus.SCHEDULED);
        m.setMatchNumber(getNullableInt(rs, "match_number"));
        m.setPredictionDeadline(fromTs(rs.getTimestamp("prediction_deadline")));
        m.setResultEnteredAt(fromTs(rs.getTimestamp("result_entered_at")));
        m.setResultLockedAt(fromTs(rs.getTimestamp("result_locked_at")));
        m.setExtraTimeHomeScore(getNullableInt(rs, "extra_time_home_score"));
        m.setExtraTimeAwayScore(getNullableInt(rs, "extra_time_away_score"));
        m.setPenaltyHomeScore(getNullableInt(rs, "penalty_home_score"));
        m.setPenaltyAwayScore(getNullableInt(rs, "penalty_away_score"));
        m.setMatchDecidedBy(rs.getString("match_decided_by"));

        // stage
        String stageStr = rs.getString("stage");
        if (stageStr != null) {
            try { m.setStage(TournamentStage.valueOf(stageStr)); } catch (IllegalArgumentException ignored) {}
        }

        // FK relations — load lazily via JDBC
        long homeTeamId = rs.getLong("home_team_id");
        if (!rs.wasNull()) teamRepo.findById(homeTeamId).ifPresent(m::setHomeTeamEntity);

        long awayTeamId = rs.getLong("away_team_id");
        if (!rs.wasNull()) teamRepo.findById(awayTeamId).ifPresent(m::setAwayTeamEntity);

        long roundId = rs.getLong("round_id");
        if (!rs.wasNull()) roundRepo.findById(roundId).ifPresent(m::setRound);

        long groupId = rs.getLong("group_id");
        if (!rs.wasNull()) groupRepo.findById(groupId).ifPresent(m::setGroup);

        return m;
    }

    // ── SQL helpers ───────────────────────────────────────────────────────

    private Timestamp toTs(LocalDateTime ldt) {
        return ldt == null ? null : Timestamp.valueOf(ldt);
    }

    private LocalDateTime fromTs(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }

    private void setNullableInt(PreparedStatement ps, int idx, Integer val) throws SQLException {
        if (val == null) ps.setNull(idx, Types.INTEGER);
        else ps.setInt(idx, val);
    }

    private void setNullableLong(PreparedStatement ps, int idx, Long val) throws SQLException {
        if (val == null) ps.setNull(idx, Types.BIGINT);
        else ps.setLong(idx, val);
    }

    private Integer getNullableInt(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }
}
