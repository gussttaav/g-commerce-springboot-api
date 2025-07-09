package com.gplanet.commerce_api.configs.app.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Health indicator for the G-Commerce API service.
 * Monitors the overall health of the application by checking critical
 * components including database tables availability and application status.
 *
 * @author Gustavo
 * @version 1.0
 */
@Component("commerceApi")
public class ApiHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;
    private final Environment environment;

    public ApiHealthIndicator(DataSource dataSource, Environment environment) {
        this.dataSource = dataSource;
        this.environment = environment;
    }

    /**
     * Evaluates the health status of the G-Commerce API.
     * Checks the application version, active profiles, and critical database tables.
     *
     * @return Health object containing the service status and relevant details
     */
    @Override
    public Health health() {
        try {
            Health.Builder healthBuilder = Health.up();
            
            Package pkg = getClass().getPackage();
            String version = (pkg != null && pkg.getImplementationVersion() != null) 
                ? pkg.getImplementationVersion() : "unknown";
            
            // Safe profile check
            String[] activeProfiles = environment != null ? environment.getActiveProfiles() : new String[0];
            String profiles = activeProfiles.length > 0 
                ? String.join(",", activeProfiles) : "default";

            healthBuilder
                    .withDetail("service", "G-Commerce API")
                    .withDetail("version", version)
                    .withDetail("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .withDetail("profile", profiles);

            // Check critical tables
            if (checkCriticalTables()) {
                healthBuilder.withDetail("criticalTables", "Available");
            } else {
                return Health.down()
                        .withDetail("service", "G-Commerce API")
                        .withDetail("error", "Critical tables not accessible")
                        .build();
            }

            return healthBuilder.build();
            
        } catch (Exception e) {
            return Health.down()
                    .withDetail("service", "G-Commerce API")
                    .withDetail("error", e.getMessage() != null ? e.getMessage() : "Unknown error")
                    .withDetail("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .build();
        }
    }

    /**
     * Verifies the existence and accessibility of critical database tables.
     *
     * @return true if all critical tables are accessible, false otherwise
     */
    private boolean checkCriticalTables() {
        try (Connection connection = dataSource.getConnection()) {
            // Check if critical tables exist and are accessible
            String[] criticalTables = {"usuario", "productos", "compras", "compra_productos"};
            
            for (String table : criticalTables) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?")) {
                    statement.setString(1, table);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next() && resultSet.getInt(1) == 0) {
                            return false; // Table doesn't exist
                        }
                    }
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}