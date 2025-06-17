package com.gplanet.commerce_api.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a password validation fails.
 * This could be due to incorrect current password, weak password format, etc.
 * Returns HTTP 401 UNAUTHORIZED status code.
 * 
 * @author Gustavo
 * @version 1.0
 */
public class InvalidPasswordException extends ApiException {
    /**
     * Creates a new invalid password exception.
     * 
     * @param message The detailed message explaining why the password is invalid
     */
    public InvalidPasswordException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}