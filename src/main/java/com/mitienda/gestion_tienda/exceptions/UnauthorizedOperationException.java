package com.mitienda.gestion_tienda.exceptions;

import org.springframework.http.HttpStatus;

public class UnauthorizedOperationException extends ApiException {
    public UnauthorizedOperationException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}