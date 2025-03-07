package com.mitienda.gestion_tienda.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a client has exceeded their rate limit for API requests.
 * This exception maps to an HTTP 429 Too Many Requests response.
 * 
 * @author Gustavo
 * @version 1.0
 */
public class RateLimitExceededException extends ApiException {

    /**
     * Constructs a new rate limit exceeded exception with the specified message.
     * 
     * @param message the detail message explaining why the rate limit was exceeded
     */
    public RateLimitExceededException(String message) {
        super(message, HttpStatus.TOO_MANY_REQUESTS);
    }
}
