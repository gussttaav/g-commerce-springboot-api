package com.gplanet.commerce.api.services;

import java.time.Duration;
import java.time.Instant;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Service that keeps the blacklist of revoked access tokens in Redis.
 * <p>
 * Tokens are identified by their {@code jti} claim, and each entry expires together with its token, so the
 * blacklist only holds tokens that would otherwise still be accepted.
 * </p>
 *
 * @author Gustavo
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "token:blacklist:";
    private static final String BLACKLISTED = "blacklisted";

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Blacklists a token until it expires. A token that has already expired is ignored.
     *
     * @param token the token to blacklist
     */
    public void blacklist(Jwt token) {
        Duration remainingLifetime = Duration.between(Instant.now(), token.getExpiresAt());
        if (remainingLifetime.isPositive()) {
            stringRedisTemplate.opsForValue().set(BLACKLIST_PREFIX + token.getId(), BLACKLISTED, remainingLifetime);
        }
    }

    /**
     * Checks whether a token is blacklisted.
     *
     * @param tokenId the {@code jti} claim of the token
     * @return true if the token is blacklisted
     */
    public boolean isBlacklisted(String tokenId) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(BLACKLIST_PREFIX + tokenId));
    }
}
