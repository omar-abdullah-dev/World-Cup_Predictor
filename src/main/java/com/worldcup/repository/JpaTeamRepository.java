package com.worldcup.repository;

import com.worldcup.model.Team;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Pure JDBC implementation of TeamRepository.
 */
@ApplicationScoped
public class JpaTeamRepository implements TeamRepository {

    private static final Logger LOG = Logger.getLogger(JpaTeamRepository.class.getName());

    @Inject
    private JdbcHelper jdbc;

    @Override
    public Team save(Team team) {
        String sql = "INSERT INTO teams (name, short_code, logo_path, flag_emoji) VALUES (?,?,?,?)";
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, team.getName());
            ps.setString(2, team.getShortCode());
            ps.setString(3, team.getLogoPath());
            ps.setString(4, team.getFlagEmoji());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) team.setId(keys.getLong(1));
            }
            return team;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "save(Team) failed", e);
            throw new RuntimeException("Failed to save team: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Team> findById(Long id) {
        String sql = "SELECT * FROM teams WHERE id = ?";
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "findById(Team) failed id=" + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Team> findByName(String name) {
        String sql = "SELECT * FROM teams WHERE name = ?";
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "findByName(Team) failed name=" + name, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Team> findAll() {
        String sql = "SELECT * FROM teams ORDER BY name";
        List<Team> list = new ArrayList<Team>();
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "findAll(Team) failed", e);
        }
        return list;
    }

    @Override
    public Team update(Team team) {
        String sql = "UPDATE teams SET name=?, short_code=?, logo_path=?, flag_emoji=? WHERE id=?";
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, team.getName());
            ps.setString(2, team.getShortCode());
            ps.setString(3, team.getLogoPath());
            ps.setString(4, team.getFlagEmoji());
            ps.setLong(5, team.getId());
            ps.executeUpdate();
            return team;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "update(Team) failed id=" + team.getId(), e);
            throw new RuntimeException("Failed to update team: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        String sql = "DELETE FROM teams WHERE id = ?";
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "deleteById(Team) failed id=" + id, e);
            return false;
        }
    }

    private Team map(ResultSet rs) throws SQLException {
        Team t = new Team();
        t.setId(rs.getLong("id"));
        t.setName(rs.getString("name"));
        t.setShortCode(rs.getString("short_code"));
        t.setLogoPath(rs.getString("logo_path"));
        t.setFlagEmoji(rs.getString("flag_emoji"));
        return t;
    }
}
