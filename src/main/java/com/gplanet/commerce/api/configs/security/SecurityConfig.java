package com.gplanet.commerce.api.configs.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.gplanet.commerce.api.configs.app.props.CorsProperties;
import com.gplanet.commerce.api.filters.JwtAuthenticationFilter;
import com.gplanet.commerce.api.services.UsuarioDetallesService;

import lombok.RequiredArgsConstructor;

/**
 * Security configuration class that sets up Spring Security for the application.
 * This class defines security rules, authentication, and authorization settings.
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

    private final CorsProperties corsProperties;
    private final UsuarioDetallesService customUserDetailsService;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    /**
     * Configures the security filter chain with specific security rules and permissions.
     *
     * @param http the HttpSecurity to configure
     * @return the configured SecurityFilterChain
     * @throws Exception if there's an error during configuration
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,  
            JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {

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

            // Configure authorization rules
            .authorizeHttpRequests(authz -> authz
                // Public endpoints - no authentication required
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/favicon.ico").permitAll()
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers("/actuator/info", "/actuator/prometheus").permitAll()

                // Admin-only endpoints
                .requestMatchers("/api/usuarios/admin/**").hasRole(ROLE_ADMIN)
                .requestMatchers("/api/productos/**").hasRole(ROLE_ADMIN)
                .requestMatchers("/api/admin/**").hasRole(ROLE_ADMIN)

                // User-specific endpoints
                .requestMatchers("/api/usuarios/perfil", "/api/usuarios/password").authenticated()
                .requestMatchers("/api/compras/nueva").hasRole("USER")
                .requestMatchers("/api/compras/**").authenticated()

                // Mixed access endpoints
                .requestMatchers("/api/usuarios/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/productos/listar").permitAll()
                
                // All other requests require authentication                
                .anyRequest().authenticated()
            )

            // Add JWT filter before the standard username/password authentication filter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }

    /**
     * Creates a password encoder bean for secure password hashing.
     *
     * @return BCryptPasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configures the authentication manager with custom user details service and password encoder.
     *
     * @param http the HttpSecurity to configure
     * @param passwordEncoder the password encoder to use
     * @return configured AuthenticationManager
     * @throws Exception if there's an error during configuration
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
     * Configures CORS (Cross-Origin Resource Sharing) settings for the application.
     * <p>The configuration is obtained from the CorsProperties bean, that read the 
     * settings from the application.properites file.</p>
     *
     * @return configured CorsConfigurationSource
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
}
