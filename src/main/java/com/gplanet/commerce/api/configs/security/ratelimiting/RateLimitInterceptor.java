package com.gplanet.commerce.api.configs.security.ratelimiting;

import java.util.function.Function;

import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;

import com.gplanet.commerce.api.exceptions.RateLimitExceededException;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Interceptor that implements rate limiting for API requests.
 * Each request consumes a token from the bucket resolved for it, and is rejected when the bucket is empty.
 * 
 * @author Gustavo
 * @version 1.1
 */
public class RateLimitInterceptor implements HandlerInterceptor {
    private static final long NANOS_PER_SECOND = 1_000_000_000L;
    private final Function<HttpServletRequest, Bucket> bucketResolver;

    /**
     * Constructs a new rate limit interceptor that consumes from the buckets of the given resolver.
     * 
     * @param bucketResolver resolves the bucket each request consumes from
     */
    public RateLimitInterceptor(Function<HttpServletRequest, Bucket> bucketResolver) {
        this.bucketResolver = bucketResolver;
    }

    /**
     * Handles the rate limiting before the request is processed.
     * Adds rate limit headers and throws exception if limit is exceeded.
     * 
     * @param request the HTTP request
     * @param response the HTTP response
     * @param handler the handler for the request
     * @return true if the request can proceed, false otherwise
     * @throws Exception if rate limit is exceeded or other error occurs
     */
    @Override
    public boolean preHandle(
        @NonNull HttpServletRequest request, 
        @NonNull HttpServletResponse response, 
        @NonNull Object handler
    ) throws Exception {
        Bucket bucket = bucketResolver.apply(request);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return true;
        } else {
            // Rounded up, so a client that waits that long finds a token
            long waitForRefill = Math.ceilDiv(probe.getNanosToWaitForRefill(), NANOS_PER_SECOND);
            response.addHeader(HttpHeaders.RETRY_AFTER, String.valueOf(waitForRefill));
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
            throw new RateLimitExceededException("Rate limit exceeded");
        }
    }
}
