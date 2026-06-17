package com.worldcup.repository;

import javax.sql.DataSource;
import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Provides a shared JDBC DataSource for all pure-SQL repositories.
 * Injected via @Resource JNDI lookup from the WildFly datasource.
 */
@ApplicationScoped
public class JdbcHelper {

    @Resource(lookup = "java:jboss/datasources/WorldCupDS")
    private DataSource dataSource;

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
