package com.mitienda.gestion_tienda.filters;

import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filter that logs HTTP request details including timing, user information, and request status.
 * This filter runs once per request and adds MDC context for structured logging.
 * 
 * The filter captures:
 * <ul>
 *   <li>Request method and URI</li>
 *   <li>Client IP address</li>
 *   <li>Request processing time</li>
 *   <li>Response status</li>
 *   <li>User id (if authenticated)</li>
 * </ul>
 * 
 * @author Gustavo
 * @version 1.0
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    /**
     * Processes each HTTP request, adding logging context and timing information.
     * 
     * @param request the HTTP request to process
     * @param response the HTTP response being processed
     * @param filterChain the filter chain to execute
     * @throws ServletException if a servlet exception occurs
     * @throws IOException if an I/O exception occurs
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("requestId", requestId);
        MDC.put("userId", getUserId(request));
        
        try {
            logger.info("Request initiated: {} {} [{}]", 
                        request.getMethod(), request.getRequestURI(), request.getRemoteAddr());
            long startTime = System.currentTimeMillis();
            
            filterChain.doFilter(request, response);
            
            logger.info("Request completed: {} {} - {} in {}ms", 
                        request.getMethod(), request.getRequestURI(), 
                        response.getStatus(), System.currentTimeMillis() - startTime);
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * Extracts the user identifier from the security context or basic auth header.
     * Returns "anonymous" for unauthenticated requests or when security context is not available.
     * 
     * @param request the HTTP request being processed
     * @return the user's identifier (email) or "anonymous" if user is not authenticated
     */
    private String getUserId(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            // Get user from security context if available
            return auth.getName();
        }
        
        // Try to get from HTTP basic auth if security context is not populated yet
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Basic ")) {
            try {
                String base64Credentials = authHeader.substring("Basic ".length());
                String credentials = new String(Base64.getDecoder().decode(base64Credentials));
                String username = credentials.split(":", 2)[0];
                return username;
            } catch (Exception ex) {
                // In case of any parsing issues
                return "anonymous";
            }
        }
        
        return "anonymous";
    }
}