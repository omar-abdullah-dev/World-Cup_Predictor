package com.worldcup.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates JDBC connections using {@link DriverManager}.
 *
 * This is the FALLBACK connection mechanism used when no JNDI DataSource
 * is available (e.g., standalone execution, local Oracle XE testing).
 *
 * Production deployments on WildFly and WebLogic use the JNDI DataSource
 * configured in {@link com.worldcup.repository.JdbcHelper} via {@code @Resource}.
 * The JNDI path always takes priority over this factory.
 *
 * Switching between Oracle and PostgreSQL requires only:
 * <pre>
 *   db.vendor=oracle   or   db.vendor=postgres
 * </pre>
 * in {@code database.properties} — no code change needed.
 *
 * Java 8 compatible.
 */
@ApplicationScoped
public class ConnectionFactory {

    private static final Logger LOG = Logger.getLogger(ConnectionFactory.class.getName());

    @Inject
    private DatabaseConfig config;

    /**
     * Opens and returns a new JDBC connection using DriverManager.
     * The caller is responsible for closing the connection.
     *
     * @return an open {@link Connection}
     * @throws SQLException if the connection cannot be established
     */
    public Connection getConnection() throws SQLException {
        String driver = config.getDriver();
        String url    = config.getUrl();
        String user   = config.getUsername();
        String pass   = config.getPassword();

        // Load driver class (required for Java 8 DriverManager)
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            LOG.log(Level.SEVERE, "[ConnectionFactory] JDBC driver not found: " + driver
                    + ". Ensure the driver JAR is on the classpath.", e);
            throw new SQLException("JDBC driver class not found: " + driver, e);
        }

        LOG.fine("[ConnectionFactory] Opening connection vendor=" + config.getVendor()
                + " url=" + url + " user=" + user);

        Connection conn = DriverManager.getConnection(url, user, pass);
        conn.setAutoCommit(true); // default; services control transactions explicitly
        return conn;
    }

    /**
     * Returns {@code true} if the active vendor is Oracle.
     */
    public boolean isOracle() {
        return config.isOracle();
    }

    /**
     * Returns {@code true} if the active vendor is PostgreSQL.
     */
    public boolean isPostgres() {
        return config.isPostgres();
    }
}
