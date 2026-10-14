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
 * Service class that handles scheduled RSA key rotation for JWT token signing.
 * Key rotation enhances security by periodically replacing cryptographic keys.
 * <p>
 * Every instance runs the schedule, so a Redis lock makes sure only one of them rotates the keys.
 * </p>
 *
 * @author Gustavo
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "oauth2.key-rotation.enabled", havingValue = "true", matchIfMissing = false)
public class KeyRotationScheduler {

    private static final String ROTATION_LOCK = "oauth2:key_rotation_lock";

    /**
     * How long the rotation lock is held. It is not released after rotating, so an instance whose clock is
     * slightly behind cannot rotate the keys a second time for the same scheduled run.
     */
    private static final Duration ROTATION_LOCK_DURATION = Duration.ofMinutes(1);

    private final KeyStoreService keyStoreService;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Rotates RSA key pairs based on cron expression.
     * This scheduled task generates new cryptographic keys and updates the active key ID.
     */
    @Scheduled(cron = "${oauth2.key-rotation.cron:0 0 2 1 * ?}")
    public void rotateKeys() {
        try {
            if (!acquireRotationLock()) {
                log.info("Skipping RSA key rotation: another instance is already rotating the keys");
                return;
            }
            log.info("Starting scheduled RSA key rotation");
            keyStoreService.rotateKeyPair();
            log.info("RSA key rotation completed successfully");
        } catch (Exception e) {
            log.error("Failed to rotate RSA keys", e);
        }
    }

    private boolean acquireRotationLock() {
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(ROTATION_LOCK, Instant.now().toString(), ROTATION_LOCK_DURATION);
        return Boolean.TRUE.equals(acquired);
    }
}
