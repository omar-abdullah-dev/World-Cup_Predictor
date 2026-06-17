package com.worldcup.repository;

import com.worldcup.model.Prediction;
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
 * Pure JDBC implementation of PredictionRepository.
 */
@ApplicationScoped
public class JpaPredictionRepository implements PredictionRepository {

    private static final Logger LOG = Logger.getLogger(JpaPredictionRepository.class.getName());

    @Inject
    private JdbcHelper jdbc;

    @Override
    public Prediction save(Prediction p) {
        if (p.getId() == null) {
            return insert(p);
        }
        return updateExisting(p);
    }

    private Prediction insert(Prediction p) {
        String sql = "INSERT INTO predictions "
                + "(user_id, match_id, predicted_home_score, predicted_away_score, earned_points, created_at, updated_at) "
                + "VALUES (?,?,?,?,?,?,?)";
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            LocalDateTime now = LocalDateTime.now();
            ps.setLong(1, p.getUserId());
            ps.setLong(2, p.getMatchId());
            ps.setInt(3, p.getPredictedHomeScore());
            ps.setInt(4, p.getPredictedAwayScore());
            ps.setInt(5, p.getEarnedPoints());
            ps.setTimestamp(6, Timestamp.valueOf(now));
            ps.setTimestamp(7, Timestamp.valueOf(now));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) p.setId(keys.getLong(1));
            }
            p.setCreatedAt(now);
            p.setUpdatedAt(now);
            return p;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "insert(Prediction) failed", e);
            throw new RuntimeException("Failed to insert prediction: " + e.getMessage(), e);
        }
    }

    private Prediction updateExisting(Prediction p) {
        String sql = "UPDATE predictions SET predicted_home_score=?, predicted_away_score=?, "
                + "earned_points=?, updated_at=? WHERE id=?";
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            LocalDateTime now = LocalDateTime.now();
            ps.setInt(1, p.getPredictedHomeScore());
            ps.setInt(2, p.getPredictedAwayScore());
            ps.setInt(3, p.getEarnedPoints());
            ps.setTimestamp(4, Timestamp.valueOf(now));
            ps.setLong(5, p.getId());
            ps.executeUpdate();
            p.setUpdatedAt(now);
            return p;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "update(Prediction) failed id=" + p.getId(), e);
            throw new RuntimeException("Failed to update prediction: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Prediction> findById(Long id) {
        String sql = "SELECT * FROM predictions WHERE id = ?";
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "findById(Prediction) failed id=" + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Prediction> findByUserAndMatch(Long userId, Long matchId) {
        String sql = "SELECT * FROM predictions WHERE user_id = ? AND match_id = ?";
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, matchId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "findByUserAndMatch failed", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Prediction> findByMatch(Long matchId) {
        String sql = "SELECT * FROM predictions WHERE match_id = ?";
        List<Prediction> list = new ArrayList<Prediction>();
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, matchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "findByMatch failed matchId=" + matchId, e);
        }
        return list;
    }

    @Override
    public List<Prediction> findAll() {
        String sql = "SELECT * FROM predictions";
        List<Prediction> list = new ArrayList<Prediction>();
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "findAll(Prediction) failed", e);
        }
        return list;
    }

    private Prediction map(ResultSet rs) throws SQLException {
        Prediction p = new Prediction();
        p.setId(rs.getLong("id"));
        p.setUserId(rs.getLong("user_id"));
        p.setMatchId(rs.getLong("match_id"));
        p.setPredictedHomeScore(rs.getInt("predicted_home_score"));
        p.setPredictedAwayScore(rs.getInt("predicted_away_score"));
        p.setEarnedPoints(rs.getInt("earned_points"));
        Timestamp ca = rs.getTimestamp("created_at");
        Timestamp ua = rs.getTimestamp("updated_at");
        p.setCreatedAt(ca != null ? ca.toLocalDateTime() : null);
        p.setUpdatedAt(ua != null ? ua.toLocalDateTime() : null);
        return p;
    }
}
