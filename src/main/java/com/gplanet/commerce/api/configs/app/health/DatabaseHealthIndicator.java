package com.gplanet.commerce.api.configs.app.health;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom database health indicator for the G-Commerce API.
 * Monitors the database connection status and performance by
 * performing connection tests and simple queries.
 *
 * @author Gustavo
 * @version 1.0
 */
@Component("customDatabase")
public class DatabaseHealthIndicator implements HealthIndicator {

    private static final String DB_KEY = "database";
    private static final String DB_NAME = "MySQL";
    private static final String STATUS_KEY = "status";

    private final DataSource dataSource;

    /**
     * Constructs the DatabaseHealthIndicator with the required DataSource.
     *
     * @param dataSource the DataSource to check database health
     */
    public DatabaseHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Evaluates the health status of the database connection.
     * Performs connection validation and basic query testing.
     *
     * @return Health object containing the database status and connection details
     */
    @Override
    public Health health() {
        try {
            return checkDatabaseHealth();
        } catch (Exception e) {
            return Health.down()
                    .withDetail(DB_KEY, DB_NAME)
                    .withDetail("error", e.getMessage())
                    .withDetail(STATUS_KEY, "Connection failed")
                    .build();
        }
    }

    /**
     * Performs detailed database health checks including connection validation
     * and response time measurement.
     *
     * @return Health object with detailed database connection information
     * @throws SQLException if database connection or query execution fails
     */
    private Health checkDatabaseHealth() throws SQLException {
        long startTime = System.currentTimeMillis();

        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(5)) {
                // Test a simple query to ensure database is responsive
                try (PreparedStatement statement = connection.prepareStatement("SELECT 1")) {
                    try (ResultSet resultSet = statement.executeQuery()) {
                        long responseTime = System.currentTimeMillis() - startTime;

                        return Health.up()
                                .withDetail(DB_KEY, DB_NAME)
                                .withDetail(STATUS_KEY, "Connected")
                                .withDetail("responseTime", responseTime + "ms")
                                .withDetail("validationQuery", "SELECT 1")
                                .withDetail("connectionValid", true)
                                .build();
                    }
                }
            } else {
                return Health.down()
                        .withDetail(DB_KEY, DB_NAME)
                        .withDetail(STATUS_KEY, "Connection invalid")
                        .withDetail("connectionValid", false)
                        .build();
            }
        }
    }
}
