package com.gplanet.commerce.api.utilities;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import com.gplanet.commerce.api.exceptions.ApiException;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for handling database operations safely.
 * Provides methods to execute database operations while handling common exceptions
 * in a consistent manner.
 * 
 * @author Gustavo
 * @version 1.0
 */
@Slf4j
public final class DatabaseOperationHandler {

    /**
     * Private constructor to prevent instantiation of this utility class.
     * Throws UnsupportedOperationException if called.
     */
    private DatabaseOperationHandler() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Executes a database operation that returns a value.
     * Handles DataIntegrityViolationException and wraps it in an ApiException.
     *
     * @param <T> the type of value returned by the operation
     * @param operation the operation to execute
     * @return the result of the operation
     * @throws ApiException if a database constraint is violated
     */
    public static <T> T executeOperation(DatabaseOperation<T> operation) {
        try {
            return operation.execute();
        } catch (DataIntegrityViolationException ex) {
            DatabaseError error = DatabaseErrorParser.parse(ex);
            throw new ApiException(error.getDetail(), HttpStatus.CONFLICT);
        }
    }

    /**
     * Executes a database operation that does not return a value.
     * Handles DataIntegrityViolationException and wraps it in an ApiException.
     *
     * @param operation the operation to execute
     * @throws ApiException if a database constraint is violated
     */
    public static void executeOperation(Runnable operation) {
        try {
            operation.run();
        } catch (DataIntegrityViolationException ex) {
            DatabaseError error = DatabaseErrorParser.parse(ex);
            throw new ApiException(error.getDetail(), HttpStatus.CONFLICT);
        }
    }

    /**
     * Functional interface for database operations that return a value.
     *
     * @param <T> the type of value returned by the operation
     */
    @FunctionalInterface
    public interface DatabaseOperation<T> {
        /**
         * Executes the database operation.
         *
         * @return the result of the operation
         * @throws DataIntegrityViolationException if a database constraint is violated
         */
        T execute() throws DataIntegrityViolationException;
    }
}
