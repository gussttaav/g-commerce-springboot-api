package com.gplanet.commerce.api.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a user attempts to perform an operation they're not authorized to do.
 * This is different from authentication failures - this is for authenticated users
 * who don't have sufficient permissions.
 * Returns HTTP 403 FORBIDDEN status code.
 * 
 * @author Gustavo
 * @version 1.0
 */
public class UnauthorizedOperationException extends ApiException {
    /**
     * Creates a new unauthorized operation exception.
     * 
     * @param message The detailed message explaining why the operation was not authorized
     */
    public UnauthorizedOperationException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
