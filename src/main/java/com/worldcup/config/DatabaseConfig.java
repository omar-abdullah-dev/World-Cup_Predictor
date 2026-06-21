package com.worldcup.config;

import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads database vendor configuration from {@code database.properties}
 * on the classpath.
 *
 * Switching between Oracle and PostgreSQL requires changing exactly one
 * property in that file:
 *
 * <pre>
 *   db.vendor=postgres   (default — WildFly / local)
 *   db.vendor=oracle     (WebLogic / production)
 * </pre>
 *
 * No code changes or recompilation are required.
 *
 * Java 8 compatible — no var, no text blocks, no switch expressions.
 */
@ApplicationScoped
public class DatabaseConfig {

    private static final Logger LOG = Logger.getLogger(DatabaseConfig.class.getName());
    private static final String PROPS_FILE = "database.properties";

    /** Recognised vendor tokens. */
    public static final String VENDOR_POSTGRES = "postgres";
    public static final String VENDOR_ORACLE   = "oracle";

    private final Properties props;

    public DatabaseConfig() {
        props = new Properties();
        InputStream is = getClass().getClassLoader().getResourceAsStream(PROPS_FILE);
        if (is == null) {
            LOG.warning("[DatabaseConfig] " + PROPS_FILE + " not found on classpath — "
                    + "defaulting to postgres vendor.");
        } else {
            try {
                props.load(is);
                LOG.info("[DatabaseConfig] Loaded " + PROPS_FILE
                        + " — db.vendor=" + props.getProperty("db.vendor", VENDOR_POSTGRES));
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "[DatabaseConfig] Failed to load " + PROPS_FILE, e);
            } finally {
                try { is.close(); } catch (IOException ignored) {}
            }
        }
    }

    // ── Vendor ────────────────────────────────────────────────────────────

    /**
     * Returns the configured database vendor, defaulting to {@code "postgres"}.
     */
    public String getVendor() {
        return props.getProperty("db.vendor", VENDOR_POSTGRES).trim().toLowerCase();
    }

    public boolean isPostgres() {
        return VENDOR_POSTGRES.equals(getVendor());
    }

    public boolean isOracle() {
        return VENDOR_ORACLE.equals(getVendor());
    }

    // ── Active vendor connection properties ───────────────────────────────

    /**
     * JDBC driver class name for the active vendor.
     */
    public String getDriver() {
        if (isOracle()) {
            return props.getProperty("oracle.driver", "oracle.jdbc.OracleDriver");
        }
        return props.getProperty("postgres.driver", "org.postgresql.Driver");
    }

    /**
     * JDBC URL for the active vendor.
     */
    public String getUrl() {
        if (isOracle()) {
            return props.getProperty("oracle.url",
                    "jdbc:oracle:thin:@localhost:1521/XEPDB1");
        }
        return props.getProperty("postgres.url",
                "jdbc:postgresql://localhost:5432/worldclub");
    }

    /**
     * Database username for the active vendor.
     */
    public String getUsername() {
        if (isOracle()) {
            return props.getProperty("oracle.username", "world_user");
        }
        return props.getProperty("postgres.username", "world_user");
    }

    /**
     * Database password for the active vendor.
     */
    public String getPassword() {
        if (isOracle()) {
            return props.getProperty("oracle.password", "password");
        }
        return props.getProperty("postgres.password", "password");
    }
}
