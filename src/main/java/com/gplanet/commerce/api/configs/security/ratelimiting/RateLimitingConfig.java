package com.gplanet.commerce.api.configs.security.ratelimiting;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;

/**
 * Configuration class for rate limiting functionality in the application.
 * <p>This class manages rate limit buckets for different types of users and provides
 * configurable rate limiting parameters through application properties.</p>
 *
 * <p>The class implements three levels of rate limiting:</p>
 * <ul>
 *   <li>Unauthenticated users: Most restricted access</li>
 *   <li>Regular authenticated users: Standard access limits</li>
 *   <li>Admin users: Higher access limits</li>
 * </ul>
 *
 * <p>Rate limits are configured using the token bucket algorithm, where:</p>
 * <ul>
 *   <li>Capacity defines the maximum number of requests allowed in a burst</li>
 *   <li>Refill rate defines how many tokens are restored per time window</li>
 *   <li>Time window is configurable in minutes</li>
 * </ul>
 *
 * <p>Configuration can be adjusted through application properties with the prefix 'rate-limit'.</p>
 *
 * @author Gustavo
 * @version 1.1
 * @see WebMvcConfigurer
 * @see RateLimitInterceptor
 */
@Component
public class RateLimitingConfig implements WebMvcConfigurer {
    /** 
     * Cache of rate limit buckets per user.
     */
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /** 
     * Maximum requests capacity for unauthenticated users.
     */
    @Value("${rate-limit.unauthenticated.capacity:5}")
    private int unauthenticatedCapacity;
    
    /** 
     * Refill rate for unauthenticated users.
     */
    @Value("${rate-limit.unauthenticated.refill:5}")
    private int unauthenticatedRefill;
    
    /** 
     * Maximum requests capacity for regular users.
     */
    @Value("${rate-limit.user.capacity:30}")
    private int userCapacity;
    
    /** 
     * Refill rate for regular users.
     */
    @Value("${rate-limit.user.refill:20}")
    private int userRefill;
    
    /** 
     * Maximum requests capacity for admin users.
     */
    @Value("${rate-limit.admin.capacity:100}")
    private int adminCapacity;
    
    /** 
     * Refill rate for admin users.
     */
    @Value("${rate-limit.admin.refill:50}")
    private int adminRefill;
    
    /** 
     * Time window in minutes for rate limit renewal.
     */
    @Value("${rate-limit.window-minutes:1}")
    private int windowMinutes;

    /**
     * Resolves the appropriate rate limit bucket for the current user.
     * Creates a new bucket if one doesn't exist.
     * 
     * @return the rate limit bucket for the current user
     */
    public Bucket resolveBucket() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // If not authenticated, use default limits
        if (auth == null || !auth.isAuthenticated()) {
            return createDefaultBucket();
        }
        
        // Use username as the key
        String username = auth.getName();
        
        return buckets.computeIfAbsent(username, this::createUserBucket);
    }

    /**
     * Creates a rate limit bucket for unauthenticated users.
     * 
     * @return a new bucket with unauthenticated user limits
     */
    private Bucket createDefaultBucket() {
        // Unauthenticated users get a more restricted rate limit
        Bandwidth limit = Bandwidth.classic(
            unauthenticatedCapacity, 
            Refill.greedy(unauthenticatedRefill, Duration.ofMinutes(windowMinutes))
        );
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Creates a rate limit bucket for an authenticated user.
     * 
     * @param username the username of the authenticated user
     * @return a new bucket with appropriate limits based on user role
     */
    private Bucket createUserBucket(String username) {
        // Different rate limits based on user role
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            Bandwidth limit = Bandwidth.classic(
                adminCapacity, 
                Refill.greedy(adminRefill, Duration.ofMinutes(windowMinutes))
            );
            return Bucket.builder()
                    .addLimit(limit)
                    .build();
        } else {
            Bandwidth limit = Bandwidth.classic(
                userCapacity, 
                Refill.greedy(userRefill, Duration.ofMinutes(windowMinutes))
            );
            return Bucket.builder()
                    .addLimit(limit)
                    .build();
        }
    }

    /**
     * Configures the rate limiting interceptor for the application.
     * Adds the RateLimitInterceptor to intercept all requests to paths starting with "/api/".
     * This method is called automatically by Spring MVC to register the interceptor.
     * 
     * @param registry the InterceptorRegistry used to register the interceptor
     * @see RateLimitInterceptor
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RateLimitInterceptor(this))
                .addPathPatterns("/api/**");
    }
}
