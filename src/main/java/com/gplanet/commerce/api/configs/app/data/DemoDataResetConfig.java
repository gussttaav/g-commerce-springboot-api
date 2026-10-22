package com.gplanet.commerce.api.configs.app.data;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables scheduled task execution when the demo data reset is enabled, so that
 * {@link com.gplanet.commerce.api.services.DemoDataResetScheduler} runs on its cron schedule.
 *
 * @author Gustavo
 * @version 1.0
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "demo.reset.enabled", havingValue = "true", matchIfMissing = false)
public class DemoDataResetConfig {
}
