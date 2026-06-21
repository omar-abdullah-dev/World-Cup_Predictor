package com.worldcup.repository;

import com.worldcup.config.ConnectionFactory;
import com.worldcup.config.DatabaseConfig;

import javax.sql.DataSource;
import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central JDBC connection provider for all pure-SQL repositories.
 *
 * Connection acquisition order (JNDI first, DriverManager fallback):
 *
 *   1. JNDI DataSource — {@code java:jboss/datasources/WorldCupDS}
 *      Used on WildFly (PostgreSQL) and WebLogic (Oracle) in production/staging.
 *      This is always preferred when the JNDI name is available.
 *
 *   2. DriverManager fallback — {@link ConnectionFactory}
 *      Used when no JNDI DataSource is registered (standalone testing,
 *      local Oracle XE, integration tests).
 *      The active vendor is determined by {@code db.vendor} in
 *      {@code database.properties} — no code change required.
 *
 * Switching between Oracle and PostgreSQL:
 *   - On an app server: configure the DataSource in the server's admin console.
 *   - Standalone: set {@code db.vendor=oracle} or {@code db.vendor=postgres}
 *     in {@code src/main/resources/database.properties}.
 *
 * Java 8 compatible — no var, no switch expressions.
 */
@ApplicationScoped
public class JdbcHelper {

    private static final Logger LOG = Logger.getLogger(JdbcHelper.class.getName());

    /**
     * JNDI DataSource — injected by the application server.
     * Will be {@code null} when running outside a full Jakarta EE container.
     */
    @Resource(lookup = "java:jboss/datasources/WorldCupDS")
    private DataSource dataSource;

    /**
     * DriverManager-based fallback for standalone / test execution.
     * CDI injects this; it reads {@code database.properties} at startup.
     */
    @Inject
    private ConnectionFactory connectionFactory;

    /**
     * CDI + vendor config — used for isOracle() / isPostgres() helpers.
     */
    @Inject
    private DatabaseConfig databaseConfig;

    /**
     * Returns an open JDBC connection.
     *
     * Tries JNDI first; falls back to {@link ConnectionFactory} if the
     * JNDI DataSource is unavailable.
     *
     * The caller must close the connection (use try-with-resources).
     *
     * @throws SQLException if neither source can provide a connection
     */
    public Connection getConnection() throws SQLException {
        // ── 1. JNDI DataSource (preferred — app-server managed) ───────────
        if (dataSource != null) {
            try {
                Connection c = dataSource.getConnection();
                if (c != null) {
                    LOG.fine("[JdbcHelper] Connection obtained from JNDI DataSource.");
                    return c;
                }
            } catch (SQLException e) {
                LOG.log(Level.WARNING,
                        "[JdbcHelper] JNDI DataSource.getConnection() failed — "
                        + "falling back to DriverManager. Cause: " + e.getMessage(), e);
            }
        }

        // ── 2. DriverManager fallback (standalone / local testing) ────────
        if (connectionFactory == null) {
            throw new SQLException("[JdbcHelper] No JNDI DataSource and ConnectionFactory is null. "
                    + "Check CDI injection and database.properties.");
        }
        LOG.fine("[JdbcHelper] JNDI DataSource unavailable — using DriverManager fallback.");
        return connectionFactory.getConnection();
    }

    // ── Vendor helpers ────────────────────────────────────────────────────

    /**
     * Returns {@code true} if the active database vendor is Oracle.
     * Repositories can use this to emit Oracle-specific SQL fragments.
     */
    public boolean isOracle() {
        return databaseConfig != null && databaseConfig.isOracle();
    }

    /**
     * Returns {@code true} if the active database vendor is PostgreSQL.
     */
    public boolean isPostgres() {
        return databaseConfig == null || databaseConfig.isPostgres();
    }

    /**
     * Returns a vendor-portable "select first N rows" clause suffix.
     *
     * PostgreSQL: {@code LIMIT n}
     * Oracle:     {@code FETCH FIRST n ROWS ONLY}
     *
     * Usage:
     * <pre>
     *   String sql = "SELECT * FROM foo ORDER BY id DESC " + jdbc.limitClause(10);
     * </pre>
     */
    public String limitClause(int n) {
        if (isOracle()) {
            return "FETCH FIRST " + n + " ROWS ONLY";
        }
        return "LIMIT " + n;
    }
}
