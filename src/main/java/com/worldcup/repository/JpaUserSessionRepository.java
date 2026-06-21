package com.worldcup.repository;

import com.worldcup.model.UserSession;
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
 * Pure JDBC implementation of UserSession repository.
 */
@ApplicationScoped
public class JpaUserSessionRepository {

    private static final Logger LOG = Logger.getLogger(JpaUserSessionRepository.class.getName());

    @Inject
    private JdbcHelper jdbc;

    public UserSession save(UserSession s) {
        if (s.getId() == null) {
            return insert(s);
        }
        return updateExisting(s);
    }

    private UserSession insert(UserSession s) {
        String sql = "INSERT INTO user_sessions "
                + "(user_id, username, session_id, browser_token, ip_address, user_agent, "
                + " login_time, last_activity_time, logout_time, status) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, s.getUserId());
            ps.setString(2, s.getUsername());
            ps.setString(3, s.getSessionId());
            ps.setString(4, s.getBrowserToken());
            ps.setString(5, s.getIpAddress());
            ps.setString(6, s.getUserAgent());
            ps.setTimestamp(7, toTs(s.getLoginTime() != null ? s.getLoginTime() : LocalDateTime.now()));
            ps.setTimestamp(8, toTs(s.getLastActivityTime()));
            ps.setTimestamp(9, toTs(s.getLogoutTime()));
            ps.setString(10, s.getStatus() != null ? s.getStatus().name() : UserSession.Status.ACTIVE.name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) s.setId(keys.getLong(1));
            }
            return s;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "insert(UserSession) failed", e);
            throw new RuntimeException("Failed to insert session: " + e.getMessage(), e);
        }
    }

    private UserSession updateExisting(UserSession s) {
        String sql = "UPDATE user_sessions SET last_activity_time=?, logout_time=?, status=? WHERE id=?";
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, toTs(s.getLastActivityTime()));
            ps.setTimestamp(2, toTs(s.getLogoutTime()));
            ps.setString(3, s.getStatus() != null ? s.getStatus().name() : UserSession.Status.ACTIVE.name());
            ps.setLong(4, s.getId());
            ps.executeUpdate();
            return s;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "update(UserSession) failed id=" + s.getId(), e);
            throw new RuntimeException("Failed to update session: " + e.getMessage(), e);
        }
    }

    public Optional<UserSession> findBySessionId(String sessionId) {
        String sql = "SELECT * FROM user_sessions WHERE session_id = ?";
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "findBySessionId failed", e);
        }
        return Optional.empty();
    }

    public Optional<UserSession> findActiveByUserId(Long userId) {
        // FETCH FIRST 1 ROWS ONLY is portable across Oracle and PostgreSQL
        String sql = "SELECT * FROM user_sessions WHERE user_id = ? AND status = 'ACTIVE' FETCH FIRST 1 ROWS ONLY";
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "findActiveByUserId failed userId=" + userId, e);
        }
        return Optional.empty();
    }

    public List<UserSession> findByUserId(Long userId) {
        String sql = "SELECT * FROM user_sessions WHERE user_id = ? ORDER BY login_time DESC";
        List<UserSession> list = new ArrayList<UserSession>();
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "findByUserId failed userId=" + userId, e);
        }
        return list;
    }

    public List<UserSession> findAll() {
        String sql = "SELECT * FROM user_sessions ORDER BY login_time DESC";
        List<UserSession> list = new ArrayList<UserSession>();
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "findAll(UserSession) failed", e);
        }
        return list;
    }

    public List<UserSession> findAllActive() {
        String sql = "SELECT * FROM user_sessions WHERE status = 'ACTIVE' ORDER BY login_time DESC";
        List<UserSession> list = new ArrayList<UserSession>();
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "findAllActive(UserSession) failed", e);
        }
        return list;
    }

    private UserSession map(ResultSet rs) throws SQLException {
        UserSession s = new UserSession();
        s.setId(rs.getLong("id"));
        s.setUserId(rs.getLong("user_id"));
        s.setUsername(rs.getString("username"));
        s.setSessionId(rs.getString("session_id"));
        s.setBrowserToken(rs.getString("browser_token"));
        s.setIpAddress(rs.getString("ip_address"));
        s.setUserAgent(rs.getString("user_agent"));
        s.setLoginTime(fromTs(rs.getTimestamp("login_time")));
        s.setLastActivityTime(fromTs(rs.getTimestamp("last_activity_time")));
        s.setLogoutTime(fromTs(rs.getTimestamp("logout_time")));
        String status = rs.getString("status");
        if (status != null) {
            try { s.setStatus(UserSession.Status.valueOf(status)); }
            catch (IllegalArgumentException ignored) { s.setStatus(UserSession.Status.ACTIVE); }
        }
        return s;
    }

    private Timestamp toTs(LocalDateTime ldt) {
        return ldt == null ? null : Timestamp.valueOf(ldt);
    }

    private LocalDateTime fromTs(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }
}
