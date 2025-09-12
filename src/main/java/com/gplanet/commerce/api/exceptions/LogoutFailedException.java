package com.gplanet.commerce.api.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when the logout process fails.
 * This can occur if tokens cannot be blacklisted or removed.
 * Returns HTTP 500 INTERNAL_SERVER_ERROR status code.
 * 
 * @author Gustavo
 * @version 1.0
 */
public class LogoutFailedException extends ApiException {

    /**
     * Creates a new LogoutFailedException with a specific message.
     *
     * @param message the detailed message explaining why the logout failed
     */
    public LogoutFailedException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

