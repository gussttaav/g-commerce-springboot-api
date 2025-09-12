package com.gplanet.commerce.api.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when storing a refresh token in Redis fails.
 * Returns HTTP 500 INTERNAL_SERVER_ERROR status code.
 * 
 * @author Gustavo
 * @version 1.0
 */
public class TokenStorageException extends ApiException {

    /**
     * Creates a new TokenStorageException with a specific message.
     *
     * @param message the detailed message explaining why storing the token failed
     */
    public TokenStorageException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
