package com.gplanet.commerce.api.configs.app.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.mock.env.MockEnvironment;

/**
 * Unit tests for the profiles reported by {@link ApiHealthIndicator}.
 */
class ApiHealthIndicatorTest {

    // Every query of the critical tables check succeeds
    private final DataSource dataSource = mock(DataSource.class, RETURNS_DEEP_STUBS);
    private final MockEnvironment environment = new MockEnvironment();

    @Test
    void healthReportsTheActiveProfiles() {
        environment.setActiveProfiles("https", "metrics");

        Health health = new ApiHealthIndicator(dataSource, environment).health();

        assertThat(health.getDetails()).containsEntry("profile", "https,metrics");
    }

    @Test
    void healthReportsTheDefaultProfileWhenNoProfileIsActive() {
        environment.setDefaultProfiles("https");

        Health health = new ApiHealthIndicator(dataSource, environment).health();

        assertThat(health.getDetails()).containsEntry("profile", "https");
    }
}
