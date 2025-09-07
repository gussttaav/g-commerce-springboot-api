package com.gplanet.commerce.api.filters;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.gplanet.commerce.api.utilities.JwtTokenUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT Authentication Filter that handles token validation and request logging.
 * This filter processes JWT tokens from the Authorization header and adds MDC context for logging.
 * 
 * <p>The filter handles:
 * <ul>
 *   <li>JWT token extraction and validation</li>
 *   <li>Authentication context setup</li>
 *   <li>Request timing and logging</li>
 *   <li>MDC context for structured logging</li>
 * </ul>
 * </p>
 * 
 * @author Gustavo
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private static final String USER_ID_MDC_KEY = "userId";
    private static final String REQUEST_ID_MDC_KEY = "requestId";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ANONYMOUS_USER = "anonymous";
    
    private final JwtTokenUtil jwtTokenUtil;
    private final UserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, 
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        setupMdcContext();
        long startTime = System.currentTimeMillis();
        
        try {
            logRequestStart(request);
            processAuthentication(request);
            filterChain.doFilter(request, response);
        } finally {
            logRequestCompletion(request, response, startTime);
            MDC.clear();
        }
    }
    
    /**
     * Sets up MDC (Mapped Diagnostic Context) with a unique request ID and a default user ID.
     * This helps in tracking logs per request and associating them with a user or anonymous context.
     */
    private void setupMdcContext() {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        MDC.put(USER_ID_MDC_KEY, ANONYMOUS_USER);
    }
    
    /**
     * Logs the start of the HTTP request.
     *
     * @param request the {@link HttpServletRequest} representing the incoming request.
     */
    private void logRequestStart(HttpServletRequest request) {
        log.info("Request initiated: {} {} [{}]", 
                request.getMethod(), request.getRequestURI(), request.getRemoteAddr());
    }
    
    /**
     * Processes authentication for the incoming request by extracting and validating the JWT or Basic Auth header.
     * This method will NOT throw exceptions for missing auth headers (that's handled by Spring Security's entry point)
     * but WILL throw exceptions for malformed or invalid tokens.
     *
     * @param request the {@link HttpServletRequest} representing the incoming request.
     */
    private void processAuthentication(HttpServletRequest request) {
        final String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.debug("No Bearer token found in Authorization header");
            return; // Let Spring Security handle this via AuthenticationEntryPoint
        }
        
        String username = extractUsernameFromJwt(authHeader);
        String jwtToken = authHeader.substring(BEARER_PREFIX.length());
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            MDC.put(USER_ID_MDC_KEY, username);
            authenticateWithJwt(request, username, jwtToken);
        }
        // If username is null due to malformed token, we let the request continue
        // and Spring Security will handle it as unauthenticated
    }
    
    /**
     * Extracts the username from a JWT Bearer token.
     *
     * @param authHeader the Authorization header containing the Bearer token.
     * @return the username from the JWT, or {@code null} if extraction fails.
     */
    private String extractUsernameFromJwt(String authHeader) {
        try {
            String jwtToken = authHeader.substring(BEARER_PREFIX.length()).trim();
            if (jwtToken.isEmpty()) {
                log.debug("Empty JWT token provided");
                return null;
            }
            return jwtTokenUtil.extractUsername(jwtToken);
        } catch (Exception e) {
            log.debug("Invalid JWT token format: {}", e.getMessage());
            return null; // Invalid token format - let it be handled as unauthenticated
        }
    }
    
    /**
     * Authenticates the user using JWT token validation and sets up the security context.
     *
     * @param request   the {@link HttpServletRequest} representing the incoming request.
     * @param username  the username extracted from the JWT token.
     * @param jwtToken  the JWT token string.
     */
    private void authenticateWithJwt(HttpServletRequest request, String username, String jwtToken) {
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            
            if (jwtTokenUtil.validateToken(jwtToken, userDetails)) {
                setupAuthenticationContext(request, userDetails, jwtToken);
                log.debug("Successfully authenticated user: {}", username);
            } else {
                log.debug("JWT token validation failed for user: {}", username);
            }
        } catch (UsernameNotFoundException e) {
            log.debug("User not found: {}", username);
        } catch (Exception e) {
            log.debug("JWT authentication failed for user {}: {}", username, e.getMessage());
        }
    }
    
    /**
     * Sets up the Spring Security authentication context with the authenticated user's details and roles.
     *
     * @param request     the {@link HttpServletRequest} representing the incoming request.
     * @param userDetails the {@link UserDetails} of the authenticated user.
     * @param jwtToken    the JWT token string.
     */
    private void setupAuthenticationContext(HttpServletRequest request, UserDetails userDetails, String jwtToken) {
        List<String> roles = jwtTokenUtil.extractRoles(jwtToken);
        List<GrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        
        UsernamePasswordAuthenticationToken authenticationToken = 
            new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        
        log.debug("Authenticated user: {} with roles: {}", userDetails.getUsername(), roles);
    }
    
    /**
     * Logs the completion of the HTTP request, including status and processing time.
     *
     * @param request     the {@link HttpServletRequest} representing the incoming request.
     * @param response    the {@link HttpServletResponse} representing the outgoing response.
     * @param startTime   the timestamp when request processing started (in milliseconds).
     */
    private void logRequestCompletion(HttpServletRequest request, HttpServletResponse response, long startTime) {
        long processingTime = System.currentTimeMillis() - startTime;
        log.info("Request completed: {} {} - {} in {}ms", 
                request.getMethod(), request.getRequestURI(), 
                response.getStatus(), processingTime);
    }
}
