package com.gplanet.commerce.api.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a token cannot be blacklisted.
 * Returns HTTP 500 INTERNAL_SERVER_ERROR status code.
 * 
 * @author Gustavo
 * @version 1.0
 */
public class TokenBlacklistException extends ApiException {

    /**
     * Creates a new TokenBlacklistException with a specific message.
     *
     * @param message the detailed message explaining why the token could not be blacklisted
     */
    public TokenBlacklistException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
