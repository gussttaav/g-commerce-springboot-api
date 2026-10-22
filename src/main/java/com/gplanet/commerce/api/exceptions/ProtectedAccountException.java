package com.gplanet.commerce.api.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a request would change the email, password or role of one of the shared demo accounts,
 * which are protected so no visitor can lock the others out of them.
 * Returns HTTP 403 FORBIDDEN status code.
 *
 * @author Gustavo
 * @version 1.0
 */
public class ProtectedAccountException extends ApiException {
    /**
     * Creates a new protected account exception.
     *
     * @param message The detailed message explaining which change is not allowed
     */
    public ProtectedAccountException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
