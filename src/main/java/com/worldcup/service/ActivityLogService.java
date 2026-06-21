package com.worldcup.service;

import com.worldcup.model.SystemActivityLog;
import com.worldcup.repository.JdbcHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persists system activity entries to the system_activity_log table.
 * Pure JDBC — no JPA/EntityManager.
 *
 * Append-only — records are never updated or deleted.
 * Every call to log() is never throws so it never breaks the caller.
 */
@ApplicationScoped
public class ActivityLogService {

    private static final Logger LOG = Logger.getLogger(ActivityLogService.class.getName());

    @Inject
    private JdbcHelper jdbc;

    // ── Write ─────────────────────────────────────────────────────────────

    /** Simple 3-arg overload — backward compatible with existing callers. */
    public void log(String opmaj, String transmaj, String profilemaj) {
        log(opmaj, transmaj, profilemaj, null, null, null, null, null, null, null);
    }

    /** Full 10-arg overload with session/audit context. */
    public void log(String opmaj, String transmaj, String profilemaj,
                    Long userId, String sessionId,
                    String ipAddress, String userAgent,
                    Long matchId, String oldValue, String newValue) {
        String sql = "INSERT INTO system_activity_log "
                + "(opmaj, datemaj, transmaj, profilemaj, user_id, session_id, "
                + " ip_address, user_agent, match_id, old_value, new_value) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, opmaj);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(3, transmaj);
            ps.setString(4, profilemaj);
            setNullableLong(ps, 5, userId);
            ps.setString(6, sessionId);
            ps.setString(7, ipAddress);
            ps.setString(8, userAgent);
            setNullableLong(ps, 9, matchId);
            ps.setString(10, oldValue);
            ps.setString(11, newValue);
            ps.executeUpdate();
        } catch (Exception e) {
            // Never let logging break the caller
            LOG.log(Level.WARNING, "[ActivityLogService] Failed to persist log entry opmaj="
                    + opmaj + " profile=" + profilemaj + ": " + e.getMessage(), e);
        }
    }

    // ── Read ──────────────────────────────────────────────────────────────

    public List<SystemActivityLog> findAll() {
        String sql = "SELECT * FROM system_activity_log ORDER BY datemaj DESC";
        return query(sql);
    }

    public List<SystemActivityLog> findByProfile(String profilemaj) {
        String sql = "SELECT * FROM system_activity_log WHERE profilemaj = ? ORDER BY datemaj DESC";
        List<SystemActivityLog> list = new ArrayList<SystemActivityLog>();
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, profilemaj);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "findByProfile failed", e);
        }
        return list;
    }

    public List<SystemActivityLog> findByOperation(String opmaj) {
        return findFiltered(null, opmaj);
    }

    public List<SystemActivityLog> findFiltered(String profilemaj, String opmaj) {
        boolean hasProfile = profilemaj != null && !profilemaj.trim().isEmpty();
        boolean hasOp      = opmaj      != null && !opmaj.trim().isEmpty();

        StringBuilder sql = new StringBuilder(
                "SELECT * FROM system_activity_log WHERE 1=1");
        if (hasProfile) sql.append(" AND LOWER(profilemaj) LIKE LOWER(?)");
        if (hasOp)      sql.append(" AND opmaj = ?");
        sql.append(" ORDER BY datemaj DESC");

        List<SystemActivityLog> list = new ArrayList<SystemActivityLog>();
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            int idx = 1;
            if (hasProfile) ps.setString(idx++, "%" + profilemaj + "%");
            if (hasOp)      ps.setString(idx,   opmaj);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "findFiltered failed", e);
        }
        return list;
    }

    public List<SystemActivityLog> findRecent(int limit) {
        // FETCH FIRST n ROWS ONLY is portable across Oracle and PostgreSQL
        String sql = "SELECT * FROM system_activity_log ORDER BY datemaj DESC FETCH FIRST " + limit + " ROWS ONLY";
        return query(sql);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private List<SystemActivityLog> query(String sql) {
        List<SystemActivityLog> list = new ArrayList<SystemActivityLog>();
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "query failed sql=" + sql, e);
        }
        return list;
    }

    private SystemActivityLog map(ResultSet rs) throws SQLException {
        SystemActivityLog entry = new SystemActivityLog();
        entry.setId(rs.getLong("id"));
        entry.setOpmaj(rs.getString("opmaj"));
        Timestamp ts = rs.getTimestamp("datemaj");
        entry.setDatemaj(ts != null ? ts.toLocalDateTime() : LocalDateTime.now());
        entry.setTransmaj(rs.getString("transmaj"));
        entry.setProfilemaj(rs.getString("profilemaj"));
        long uid = rs.getLong("user_id");
        entry.setUserId(rs.wasNull() ? null : uid);
        entry.setSessionId(rs.getString("session_id"));
        entry.setIpAddress(rs.getString("ip_address"));
        entry.setUserAgent(rs.getString("user_agent"));
        long mid = rs.getLong("match_id");
        entry.setMatchId(rs.wasNull() ? null : mid);
        entry.setOldValue(rs.getString("old_value"));
        entry.setNewValue(rs.getString("new_value"));
        return entry;
    }

    private void setNullableLong(PreparedStatement ps, int idx, Long val) throws SQLException {
        if (val == null) ps.setNull(idx, Types.BIGINT);
        else ps.setLong(idx, val);
    }
}
