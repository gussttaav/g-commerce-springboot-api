package com.gplanet.commerce_api.configs.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.gplanet.commerce_api.configs.app.props.CorsProperties;
import com.gplanet.commerce_api.services.UsuarioDetallesService;

import lombok.RequiredArgsConstructor;

/**
 * Security configuration class that sets up Spring Security for the application.
 * This class defines security rules, authentication, and authorization settings.
 *
 * @author Gustavo
 * @version 1.0
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CorsProperties corsProperties;
    private final UsuarioDetallesService customUserDetailsService;

    /**
     * Configures the security filter chain with specific security rules and permissions.
     *
     * @param http the HttpSecurity to configure
     * @return the configured SecurityFilterChain
     * @throws Exception if there's an error during configuration
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/favicon.ico").permitAll()
                .requestMatchers("/api/usuarios/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/usuarios/perfil", "/api/usuarios/password").authenticated()
                .requestMatchers("/api/usuarios/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/productos/listar").permitAll()
                .requestMatchers("/api/productos/**").hasRole("ADMIN")
                .requestMatchers("/api/compras/nueva").hasRole("USER")
                .requestMatchers("/api/compras/**").authenticated()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .httpBasic(httpBasic -> httpBasic.realmName("MyAppRealm"));
        
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
     * 
     * The configuration is obtained from the CorsProperties bean, that read the 
     * settings from the application.properites file
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
