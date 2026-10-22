package com.gplanet.commerce.api.configs.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtAudienceValidator;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.gplanet.commerce.api.configs.app.props.CorsProperties;
import com.gplanet.commerce.api.configs.app.props.JwtProperties;
import com.gplanet.commerce.api.services.CustomUserDetailsService;
import com.gplanet.commerce.api.services.KeyStoreService;
import com.gplanet.commerce.api.services.OAuth2TokenService;
import com.gplanet.commerce.api.services.TokenBlacklistService;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

import lombok.RequiredArgsConstructor;

/**
 * Security configuration class that sets up Spring Security for the application.
 * <p>
 * This class configures authentication, authorization, JWT handling, CORS, and exception handling
 * for the API. It integrates with a custom user details service, key store service for JWT verification,
 * and custom handlers for authentication and access denied events.
 * </p>
 * <ul>
 *   <li>Configures stateless session management</li>
 *   <li>Defines public, user, and admin endpoints</li>
 *   <li>Sets up JWT decoding and authority mapping</li>
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

    /**
     * Name of the {@link JwtDecoder} bean that decodes refresh tokens.
     */
    public static final String REFRESH_TOKEN_DECODER = "refreshTokenDecoder";

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_USER = "USER";
    private static final Pattern USER_ID = Pattern.compile("\\d+");

    private final CorsProperties corsProperties;
    private final JwtProperties jwtProperties;
    private final KeyStoreService keyStoreService;
    private final TokenBlacklistService tokenBlacklistService;
    private final CustomUserDetailsService customUserDetailsService;
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
                .bearerTokenResolver(bearerTokenResolver())
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
                .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products").permitAll()

                // Admin-only endpoints
                .requestMatchers(HttpMethod.GET, "/api/users").hasRole(ROLE_ADMIN)
                .requestMatchers(HttpMethod.PATCH, "/api/users/{id}/role").hasRole(ROLE_ADMIN)
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
     * Provides the {@link AuthenticationManager} that checks the email and password of a login.
     * <p>
     * An unknown email and a wrong password fail with the same {@code BadCredentialsException}, and the password
     * of an unknown email is still checked against a dummy hash, so neither the error nor the response time reveal
     * which emails are registered.
     * </p>
     *
     * @param passwordEncoder the password encoder bean
     * @return the configured {@link AuthenticationManager}
     */
    @Bean
    AuthenticationManager authManager(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    /**
     * Provides the source of public keys used to verify JWT signatures.
     * <p>
     * Contains the active key and every retired key that may still have valid tokens. The key set is cached,
     * and reloaded as soon as a token carries an unknown key ID, so a key rotated by any instance is picked up
     * without a restart. Refreshes are not rate limited because the keys come from our own Redis rather than
     * a remote JWKS endpoint, and a rate-limited refresh could reject tokens signed with a just-rotated key.
     * </p>
     *
     * @return a {@link JWKSource} for JWT verification
     */
    @Bean
    JWKSource<SecurityContext> jwkSource() {
        return JWKSourceBuilder.create(new KeyStoreJwkSetSource(keyStoreService))
                .rateLimited(false)
                .build();
    }

    /**
     * Provides the decoder for access tokens, used by the resource server and by {@code /api/auth/validate}.
     * <p>
     * On top of the checks shared by every token, it rejects refresh tokens and revoked tokens.
     * </p>
     *
     * @param jwkSource the JWK source bean
     * @return a {@link JwtDecoder} for access tokens
     */
    @Bean
    @Primary
    JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return createDecoder(jwkSource, List.of(
                // Access tokens carry no token type; refresh tokens do
                new JwtClaimValidator<String>(OAuth2TokenService.TOKEN_TYPE_CLAIM, Objects::isNull),
                new TokenBlacklistValidator(tokenBlacklistService)));
    }

    /**
     * Provides the decoder for refresh tokens, used by {@code /api/auth/refresh} and {@code /api/auth/logout}.
     * <p>
     * On top of the checks shared by every token, it only accepts refresh tokens. Whether a refresh token has
     * already been used or revoked is checked against the refresh tokens stored in Redis.
     * </p>
     *
     * @param jwkSource the JWK source bean
     * @return a {@link JwtDecoder} for refresh tokens
     */
    @Bean(REFRESH_TOKEN_DECODER)
    JwtDecoder refreshTokenDecoder(JWKSource<SecurityContext> jwkSource) {
        return createDecoder(jwkSource, List.of(new JwtClaimValidator<String>(
                OAuth2TokenService.TOKEN_TYPE_CLAIM, OAuth2TokenService.REFRESH_TOKEN_TYPE::equals)));
    }

    /**
     * Creates a JWT decoder that selects the verification key by the token's key ID and validates its
     * timestamps, issuer, audience, subject and the given token-specific claims.
     * <p>
     * The subject must be a user ID. Tokens issued by earlier versions of the API have the user's email as
     * subject, and are rejected: emails can change, so the email of a token may belong to another user by now.
     * </p>
     *
     * @param jwkSource the JWK source bean
     * @param tokenValidators validators for the claims specific to the type of token
     * @return a {@link JwtDecoder} instance
     */
    private JwtDecoder createDecoder(JWKSource<SecurityContext> jwkSource,
                                     List<OAuth2TokenValidator<Jwt>> tokenValidators) {
        DefaultJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
        jwtProcessor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource));
        // Claims are checked by the Spring validators below
        jwtProcessor.setJWTClaimsSetVerifier((claims, context) -> { });

        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>(List.of(
                new JwtIssuerValidator(jwtProperties.getIssuer()),
                new JwtAudienceValidator(jwtProperties.getAudience()),
                new JwtClaimValidator<String>(JwtClaimNames.SUB,
                        subject -> subject != null && USER_ID.matcher(subject).matches())));
        validators.addAll(tokenValidators);

        NimbusJwtDecoder decoder = new NimbusJwtDecoder(jwtProcessor);
        decoder.setJwtValidator(JwtValidators.createDefaultWithValidators(validators));
        return decoder;
    }

    /**
     * Provides the resolver of the bearer token that authenticates a request.
     * <p>
     * {@code /api/auth/refresh} and {@code /api/auth/validate} receive a token to check rather than to
     * authenticate the request: a refresh token, which the access token decoder rejects, and a token that may
     * be invalid, which must be reported instead of failing with 401. The token is not resolved there, so the
     * requests reach the endpoints, which decode the token themselves.
     * </p>
     *
     * @return a {@link BearerTokenResolver} for the resource server
     */
    @Bean
    BearerTokenResolver bearerTokenResolver() {
        DefaultBearerTokenResolver resolver = new DefaultBearerTokenResolver();
        RequestMatcher tokenEndpoints = new OrRequestMatcher(
                PathPatternRequestMatcher.withDefaults().matcher("/api/auth/refresh"),
                PathPatternRequestMatcher.withDefaults().matcher("/api/auth/validate"));
        return request -> tokenEndpoints.matches(request) ? null : resolver.resolve(request);
    }

    /**
     * Configures CORS for the API and discovery endpoints using properties from {@link CorsProperties}.
     * Browser clients such as the frontend and the docs console call both, and read the exposed
     * rate limit headers.
     *
     * @return a {@link CorsConfigurationSource} for CORS handling
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(corsProperties.getAllowedMethods());
        configuration.setAllowedHeaders(corsProperties.getAllowedHeaders());
        configuration.setExposedHeaders(corsProperties.getExposedHeaders());
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/.well-known/**", configuration);
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
