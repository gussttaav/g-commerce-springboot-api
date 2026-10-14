package com.gplanet.commerce.api.configs.app.props;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration properties for rate limiting, bound from the properties prefixed with <code>rate-limit</code>.
 * <p>
 * Each limit is a token bucket: a client can send up to <b>capacity</b> requests in a burst, and the bucket refills
 * at <b>refill</b> requests per window of <b>window-minutes</b>.
 * </p>
 *
 * <ul>
 *   <li><b>unauthenticated</b>: anonymous requests, per client IP address, except login attempts.</li>
 *   <li><b>user</b>: requests of a user with the USER role.</li>
 *   <li><b>admin</b>: requests of a user with the ADMIN role.</li>
 *   <li><b>login</b>: login attempts, per client IP address.</li>
 * </ul>
 *
 * <p>
 * Registered by {@link com.gplanet.commerce.api.configs.security.ratelimiting.RateLimitingConfig}, so it's
 * available wherever the rate-limit interceptors are, including web MVC test slices.
 * </p>
 *
 * @author Gustavo
 * @version 1.0
 */
@Data
@Validated
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {
    /**
     * Time window, in minutes, in which the buckets refill.
     */
    @Positive
    private int windowMinutes = 1;

    /**
     * Limit of anonymous requests per client IP address, except login attempts.
     */
    @Valid
    @NotNull
    private Limit unauthenticated = new Limit(10, 5);

    /**
     * Limit of requests per user with the USER role.
     */
    @Valid
    @NotNull
    private Limit user = new Limit(30, 20);

    /**
     * Limit of requests per user with the ADMIN role.
     */
    @Valid
    @NotNull
    private Limit admin = new Limit(100, 50);

    /**
     * Limit of login attempts per client IP address.
     */
    @Valid
    @NotNull
    private Limit login = new Limit(5, 1);

    /**
     * A token bucket limit.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Limit {
        /**
         * Maximum number of requests allowed in a burst.
         */
        @Positive
        private int capacity;

        /**
         * Number of requests the bucket refills per window.
         */
        @Positive
        private int refill;
    }
}
