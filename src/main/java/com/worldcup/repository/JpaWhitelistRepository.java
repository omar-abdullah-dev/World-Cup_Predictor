package com.worldcup.repository;

import com.worldcup.model.WhitelistEntry;
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
 * Pure JDBC implementation of WhitelistRepository.
 *
 * NOTE: The actual DB column name is "ad_username" (no underscore),
 * matching how PostgreSQL created it from the legacy Hibernate entity.
 * All SQL in this class uses the real DB column name "ad_username".
 */
@ApplicationScoped
public class JpaWhitelistRepository implements WhitelistRepository {

    private static final Logger LOG = Logger.getLogger(JpaWhitelistRepository.class.getName());

    @Inject
    private JdbcHelper jdbc;

    @Override
    public WhitelistEntry save(WhitelistEntry entry) {
        String sql = "INSERT INTO whitelist (ad_username, employee_name, email, enabled, added_at, added_by_user_id) "
                + "VALUES (?,?,?,?,?,?)";
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, entry.getAdUsername());
            ps.setString(2, entry.getEmployeeName());
            ps.setString(3, entry.getEmail());
            ps.setBoolean(4, entry.isEnabled());
            ps.setTimestamp(5, entry.getAddedAt() != null
                    ? Timestamp.valueOf(entry.getAddedAt())
                    : Timestamp.valueOf(LocalDateTime.now()));
            if (entry.getAddedByUserId() != null) {
                ps.setLong(6, entry.getAddedByUserId());
            } else {
                ps.setNull(6, Types.BIGINT);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) entry.setId(keys.getLong(1));
            }
            return entry;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "save(WhitelistEntry) failed", e);
            throw new RuntimeException("Failed to save whitelist entry: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<WhitelistEntry> findById(Long id) {
        String sql = "SELECT * FROM whitelist WHERE id = ?";
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "findById(WhitelistEntry) failed id=" + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<WhitelistEntry> findByAdUsername(String adUsername) {
        // Case-insensitive match — handles both upper and lower case QNB usernames
        String sql = "SELECT * FROM whitelist WHERE LOWER(ad_username) = LOWER(?)";
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, adUsername);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "findByAdUsername(WhitelistEntry) failed", e);
        }
        return Optional.empty();
    }

    @Override
    public List<WhitelistEntry> findAll() {
        String sql = "SELECT * FROM whitelist ORDER BY added_at DESC";
        List<WhitelistEntry> list = new ArrayList<WhitelistEntry>();
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "findAll(WhitelistEntry) failed", e);
        }
        return list;
    }

    @Override
    public WhitelistEntry update(WhitelistEntry entry) {
        String sql = "UPDATE whitelist SET ad_username=?, employee_name=?, email=?, enabled=?, "
                + "added_by_user_id=? WHERE id=?";
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, entry.getAdUsername());
            ps.setString(2, entry.getEmployeeName());
            ps.setString(3, entry.getEmail());
            ps.setBoolean(4, entry.isEnabled());
            if (entry.getAddedByUserId() != null) {
                ps.setLong(5, entry.getAddedByUserId());
            } else {
                ps.setNull(5, Types.BIGINT);
            }
            ps.setLong(6, entry.getId());
            ps.executeUpdate();
            return entry;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "update(WhitelistEntry) failed id=" + entry.getId(), e);
            throw new RuntimeException("Failed to update whitelist entry: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        String sql = "DELETE FROM whitelist WHERE id = ?";
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "deleteById(WhitelistEntry) failed id=" + id, e);
            return false;
        }
    }

    private WhitelistEntry map(ResultSet rs) throws SQLException {
        WhitelistEntry e = new WhitelistEntry();
        e.setId(rs.getLong("id"));
        // DB column is "ad_username" (no underscore)
        e.setAdUsername(rs.getString("ad_username"));
        e.setEmployeeName(rs.getString("employee_name"));
        e.setEmail(rs.getString("email"));
        e.setEnabled(rs.getBoolean("enabled"));
        Timestamp addedAt = rs.getTimestamp("added_at");
        e.setAddedAt(addedAt != null ? addedAt.toLocalDateTime() : null);
        long addedBy = rs.getLong("added_by_user_id");
        e.setAddedByUserId(rs.wasNull() ? null : addedBy);
        return e;
    }
}
