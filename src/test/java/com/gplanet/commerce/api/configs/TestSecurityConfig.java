package com.gplanet.commerce.api.configs;

import java.util.Arrays;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

import com.gplanet.commerce.api.configs.app.props.CorsProperties;

/**
 * Test security configuration that provides in-memory users for testing
 * and mocks the CORS properties.
 * 
 * @author Gustavo
 * @version 1.0
 */
@TestConfiguration
@EnableWebSecurity
public class TestSecurityConfig {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_USER = "USER";
    
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
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            )
            .authorizeHttpRequests(authz -> authz
                // Public endpoints - no authentication required
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/favicon.ico").permitAll()
                .requestMatchers("/.well-known/jwks.json", "/.well-known/openid-configuration").permitAll()
                .requestMatchers("/api/auth/login", "/api/auth/refresh", "/api/auth/validate").permitAll()
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers("/actuator/info", "/actuator/prometheus").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/productos/listar").permitAll()

                // Admin-only endpoints
                .requestMatchers("/api/usuarios/admin/**").hasRole(ROLE_ADMIN)
                .requestMatchers("/api/productos/crear", 
                                "/api/productos/actualizar/**", 
                                "/api/productos/eliminar/**").hasRole(ROLE_ADMIN)
                .requestMatchers("/api/admin/**").hasRole(ROLE_ADMIN)

                // User-specific endpoints
                .requestMatchers("/api/usuarios/perfil", "/api/usuarios/password").authenticated()
                .requestMatchers("/api/compras/nueva").hasRole(ROLE_USER)
                .requestMatchers("/api/compras/**").authenticated()

                // Mixed access endpoints
                .requestMatchers("/api/usuarios/**").permitAll()
                
                // All other requests require authentication                
                .anyRequest().authenticated()
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
