package com.gplanet.commerce.api.exceptions;

import org.springframework.http.HttpStatus;


/**
 * Exception thrown when a token validation fails.
 * This could be due to expired token, invalid signature, blacklisted token,
 * or malformed token structure.
 * Returns HTTP 401 UNAUTHORIZED status code.
 * 
 * @author Gustavo
 * @version 1.0
 */
public class InvalidTokenException extends ApiException {

    /**
     * Creates a new invalid token exception.
     * 
     * @param message The detailed message explaining why the token is invalid
     */
    public InvalidTokenException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
