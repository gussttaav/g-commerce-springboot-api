package com.gplanet.commerce.api.utilities;

import org.springframework.dao.DataIntegrityViolationException;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for parsing database exceptions into user-friendly error messages.
 * Handles common database errors such as duplicate entries, foreign key violations,
 * and null constraints.
 * 
 * @author Gustavo
 * @version 1.0
 */
@Slf4j
public final class DatabaseErrorParser {
    
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private DatabaseErrorParser() {
        // Prevent instantiation
    }
    
    /**
     * Parses a DataIntegrityViolationException into a DatabaseError object.
     * @param ex the exception to parse
     * @return a DatabaseError containing user-friendly error messages
     */
    public static DatabaseError parse(DataIntegrityViolationException ex) {
        String originalMessage = ex.getMostSpecificCause().getMessage().toLowerCase();
        
        if (originalMessage.contains("duplicate entry")) {
            return handleDuplicateEntry(originalMessage);
        } else if (originalMessage.contains("foreign key constraint")) {
            return handleForeignKeyViolation(originalMessage);
        } else if (originalMessage.contains("cannot be null")) {
            return handleNullConstraint(originalMessage);
        }
        
        return new DatabaseError("Database error", "An error ocurred processing this operation");
    }
    
    /**
     * Handles duplicate entry database errors.
     * @param message the original error message
     * @return a DatabaseError with user-friendly messages about the duplication
     */
    private static DatabaseError handleDuplicateEntry(String message) {
        try {
            String field = extractConstraintName(message);
            return new DatabaseError(
                "Duplicated value",
                String.format("Already exists an entry with value '%s'", field)
            );
        } catch (Exception e) {
            log.error("Error parsing duplicate entry message", e);
            return new DatabaseError("Duplicated value", "Already exists a record with these data");
        }
    }
    
    /**
     * Handles foreign key constraint violation errors.
     * @param message the original error message
     * @return a DatabaseError with user-friendly messages about the constraint violation
     */
    private static DatabaseError handleForeignKeyViolation(String message) {
        try {
            String constraintName = extractConstraintName(message);
            return new DatabaseError(
                "Foreign key violation",
                String.format("The operation violates the relation '%s'", constraintName)
            );
        } catch (Exception e) {
            log.error("Error parsing foreign key message", e);
            return new DatabaseError(
                "Foreign key violation",
                "The operation violates integrity in related data"
            );
        }
    }
    
    /**
     * Handles null constraint violation errors.
     * @param message the original error message
     * @return a DatabaseError with user-friendly messages about the null constraint
     */
    private static DatabaseError handleNullConstraint(String message) {
        try {
            String column = message.substring(
                message.indexOf("column '") + 8,
                message.indexOf("' cannot")
            );
            return new DatabaseError(
                "Value required",
                String.format("The field '%s' can not be null", column)
            );
        } catch (Exception e) {
            log.error("Error parsing null constraint message", e);
            return new DatabaseError(
                "Value required",
                "One or more required fields are null"
            );
        }
    }
    
    /**
     * Extracts the constraint name from the error message.
     * @param message the error message containing the constraint information
     * @return the extracted constraint name
     * @throws IllegalArgumentException if the constraint name cannot be extracted
     */
    private static String extractConstraintName(String message) {
        int keyIdx = message.lastIndexOf("for key '");
        if (keyIdx != -1) {
            return message.substring(keyIdx + 9, message.lastIndexOf('\''));
        }
        throw new IllegalArgumentException("The restriction name could not be obtained");
    }
}
