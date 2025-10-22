package com.gplanet.commerce.api.configs.security;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gplanet.commerce.api.dtos.api.ApiErrorDTO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Access denied handler for handling forbidden access attempts.
 * <p>
 * This component is triggered when an authenticated user tries to access a
 * resource for which they lack the required permissions or roles.
 * It produces a standardized JSON error response using {@link ApiErrorDTO}.
 * </p>
 * Integrated with Spring Security via {@link org.springframework.security.web.access.AccessDeniedHandler}.
 *
 * @author Gustavo
 * @version 1.0
 */
@Slf4j
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper mapper;

    /**
     * Default constructor that initializes the ObjectMapper and registers modules for proper serialization.
     */
    public JwtAccessDeniedHandler() {
        this.mapper = new ObjectMapper();
        this.mapper.findAndRegisterModules(); // For LocalDateTime serialization
    }

    /**
     * Handles authorization failures by sending a JSON error response.
     *
     * @param request the HTTP request that resulted in an authorization error
     * @param response the HTTP response to be sent back to the client
     * @param accessDeniedException the exception that caused the access denial
     * @throws IOException if writing the JSON response fails
     */
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        log.warn("Access denied for request to {}: {}", request.getRequestURI(), accessDeniedException.getMessage());

        // Clear any existing response content
        response.reset();

        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding("UTF-8");
        
        // Ensure the response is not cached
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        ApiErrorDTO error = new ApiErrorDTO(
                LocalDateTime.now(),
                HttpServletResponse.SC_FORBIDDEN,
                "Forbidden",
                "Unauthorized operation",
                request.getRequestURI(),
                new ArrayList<>()
        );
        
        response.getWriter().write(mapper.writeValueAsString(error));
        response.getWriter().flush();
        
        log.debug("Sent 403 Forbidden response for request to {}", request.getRequestURI());
    }
}
