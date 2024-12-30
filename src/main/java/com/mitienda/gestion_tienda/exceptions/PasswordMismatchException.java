package com.mitienda.gestion_tienda.exceptions;

import org.springframework.http.HttpStatus;

public class PasswordMismatchException extends ApiException {
    public PasswordMismatchException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}