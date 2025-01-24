package com.mitienda.gestion_tienda.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested resource cannot be found in the system.
 * This could be a user, product, purchase, or any other entity.
 * Returns HTTP 404 NOT FOUND status code.
 * 
 * @author Gustavo
 * @version 1.0
 */
public class ResourceNotFoundException extends ApiException {
    /**
     * Creates a new resource not found exception.
     * 
     * @param message The detailed message explaining which resource was not found
     */
    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}