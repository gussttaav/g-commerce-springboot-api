package com.gplanet.commerce_api.configs.app.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component("customDatabase")
public class DatabaseHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    public DatabaseHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        try {
            return checkDatabaseHealth();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("database", "MySQL")
                    .withDetail("error", e.getMessage())
                    .withDetail("status", "Connection failed")
                    .build();
        }
    }

    private Health checkDatabaseHealth() throws SQLException {
        long startTime = System.currentTimeMillis();
        
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(5)) {
                // Test a simple query to ensure database is responsive
                try (PreparedStatement statement = connection.prepareStatement("SELECT 1")) {
                    try (ResultSet resultSet = statement.executeQuery()) {
                        long responseTime = System.currentTimeMillis() - startTime;
                        
                        return Health.up()
                                .withDetail("database", "MySQL")
                                .withDetail("status", "Connected")
                                .withDetail("responseTime", responseTime + "ms")
                                .withDetail("validationQuery", "SELECT 1")
                                .withDetail("connectionValid", true)
                                .build();
                    }
                }
            } else {
                return Health.down()
                        .withDetail("database", "MySQL")
                        .withDetail("status", "Connection invalid")
                        .withDetail("connectionValid", false)
                        .build();
            }
        }
    }
}