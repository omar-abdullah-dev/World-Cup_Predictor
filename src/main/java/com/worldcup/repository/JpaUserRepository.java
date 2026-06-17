package com.worldcup.repository;

import com.worldcup.model.User;
import com.worldcup.security.Role;
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
 * Pure JDBC implementation of UserRepository.
 * No JPA/Hibernate queries — all operations use PreparedStatement.
 */
@ApplicationScoped
public class JpaUserRepository implements UserRepository {

    private static final Logger LOG = Logger.getLogger(JpaUserRepository.class.getName());

    @Inject
    private JdbcHelper jdbc;

    // ── save ─────────────────────────────────────────────────────────────

    @Override
    public User save(User user) {
        String sql = "INSERT INTO users (username, password_hash, role, total_points, "
                + "created_at, last_login, ad_username, employee_id, email, display_name) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getRole() != null ? user.getRole().name() : Role.NORMAL_USER.name());
            ps.setInt(4, user.getTotalPoints());
            ps.setTimestamp(5, toTs(user.getCreatedAt() != null ? user.getCreatedAt() : LocalDateTime.now()));
            ps.setTimestamp(6, toTs(user.getLastLogin()));
            ps.setString(7, user.getAdUsername());
            ps.setString(8, user.getEmployeeId());
            ps.setString(9, user.getEmail());
            ps.setString(10, user.getDisplayName());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) user.setId(keys.getLong(1));
            }
            return user;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "save(User) failed", e);
            throw new RuntimeException("Failed to save user: " + e.getMessage(), e);
        }
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Override
    public Optional<User> findById(Long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "findById failed id=" + id, e);
        }
        return Optional.empty();
    }

    // ── findByUsername ────────────────────────────────────────────────────

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "findByUsername failed username=" + username, e);
        }
        return Optional.empty();
    }

    // ── findByAdUsername ──────────────────────────────────────────────────

    @Override
    public Optional<User> findByAdUsername(String adUsername) {
        String sql = "SELECT * FROM users WHERE ad_username = ?";
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, adUsername);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "findByAdUsername failed", e);
        }
        return Optional.empty();
    }

    // ── findAll ───────────────────────────────────────────────────────────

    @Override
    public List<User> findAll() {
        String sql = "SELECT * FROM users ORDER BY created_at DESC";
        List<User> list = new ArrayList<User>();
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "findAll(User) failed", e);
        }
        return list;
    }

    // ── update ────────────────────────────────────────────────────────────

    @Override
    public User update(User user) {
        String sql = "UPDATE users SET username=?, password_hash=?, role=?, total_points=?, "
                + "last_login=?, ad_username=?, employee_id=?, email=?, display_name=? "
                + "WHERE id=?";
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getRole() != null ? user.getRole().name() : Role.NORMAL_USER.name());
            ps.setInt(4, user.getTotalPoints());
            ps.setTimestamp(5, toTs(user.getLastLogin()));
            ps.setString(6, user.getAdUsername());
            ps.setString(7, user.getEmployeeId());
            ps.setString(8, user.getEmail());
            ps.setString(9, user.getDisplayName());
            ps.setLong(10, user.getId());
            ps.executeUpdate();
            return user;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "update(User) failed id=" + user.getId(), e);
            throw new RuntimeException("Failed to update user: " + e.getMessage(), e);
        }
    }

    // ── mapping ───────────────────────────────────────────────────────────

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        String roleStr = rs.getString("role");
        u.setRole(roleStr != null ? Role.valueOf(roleStr) : Role.NORMAL_USER);
        u.setTotalPoints(rs.getInt("total_points"));
        u.setCreatedAt(fromTs(rs.getTimestamp("created_at")));
        u.setLastLogin(fromTs(rs.getTimestamp("last_login")));
        u.setAdUsername(rs.getString("ad_username"));
        u.setEmployeeId(rs.getString("employee_id"));
        u.setEmail(rs.getString("email"));
        u.setDisplayName(rs.getString("display_name"));
        return u;
    }

    private Timestamp toTs(LocalDateTime ldt) {
        return ldt == null ? null : Timestamp.valueOf(ldt);
    }

    private LocalDateTime fromTs(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }
}
