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

    private RedisServer redisServer;

    /**
     * Starts the embedded Redis server on port 6379.
     *
     * @throws IOException if the Redis server cannot be started
     */
    @PostConstruct
    public void startRedis() throws IOException {
        redisServer = new RedisServer(REDIS_PORT);
        redisServer.start();
    }

    /**
     * Stops the embedded Redis server if it is running.
     * <p>
     * This method is called automatically when the Spring context is shut down.
     * </p>
     */
    @PreDestroy
    public void stopRedis() {
        if (redisServer != null) {
            redisServer.stop();
        }
    }

    /**
     * Provides a {@link RedisConnectionFactory} bean connected to the embedded Redis server.
     *
     * @return a {@link LettuceConnectionFactory} configured for localhost:6379
     */
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory("localhost", REDIS_PORT);
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
