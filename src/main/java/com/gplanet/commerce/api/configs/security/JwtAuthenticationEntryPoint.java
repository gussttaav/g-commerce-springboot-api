package com.gplanet.commerce.api.configs.security;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gplanet.commerce.api.dtos.api.ApiErrorDTO;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Authentication entry point for handling unauthorized access attempts.
 * <p> 
 * This component is triggered when an unauthenticated user (missing or invalid JWT token)
 * tries to access a secured resource. Instead of returning an HTML error page,
 * it produces a standardized JSON error response using {@link ApiErrorDTO}.
 * </p>
 * Integrated with Spring Security via {@link org.springframework.security.web.AuthenticationEntryPoint}.
 *
 * @author Gustavo
 * @version 1.0
 */
@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper mapper;

    /**
     * Default constructor that initializes the ObjectMapper and registers modules for proper serialization.
     */
    public JwtAuthenticationEntryPoint() {
        this.mapper = new ObjectMapper();
        this.mapper.findAndRegisterModules(); // For LocalDateTime serialization
    }

    /**
     * Handles authentication failures by sending a JSON error response.
     *
     * @param request the HTTP request that resulted in an authentication error
     * @param response the HTTP response to be sent back to the client
     * @param authException the exception that caused the authentication failure
     * @throws IOException if writing the JSON response fails
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        
        log.warn("Unauthorized access attempt to {}: {}", request.getRequestURI(), authException.getMessage());

        // Clear any existing response content
        response.reset();

        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        
        // Ensure the response is not cached
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        ApiErrorDTO error = new ApiErrorDTO(
                LocalDateTime.now(),
                HttpServletResponse.SC_UNAUTHORIZED,
                "Unauthorized",
                "Token de acceso requerido para acceder a este recurso",
                request.getRequestURI(),
                new ArrayList<>()
        );
        
        response.getWriter().write(mapper.writeValueAsString(error));
        response.getWriter().flush();
        
        log.debug("Sent 401 Unauthorized response for request to {}", request.getRequestURI());
    }
}
