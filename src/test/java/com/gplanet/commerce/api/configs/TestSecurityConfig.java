package com.gplanet.commerce.api.configs;

import static com.gplanet.commerce.api.testutils.TestConstants.ROLE_ADMIN;
import static com.gplanet.commerce.api.testutils.TestConstants.ROLE_USER;
import static com.gplanet.commerce.api.testutils.TestConstants.TEST_ADMIN_EMAIL;
import static com.gplanet.commerce.api.testutils.TestConstants.TEST_ADMIN_PASSWD;
import static com.gplanet.commerce.api.testutils.TestConstants.TEST_USER_EMAIL;
import static com.gplanet.commerce.api.testutils.TestConstants.TEST_USER_PASSWD;

import java.util.Arrays;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import com.gplanet.commerce.api.configs.app.props.CorsProperties;

/**
 * Test security configuration that provides in-memory users for testing
 * and mocks the CORS properties.
 */
@TestConfiguration
@EnableWebSecurity
public class TestSecurityConfig {
    
    /**
     * Provides test CORS properties configuration.
     */
    @Bean
    @Primary
    public CorsProperties corsProperties() {
        CorsProperties properties = new CorsProperties();
        properties.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
        properties.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        properties.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        properties.setTestOrigin("http://localhost:3000");
        return properties;
    }

    /**
     * Test security filter chain that allows access to test endpoints.
     */
    @Bean
    @Primary
    public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/compras/nueva").hasRole("USER")
                .requestMatchers("/api/compras/**").authenticated()
                .anyRequest().permitAll()
            );
        
        return http.build();
    }

    /**
     * Password encoder for tests.
     */
    @Bean
    @Primary
    public PasswordEncoder testPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
