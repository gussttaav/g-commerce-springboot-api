package com.gplanet.commerce.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    private static final String TOKEN_ID = "token-id";
    private static final String BLACKLIST_ENTRY = "token:blacklist:" + TOKEN_ID;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TokenBlacklistService tokenBlacklistService;

    @Test
    void blacklistKeepsTokenUntilItExpires() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        Duration remainingLifetime = Duration.ofMinutes(30);

        tokenBlacklistService.blacklist(tokenExpiringAt(Instant.now().plus(remainingLifetime)));

        ArgumentCaptor<Duration> ttl = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(eq(BLACKLIST_ENTRY), anyString(), ttl.capture());
        assertThat(ttl.getValue()).isPositive().isLessThanOrEqualTo(remainingLifetime);
    }

    @Test
    void blacklistIgnoresExpiredToken() {
        tokenBlacklistService.blacklist(tokenExpiringAt(Instant.now().minusSeconds(1)));

        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void isBlacklistedChecksTokenId() {
        when(stringRedisTemplate.hasKey(BLACKLIST_ENTRY)).thenReturn(true);

        assertThat(tokenBlacklistService.isBlacklisted(TOKEN_ID)).isTrue();
        assertThat(tokenBlacklistService.isBlacklisted("another-token-id")).isFalse();
    }

    private static Jwt tokenExpiringAt(Instant expiresAt) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .jti(TOKEN_ID)
                .issuedAt(expiresAt.minus(Duration.ofHours(1)))
                .expiresAt(expiresAt)
                .build();
    }
}
