package com.gplanet.commerce.api.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a login fails because the email is not registered or the password is wrong.
 * Both cases are reported the same way, so the response doesn't reveal which emails are registered.
 * Returns HTTP 401 UNAUTHORIZED status code.
 *
 * @author Gustavo
 * @version 1.0
 */
public class InvalidCredentialsException extends ApiException {
    /**
     * Creates a new invalid credentials exception.
     *
     * @param message The detailed message explaining why the login failed
     */
    public InvalidCredentialsException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
