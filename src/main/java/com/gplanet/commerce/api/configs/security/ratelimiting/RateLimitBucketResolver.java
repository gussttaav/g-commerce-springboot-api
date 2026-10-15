package com.gplanet.commerce.api.configs.security.ratelimiting;

import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;

import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import com.gplanet.commerce.api.configs.app.props.RateLimitProperties;
import com.gplanet.commerce.api.configs.app.props.RateLimitProperties.Limit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves the rate-limit bucket that a request consumes from.
 * <p>
 * Authenticated requests are limited per user, with the limit of the role the request is authenticated with, so a
 * role change applies as soon as the user authenticates with the new role. Anonymous requests are limited per client
 * IP address, and login attempts have a stricter limit of their own, also per client IP address.
 * </p>
 * <p>
 * The client IP address is the remote address of the request. Behind a reverse proxy, that's the address of the
 * proxy unless {@code server.forward-headers-strategy} is {@code native}, which makes the server take the client
 * address from the {@code X-Forwarded-For} header sent by trusted proxies
 * ({@code server.tomcat.remoteip.internal-proxies}). The header must not be trusted otherwise: any client could set
 * it and get a new bucket on every request.
 * </p>
 * <p>
 * Buckets are kept in memory, so each application instance limits the requests it receives. A bucket is evicted
 * once it has been unused for as long as an empty bucket takes to refill: by then it is full again, so evicting it
 * doesn't reset any limit. The number of buckets is bounded, so clients that cycle through many IP addresses can't
 * exhaust the memory.
 * </p>
 *
 * @author Gustavo
 * @version 1.0
 */
public class RateLimitBucketResolver {

    private static final long MAX_BUCKETS = 100_000;
    private static final String ADMIN_AUTHORITY = "ROLE_ADMIN";

    private final RateLimitProperties properties;
    private final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();
    private final Cache<BucketKey, Bucket> buckets;

    /**
     * Creates a resolver with the given limits.
     *
     * @param properties the rate limits
     */
    public RateLimitBucketResolver(RateLimitProperties properties) {
        this(properties, Ticker.systemTicker());
    }

    /**
     * Creates a resolver with the given limits that measures how long buckets are unused with the given ticker.
     *
     * @param properties the rate limits
     * @param ticker the time source for bucket eviction
     */
    RateLimitBucketResolver(RateLimitProperties properties, Ticker ticker) {
        this.properties = properties;
        this.buckets = Caffeine.newBuilder()
                .expireAfterAccess(longestRefillTime())
                .maximumSize(MAX_BUCKETS)
                .ticker(ticker)
                .build();
    }

    /**
     * Resolves the bucket of a request: the bucket of the authenticated user, with the limit of their role, or the
     * bucket of the client IP address for anonymous requests.
     *
     * @param request the HTTP request
     * @return the bucket the request consumes from
     */
    public Bucket resolveBucket(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // Anonymous requests carry an AnonymousAuthenticationToken, which counts as authenticated
        if (!trustResolver.isAuthenticated(authentication)) {
            return bucket(BucketType.ANONYMOUS, request.getRemoteAddr());
        }
        if (isAdmin(authentication)) {
            return bucket(BucketType.ADMIN, authentication.getName());
        }
        return bucket(BucketType.USER, authentication.getName());
    }

    /**
     * Resolves the bucket of a login attempt: the login bucket of the client IP address.
     *
     * @param request the login request
     * @return the bucket the login attempt consumes from
     */
    public Bucket resolveLoginBucket(HttpServletRequest request) {
        return bucket(BucketType.LOGIN, request.getRemoteAddr());
    }

    private Bucket bucket(BucketType type, String client) {
        return buckets.get(new BucketKey(type, client), key -> createBucket(limit(key.type())));
    }

    private Bucket createBucket(Limit limit) {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(limit.getCapacity(), Refill.greedy(limit.getRefill(), window())))
                .build();
    }

    private Limit limit(BucketType type) {
        return switch (type) {
            case ANONYMOUS -> properties.getUnauthenticated();
            case USER -> properties.getUser();
            case ADMIN -> properties.getAdmin();
            case LOGIN -> properties.getLogin();
        };
    }

    /**
     * Calculates how long the slowest bucket takes to refill completely after being emptied.
     *
     * @return the longest refill time of all the limits
     */
    private Duration longestRefillTime() {
        return Arrays.stream(BucketType.values())
                .map(this::limit)
                .map(limit -> window().multipliedBy(Math.ceilDiv(limit.getCapacity(), limit.getRefill())))
                .max(Comparator.naturalOrder())
                .orElseThrow();
    }

    private Duration window() {
        return Duration.ofMinutes(properties.getWindowMinutes());
    }

    private static boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> ADMIN_AUTHORITY.equals(authority.getAuthority()));
    }

    /**
     * The kinds of buckets, each with its own limit.
     */
    private enum BucketType {
        ANONYMOUS,
        USER,
        ADMIN,
        LOGIN
    }

    /**
     * Identifies a bucket: its kind and the client it limits, a user ID or an IP address. The kind is part of the
     * key, so a user gets a new bucket when their role changes, and login attempts don't consume from the bucket of
     * the client's other requests.
     *
     * @param type the kind of bucket
     * @param client the user ID or IP address of the client
     */
    private record BucketKey(BucketType type, String client) {
    }
}
