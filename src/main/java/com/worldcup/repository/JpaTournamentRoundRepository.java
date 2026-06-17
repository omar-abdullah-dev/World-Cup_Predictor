package com.worldcup.repository;

import com.worldcup.model.RoundStatus;
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
 * Pure JDBC implementation of TournamentRoundRepository.
 */
@ApplicationScoped
public class JpaTournamentRoundRepository implements TournamentRoundRepository {

    private static final Logger LOG = Logger.getLogger(JpaTournamentRoundRepository.class.getName());

    @Inject
    private JdbcHelper jdbc;

    @Override
    public TournamentRound save(TournamentRound r) {
        String sql = "INSERT INTO tournament_rounds (stage, status, openedat, predictiondeadline, lockedat, closedat) "
                + "VALUES (?,?,?,?,?,?)";
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, r.getStage() != null ? r.getStage().name() : null);
            ps.setString(2, r.getStatus() != null ? r.getStatus().name() : RoundStatus.UPCOMING.name());
            ps.setTimestamp(3, toTs(r.getOpenedAt()));
            ps.setTimestamp(4, toTs(r.getPredictionDeadline()));
            ps.setTimestamp(5, toTs(r.getLockedAt()));
            ps.setTimestamp(6, toTs(r.getClosedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) r.setId(keys.getLong(1));
            }
            return r;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "save(TournamentRound) failed", e);
            throw new RuntimeException("Failed to save round: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<TournamentRound> findById(Long id) {
        String sql = "SELECT * FROM tournament_rounds WHERE id = ?";
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "findById(TournamentRound) failed id=" + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<TournamentRound> findByStage(TournamentStage stage) {
        String sql = "SELECT * FROM tournament_rounds WHERE stage = ?";
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, stage.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "findByStage failed stage=" + stage, e);
        }
        return Optional.empty();
    }

    @Override
    public List<TournamentRound> findAll() {
        String sql = "SELECT * FROM tournament_rounds ORDER BY stage";
        List<TournamentRound> list = new ArrayList<TournamentRound>();
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "findAll(TournamentRound) failed", e);
        }
        return list;
    }

    @Override
    public List<TournamentRound> findByStatus(RoundStatus status) {
        String sql = "SELECT * FROM tournament_rounds WHERE status = ? ORDER BY stage";
        List<TournamentRound> list = new ArrayList<TournamentRound>();
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "findByStatus(TournamentRound) failed status=" + status, e);
        }
        return list;
    }

    @Override
    public TournamentRound update(TournamentRound r) {
        String sql = "UPDATE tournament_rounds SET stage=?, status=?, openedat=?, "
                + "predictiondeadline=?, lockedat=?, closedat=? WHERE id=?";
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, r.getStage() != null ? r.getStage().name() : null);
            ps.setString(2, r.getStatus() != null ? r.getStatus().name() : RoundStatus.UPCOMING.name());
            ps.setTimestamp(3, toTs(r.getOpenedAt()));
            ps.setTimestamp(4, toTs(r.getPredictionDeadline()));
            ps.setTimestamp(5, toTs(r.getLockedAt()));
            ps.setTimestamp(6, toTs(r.getClosedAt()));
            ps.setLong(7, r.getId());
            ps.executeUpdate();
            return r;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "update(TournamentRound) failed id=" + r.getId(), e);
            throw new RuntimeException("Failed to update round: " + e.getMessage(), e);
        }
    }

    private TournamentRound map(ResultSet rs) throws SQLException {
        TournamentRound r = new TournamentRound();
        r.setId(rs.getLong("id"));
        String stage = rs.getString("stage");
        if (stage != null) r.setStage(TournamentStage.valueOf(stage));
        String status = rs.getString("status");
        r.setStatus(status != null ? RoundStatus.valueOf(status) : RoundStatus.UPCOMING);
        r.setOpenedAt(fromTs(rs.getTimestamp("openedat")));
        r.setPredictionDeadline(fromTs(rs.getTimestamp("predictiondeadline")));
        r.setLockedAt(fromTs(rs.getTimestamp("lockedat")));
        r.setClosedAt(fromTs(rs.getTimestamp("closedat")));
        return r;
    }

    private Timestamp toTs(LocalDateTime ldt) {
        return ldt == null ? null : Timestamp.valueOf(ldt);
    }

    private LocalDateTime fromTs(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }
}
