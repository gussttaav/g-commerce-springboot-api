package com.gplanet.commerce.api.configs.app.health;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.sql.DataSource;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

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

    private static final String SERVICE_KEY = "service";
    private static final String SERVICE_NAME = "G-Commerce API";

    private final DataSource dataSource;
    private final Environment environment;

    /**
     * Constructs the ApiHealthIndicator with the required DataSource and Environment.
     *
     * @param dataSource   the DataSource to check database health
     * @param environment  the Spring Environment to get active profiles
     */
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
            Package pkg = getClass().getPackage();
            String version;
            if (pkg != null && pkg.getImplementationVersion() != null) {
                version = pkg.getImplementationVersion();
            } else {
                version = "unknown";
            }

            // Safe profile check
            String[] activeProfiles;
            if (environment != null) {
                activeProfiles = environment.getActiveProfiles();
            } else {
                activeProfiles = new String[0];
            }
            String profiles;
            if (activeProfiles.length > 0) {
                profiles = String.join(",", activeProfiles);
            } else {
                profiles = "default";
            }

            Health.Builder healthBuilder = Health.up();
            healthBuilder
                    .withDetail(SERVICE_KEY, SERVICE_NAME)
                    .withDetail("version", version)
                    .withDetail("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .withDetail("profile", profiles);

            // Check critical tables
            boolean criticalTablesOk = checkCriticalTables();
            if (criticalTablesOk) {
                healthBuilder.withDetail("criticalTables", "Available");
            } else {
                return Health.down()
                        .withDetail(SERVICE_KEY, SERVICE_NAME)
                        .withDetail("error", "Critical tables not accessible")
                        .build();
            }

            return healthBuilder.build();

        } catch (Exception e) {
            String errorMsg;
            if (e.getMessage() != null) {
                errorMsg = e.getMessage();
            } else {
                errorMsg = "Unknown error";
            }
            return Health.down()
                    .withDetail(SERVICE_KEY, SERVICE_NAME)
                    .withDetail("error", errorMsg)
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
                        "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = DATABASE() AND table_name = ?")) {
                    statement.setString(1, table);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            int count = resultSet.getInt(1);
                            if (count == 0) {
                                return false; // Table doesn't exist
                            }
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
