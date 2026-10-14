package com.gplanet.commerce.api.configs.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables scheduled task execution when automatic key rotation is enabled, so that
 * {@link com.gplanet.commerce.api.services.KeyRotationScheduler} runs on its cron schedule.
 *
 * @author Gustavo
 * @version 1.0
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "oauth2.key-rotation.enabled", havingValue = "true", matchIfMissing = false)
public class KeyRotationConfig {
}
