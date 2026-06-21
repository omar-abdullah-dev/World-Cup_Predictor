package com.worldcup.repository;

import com.worldcup.model.Group;
import com.worldcup.model.RoundStatus;
import com.worldcup.model.Team;
import com.worldcup.model.TournamentRound;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Pure JDBC implementation of GroupRepository.
 * Groups have a many-to-many join with teams (group_teams table)
 * and an optional FK to tournament_rounds.
 */
@ApplicationScoped
public class JpaGroupRepository implements GroupRepository {

    private static final Logger LOG = Logger.getLogger(JpaGroupRepository.class.getName());

    @Inject private JdbcHelper jdbc;
    @Inject private JpaTournamentRoundRepository roundRepo;
    @Inject private JpaTeamRepository teamRepo;

    @Override
    public Group save(Group group) {
        String sql = "INSERT INTO groups_table (name, status, round_id) VALUES (?,?,?)";
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, group.getName());
            ps.setString(2, group.getStatus() != null ? group.getStatus().name() : RoundStatus.OPEN.name());
            if (group.getRound() != null && group.getRound().getId() != null) {
                ps.setLong(3, group.getRound().getId());
            } else {
                ps.setNull(3, Types.BIGINT);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) group.setId(keys.getLong(1));
            }
            saveGroupTeams(c, group);
            return group;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "save(Group) failed", e);
            throw new RuntimeException("Failed to save group: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Group> findById(Long id) {
        String sql = "SELECT * FROM groups_table WHERE id = ?";
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapWithRelations(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "findById(Group) failed id=" + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Group> findByName(String name) {
        String sql = "SELECT * FROM groups_table WHERE name = ?";
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapWithRelations(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "findByName(Group) failed name=" + name, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Group> findAll() {
        String sql = "SELECT * FROM groups_table ORDER BY name";
        List<Group> list = new ArrayList<Group>();
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapWithRelations(rs));
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "findAll(Group) failed", e);
        }
        return list;
    }

    @Override
    public Group update(Group group) {
        String sql = "UPDATE groups_table SET name=?, status=?, round_id=? WHERE id=?";
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, group.getName());
            ps.setString(2, group.getStatus() != null ? group.getStatus().name() : RoundStatus.OPEN.name());
            if (group.getRound() != null && group.getRound().getId() != null) {
                ps.setLong(3, group.getRound().getId());
            } else {
                ps.setNull(3, Types.BIGINT);
            }
            ps.setLong(4, group.getId());
            ps.executeUpdate();
            // Sync group_teams: delete all then re-insert
            deleteGroupTeams(c, group.getId());
            saveGroupTeams(c, group);
            return group;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "update(Group) failed id=" + group.getId(), e);
            throw new RuntimeException("Failed to update group: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        try (Connection c = jdbc.getConnection()) {
            deleteGroupTeams(c, id);
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM groups_table WHERE id = ?")) {
                ps.setLong(1, id);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "deleteById(Group) failed id=" + id, e);
            return false;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Group mapWithRelations(ResultSet rs) throws SQLException {
        Group g = new Group();
        g.setId(rs.getLong("id"));
        g.setName(rs.getString("name"));
        String status = rs.getString("status");
        g.setStatus(status != null ? RoundStatus.valueOf(status) : RoundStatus.OPEN);
        long roundId = rs.getLong("round_id");
        if (!rs.wasNull()) {
            roundRepo.findById(roundId).ifPresent(g::setRound);
        }
        g.setTeams(loadTeamsForGroup(g.getId()));
        return g;
    }

    private List<Team> loadTeamsForGroup(Long groupId) {
        String sql = "SELECT team_id FROM group_teams WHERE group_id = ?";
        List<Team> teams = new ArrayList<Team>();
        try (Connection c = jdbc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long tid = rs.getLong("team_id");
                    teamRepo.findById(tid).ifPresent(teams::add);
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "loadTeamsForGroup failed groupId=" + groupId, e);
        }
        return teams;
    }

    private void saveGroupTeams(Connection c, Group group) throws SQLException {
        if (group.getId() == null || group.getTeams() == null) return;
        // Portable insert: check existence first to avoid ON CONFLICT (PostgreSQL-only).
        // Works on Oracle and PostgreSQL without any SQL differences.
        String checkSql  = "SELECT COUNT(*) FROM group_teams WHERE group_id = ? AND team_id = ?";
        String insertSql = "INSERT INTO group_teams (group_id, team_id) VALUES (?,?)";
        try (PreparedStatement chk = c.prepareStatement(checkSql);
             PreparedStatement ins = c.prepareStatement(insertSql)) {
            for (Team t : group.getTeams()) {
                if (t.getId() == null) continue;
                chk.setLong(1, group.getId());
                chk.setLong(2, t.getId());
                try (ResultSet rs = chk.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        ins.setLong(1, group.getId());
                        ins.setLong(2, t.getId());
                        ins.addBatch();
                    }
                }
            }
            ins.executeBatch();
        }
    }

    private void deleteGroupTeams(Connection c, Long groupId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM group_teams WHERE group_id = ?")) {
            ps.setLong(1, groupId);
            ps.executeUpdate();
        }
    }
}
