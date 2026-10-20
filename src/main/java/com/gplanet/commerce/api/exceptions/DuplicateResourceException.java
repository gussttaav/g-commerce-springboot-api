package com.gplanet.commerce.api.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a resource can't be created or updated because another resource
 * already has a value that must be unique, such as a user's email or a product's name.
 * Returns HTTP 409 CONFLICT status code.
 *
 * @author Gustavo
 * @version 1.0
 */
public class DuplicateResourceException extends ApiException {
    /**
     * Creates a new duplicate resource exception.
     *
     * @param message The detailed message explaining which value is already taken
     */
    public DuplicateResourceException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
