package com.mitienda.gestion_tienda.utilities;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;

@Slf4j
public class DatabaseErrorParser {
    
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
    
    private static String extractConstraintName(String message) {
        int keyIdx = message.lastIndexOf("for key '");
        if (keyIdx != -1) {
            return message.substring(keyIdx + 9, message.lastIndexOf('\''));
        }
        throw new IllegalArgumentException("No se pudo extraer el nombre de la restricción");
    }
}
