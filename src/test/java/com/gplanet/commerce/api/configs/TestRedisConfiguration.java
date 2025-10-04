package com.gplanet.commerce.api.configs;

import java.io.IOException;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import redis.embedded.RedisServer;

/**
 * Test configuration for setting up an embedded Redis server during integration tests.
 * <p>
 * This configuration ensures a Redis instance is started before tests run
 * and stopped once the context is destroyed. It also provides
 * Redis beans (connection factory and template) configured to use the embedded server.
 * </p>
 * 
 * @author Gustavo
 * @version 1.0
 */
@TestConfiguration
public class TestRedisConfiguration {

    private static final int REDIS_PORT = 6379;

    // Static server shared across all test contexts
    private static RedisServer redisServer;
    private static boolean started;

    /**
     * Starts the embedded Redis server on port 6379.
     * Uses synchronized block to prevent multiple contexts from starting the server.
     *
     * @throws IOException if the Redis server cannot be started
     */
    @PostConstruct
    public void startRedis() throws IOException {
        synchronized (TestRedisConfiguration.class) {
            if (!started) {
                redisServer = new RedisServer(REDIS_PORT);
                redisServer.start();
                started = true;
                
                // Ensure server is stopped when JVM exits
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    if (redisServer != null && redisServer.isActive()) {
                        redisServer.stop();
                    }
                }));
            }
        }
    }

    /**
     * Stops the embedded Redis server if it is running.
     * <p>
     * Note: With shared server approach, we don't stop the server here
     * as other test contexts might still need it. The shutdown hook will handle cleanup.
     * </p>
     */
    @PreDestroy
    public void stopRedis() {
        // Server is stopped by shutdown hook to ensure all tests complete
    }

    /**
     * Provides a {@link RedisConnectionFactory} bean connected to the embedded Redis server.
     *
     * @return a {@link LettuceConnectionFactory} configured for localhost:6379
     */
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        LettuceConnectionFactory factory = new LettuceConnectionFactory("localhost", REDIS_PORT);
        factory.afterPropertiesSet();
        return factory;
    }

    /**
     * Provides a {@link StringRedisTemplate} bean for interacting with Redis using String keys and values.
     *
     * @param redisConnectionFactory the connection factory used to connect to Redis
     * @return a {@link StringRedisTemplate} bound to the embedded Redis server
     */
    @Bean
    @Primary
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }
}
