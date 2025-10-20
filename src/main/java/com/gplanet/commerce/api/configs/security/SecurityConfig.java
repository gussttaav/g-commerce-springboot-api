package com.gplanet.commerce.api.configs.security;

import java.security.interfaces.RSAPublicKey;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.gplanet.commerce.api.configs.app.props.CorsProperties;
import com.gplanet.commerce.api.services.KeyStoreService;
import com.gplanet.commerce.api.services.UsuarioDetallesService;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import lombok.RequiredArgsConstructor;

/**
 * Security configuration class that sets up Spring Security for the application.
 * <p>
 * This class configures authentication, authorization, JWT handling, CORS, and exception handling
 * for the API. It integrates with a custom user details service, key store service for JWT signing,
 * and custom handlers for authentication and access denied events.
 * </p>
 * <ul>
 *   <li>Configures stateless session management</li>
 *   <li>Defines public, user, and admin endpoints</li>
 *   <li>Sets up JWT encoder/decoder and authority mapping</li>
 *   <li>Configures CORS for API endpoints</li>
 * </ul>
 * 
 * @author Gustavo
 * @version 1.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_USER = "USER";

    private final CorsProperties corsProperties;
    private final KeyStoreService keyStoreService;
    private final UsuarioDetallesService customUserDetailsService;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    /**
     * Configures the security filter chain for the application.
     * <p>
     * Sets up CORS, disables CSRF, configures stateless session management,
     * exception handling, JWT resource server, and authorization rules for endpoints.
     * </p>
     *
     * @param http the {@link HttpSecurity} to modify
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if an error occurs during configuration
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            // CORS configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Disable CSRF for stateless API
            .csrf(csrf -> csrf.disable())

            // Configure session management for stateless authentication
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Configure exception handling - this is crucial for proper error responses
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(jwtAccessDeniedHandler)
            )

            // Resource Server with JWT
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(jwtAccessDeniedHandler)
            )

            // Configure authorization rules
            .authorizeHttpRequests(authz -> authz
                // Public endpoints - no authentication required
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/favicon.ico").permitAll()
                .requestMatchers("/.well-known/jwks.json", "/.well-known/openid-configuration").permitAll()
                .requestMatchers("/api/auth/login", "/api/auth/refresh", "/api/auth/validate").permitAll()
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers("/actuator/info", "/actuator/prometheus").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/usuarios").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products").permitAll()

                // Admin-only endpoints
                .requestMatchers(HttpMethod.GET, "/api/usuarios").hasRole(ROLE_ADMIN)
                .requestMatchers(HttpMethod.PATCH, "/api/usuarios/{id}/role").hasRole(ROLE_ADMIN)
                .requestMatchers("/api/products/**").hasRole(ROLE_ADMIN)

                // User-specific endpoints
                .requestMatchers(HttpMethod.POST, "/api/purchases").hasRole(ROLE_USER)
                
                // All other requests require authentication                
                .anyRequest().authenticated()
            );
        
        return http.build();
    }

    /**
     * Provides a password encoder bean using BCrypt.
     * <p>
     * Used for hashing and verifying user passwords.
     * </p>
     *
     * @return a {@link PasswordEncoder} instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configures the {@link AuthenticationManager} with the custom user details service and password encoder.
     *
     * @param http the {@link HttpSecurity} to use for shared objects
     * @param passwordEncoder the password encoder bean
     * @return the configured {@link AuthenticationManager}
     * @throws Exception if an error occurs during configuration
     */
    @Bean
    AuthenticationManager authManager(HttpSecurity http, PasswordEncoder passwordEncoder) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder = 
                http.getSharedObject(AuthenticationManagerBuilder.class);

        authenticationManagerBuilder
                .userDetailsService(customUserDetailsService)
                .passwordEncoder(passwordEncoder);
        
        return authenticationManagerBuilder.build();
    }

    /**
     * Provides a JWK source for JWT signing and verification.
     * <p>
     * Uses the active RSA key pair from the key store service.
     * </p>
     *
     * @return a {@link JWKSource} for JWT operations
     */
    @Bean
    JWKSource<SecurityContext> jwkSource() {
        return (jwkSelector, securityContext) -> {
            RSAKey rsaKey = keyStoreService.getOrCreateActiveKeyPair();
            JWKSet set = new JWKSet(List.of((JWK) rsaKey));
            return jwkSelector.select(set);
        };
    }

    /**
     * Provides a JWT encoder bean using the configured JWK source.
     *
     * @param jwkSource the JWK source bean
     * @return a {@link JwtEncoder} instance
     */
    @Bean
    JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    /**
     * Provides a JWT decoder bean using the current RSA public key.
     *
     * @return a {@link JwtDecoder} instance
     * @throws IllegalStateException if the RSA public key cannot be extracted
     */
    @Bean
    JwtDecoder jwtDecoder() {
        // Build decoder from current RSA public key
        RSAKey rsaKey = keyStoreService.getOrCreateActiveKeyPair();
        RSAPublicKey publicKey;
        try {
            publicKey = rsaKey.toRSAPublicKey();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to extract RSA public key from JWK", ex);
        }
        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }

    /**
     * Configures CORS for API endpoints using properties from {@link CorsProperties}.
     *
     * @return a {@link CorsConfigurationSource} for CORS handling
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(corsProperties.getAllowedMethods());
        configuration.setAllowedHeaders(corsProperties.getAllowedHeaders());
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    /**
     * Provides a JWT authentication converter that maps claims to authorities.
     * <p>
     * Maps the 'authorities' claim and merges with scopes, removing any authority prefix.
     * </p>
     *
     * @return a {@link JwtAuthenticationConverter} for JWT authentication
     */
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();
        // Default reads 'scope'/'scp' and prefixes with 'SCOPE_'; remove prefix for uniformity
        scopeConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter((Jwt jwt) -> {
            // Authorities from 'authorities' claim (list of strings)
            java.util.Collection<GrantedAuthority> authorities = new java.util.ArrayList<>();
            Object authClaim = jwt.getClaim("authorities");
            if (authClaim instanceof java.util.List<?> list) {
                for (Object o : list) {
                    if (o instanceof String s) {
                        authorities.add(new SimpleGrantedAuthority(s));
                    }
                }
            }
            // Merge with scopes
            authorities.addAll(scopeConverter.convert(jwt));
            return authorities;
        });
        return converter;
    }
}
