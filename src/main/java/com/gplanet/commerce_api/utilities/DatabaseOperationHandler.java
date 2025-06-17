package com.gplanet.commerce_api.utilities;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import com.gplanet.commerce_api.exceptions.ApiException;

/**
 * Utility class for handling database operations safely.
 * Provides methods to execute database operations while handling common exceptions
 * in a consistent manner.
 * 
 * @author Gustavo
 * @version 1.0
 */
@Slf4j
public class DatabaseOperationHandler {

    /**
     * Functional interface for database operations that return a value.
     * @param <T> the type of value returned by the operation
     */
    @FunctionalInterface
    public interface DatabaseOperation<T> {
        /**
         * Executes the database operation.
         * @return the result of the operation
         * @throws DataIntegrityViolationException if a database constraint is violated
         */
        T execute() throws DataIntegrityViolationException;
    }

    /**
     * Executes a database operation that returns a value.
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
     * Executes a database operation that doesn't return a value.
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
}