package com.mitienda.gestion_tienda.exceptions;

import org.springframework.http.HttpStatus;
import lombok.Getter;

/**
 * Base exception class for all API-related exceptions in the application.
 * Provides a standardized way to handle errors with HTTP status codes.
 * 
 * @author Gustavo
 * @version 1.0
 */
@Getter
public class ApiException extends RuntimeException {
    /**
     * The HTTP status code associated with this exception.
     */
    private final HttpStatus status;
    
    /**
     * Creates a new API exception with a specific message and HTTP status.
     * 
     * @param message The detailed error message describing what went wrong
     * @param status The HTTP status code that should be returned to the client
     */
    public ApiException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}