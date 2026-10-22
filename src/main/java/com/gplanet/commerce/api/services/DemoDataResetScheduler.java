package com.gplanet.commerce.api.services;

import java.time.Duration;
import java.time.Instant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service class that resets the demo data of the public deployment on a schedule ({@code demo.reset.cron}, in
 * UTC), so whatever visitors change lasts one day at most.
 * <p>
 * Every instance runs the schedule, so a Redis lock makes sure only one of them resets the data.
 * </p>
 *
 * @author Gustavo
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "demo.reset.enabled", havingValue = "true", matchIfMissing = false)
public class DemoDataResetScheduler {

    private static final String RESET_LOCK = "demo:reset_lock";

    /**
     * How long the reset lock is held. It is not released after the reset, so an instance whose clock is
     * slightly behind cannot reset the data a second time for the same scheduled run.
     */
    private static final Duration RESET_LOCK_DURATION = Duration.ofMinutes(1);

    private final DemoDataService demoDataService;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Restores the demo data, unless another instance is already doing it.
     */
    @Scheduled(cron = "${demo.reset.cron:0 0 3 * * *}", zone = "UTC")
    public void resetDemoData() {
        try {
            if (!acquireResetLock()) {
                log.info("Skipping the demo data reset: another instance is already resetting it");
                return;
            }
            log.info("Starting the scheduled demo data reset");
            demoDataService.resetDemoData();
        } catch (Exception e) {
            log.error("Failed to reset the demo data", e);
        }
    }

    private boolean acquireResetLock() {
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(RESET_LOCK, Instant.now().toString(), RESET_LOCK_DURATION);
        return Boolean.TRUE.equals(acquired);
    }
}
