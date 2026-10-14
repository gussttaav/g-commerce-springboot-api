package com.gplanet.commerce.api.configs.security.ratelimiting;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.gplanet.commerce.api.configs.app.props.RateLimitProperties;

/**
 * Configuration class for rate limiting functionality in the application.
 * <p>This class registers the interceptors that rate-limit API requests with the token bucket algorithm,
 * with the limits configured by the properties prefixed with 'rate-limit' ({@link RateLimitProperties}):</p>
 * <ul>
 *   <li>Unauthenticated requests: Most restricted access, per client IP address</li>
 *   <li>Regular authenticated users: Standard access limits, per user</li>
 *   <li>Admin users: Higher access limits, per user</li>
 *   <li>Login attempts: A stricter limit of their own, per client IP address</li>
 * </ul>
 *
 * @author Gustavo
 * @version 1.2
 * @see RateLimitBucketResolver
 * @see RateLimitInterceptor
 */
@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitingConfig implements WebMvcConfigurer {

    private static final String LOGIN_PATH = "/api/auth/login";

    private final RateLimitBucketResolver bucketResolver;

    /**
     * Creates the rate limiting configuration with the given limits.
     *
     * @param properties the rate limits
     */
    public RateLimitingConfig(RateLimitProperties properties) {
        this.bucketResolver = new RateLimitBucketResolver(properties);
    }

    /**
     * Registers the rate limiting interceptors for the API: login attempts consume from the login bucket of the
     * client, and every other request under "/api/" from the bucket of the user or anonymous client.
     * This method is called automatically by Spring MVC.
     *
     * @param registry the InterceptorRegistry used to register the interceptors
     * @see RateLimitInterceptor
     */
    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(new RateLimitInterceptor(bucketResolver::resolveLoginBucket))
                .addPathPatterns(LOGIN_PATH);
        registry.addInterceptor(new RateLimitInterceptor(bucketResolver::resolveBucket))
                .addPathPatterns("/api/**")
                .excludePathPatterns(LOGIN_PATH);
    }
}
