package com.gplanet.commerce_api.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when password confirmation doesn't match the original password.
 * Typically used during password change or user registration operations.
 * Returns HTTP 400 BAD REQUEST status code.
 * 
 * @author Gustavo
 * @version 1.0
 */
public class PasswordMismatchException extends ApiException {
    /**
     * Creates a new password mismatch exception.
     * 
     * @param message The detailed message explaining the mismatch
     */
    public PasswordMismatchException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}