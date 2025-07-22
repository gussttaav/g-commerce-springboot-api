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
        // Evita la instanciación
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
        
        return new DatabaseError("Error de base de datos", "Se produjo un error al procesar la operación");
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
                "Valor duplicado",
                String.format("Ya existe un registro con el mismo valor para '%s'", field)
            );
        } catch (Exception e) {
            log.error("Error parsing duplicate entry message", e);
            return new DatabaseError("Valor duplicado", "Ya existe un registro con estos datos");
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
                "Error de referencia",
                String.format("No se puede realizar la operación porque afectaría la relación '%s'", constraintName)
            );
        } catch (Exception e) {
            log.error("Error parsing foreign key message", e);
            return new DatabaseError(
                "Error de referencia",
                "No se puede realizar la operación porque afectaría datos relacionados"
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
                "Valor requerido",
                String.format("El campo '%s' no puede estar vacío", column)
            );
        } catch (Exception e) {
            log.error("Error parsing null constraint message", e);
            return new DatabaseError(
                "Valor requerido",
                "Uno o más campos requeridos están vacíos"
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
        throw new IllegalArgumentException("No se pudo extraer el nombre de la restricción");
    }
}
