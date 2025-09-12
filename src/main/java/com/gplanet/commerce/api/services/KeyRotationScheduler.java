package com.gplanet.commerce.api.services;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service class that handles scheduled RSA key rotation for JWT token signing.
 * Key rotation enhances security by periodically replacing cryptographic keys.
 * 
 * @author Gustavo
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "oauth2.key-rotation.enabled", havingValue = "true", matchIfMissing = false)
public class KeyRotationScheduler {

    private final KeyStoreService keyStoreService;

    /**
     * Rotates RSA key pairs based on cron expression.
     * This scheduled task generates new cryptographic keys and updates the active key ID.
     */
    @Scheduled(cron = "${oauth2.key-rotation.cron:0 0 2 1 * ?}")
    public void rotateKeys() {
        try {
            log.info("Starting scheduled RSA key rotation");
            keyStoreService.rotateKeyPair();
            log.info("RSA key rotation completed successfully");
        } catch (Exception e) {
            log.error("Failed to rotate RSA keys", e);
        }
    }
}


