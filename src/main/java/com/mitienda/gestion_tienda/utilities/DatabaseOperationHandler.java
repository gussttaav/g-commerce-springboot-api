package com.mitienda.gestion_tienda.utilities;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import com.mitienda.gestion_tienda.exceptions.ApiException;

@Slf4j
public class DatabaseOperationHandler {

    @FunctionalInterface
    public interface DatabaseOperation<T> {
        T execute() throws DataIntegrityViolationException;
    }

    public static <T> T executeOperation(DatabaseOperation<T> operation) {
        try {
            return operation.execute();
        } catch (DataIntegrityViolationException ex) {
            DatabaseError error = DatabaseErrorParser.parse(ex);
            throw new ApiException(error.getDetail(), HttpStatus.CONFLICT);
        }
    }

    // Overload for operations that don't return a value
    public static void executeOperation(Runnable operation) {
        try {
            operation.run();
        } catch (DataIntegrityViolationException ex) {
            DatabaseError error = DatabaseErrorParser.parse(ex);
            throw new ApiException(error.getDetail(), HttpStatus.CONFLICT);
        }
    }
}