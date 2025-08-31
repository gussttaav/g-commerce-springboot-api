package com.gplanet.commerce.api.configs.security.ratelimiting;

import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;

import com.gplanet.commerce.api.exceptions.RateLimitExceededException;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Interceptor that implements rate limiting for API requests.
 * Uses token bucket algorithm to control request rates based on user roles.
 * 
 * @author Gustavo
 * @version 1.0
 */
public class RateLimitInterceptor implements HandlerInterceptor {
    private static final long NANOS_PER_SECOND = 1_000_000_000L;
    private final RateLimitingConfig rateLimitingConfig;

    /**
     * Constructs a new rate limit interceptor with the specified configuration.
     * 
     * @param rateLimitingConfig the configuration containing rate limit settings
     */
    public RateLimitInterceptor(RateLimitingConfig rateLimitingConfig) {
        this.rateLimitingConfig = rateLimitingConfig;
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
        Bucket bucket = rateLimitingConfig.resolveBucket();
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return true;
        } else {
            long waitForRefill = probe.getNanosToWaitForRefill() 
                / NANOS_PER_SECOND;
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
            throw new RateLimitExceededException("Rate limit exceeded");
        }
    }
}
