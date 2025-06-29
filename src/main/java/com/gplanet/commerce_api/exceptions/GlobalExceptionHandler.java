package com.gplanet.commerce_api.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.gplanet.commerce_api.dtos.api.ApiErrorDTO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler that provides centralized exception handling across the application.
 * Translates various exceptions into standardized API responses.
 * Uses {@link ApiErrorDTO} to maintain consistent error response format.
 *
 * @author Gustavo
 * @version 1.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles validation errors from @Valid annotated request parameters.
     * Consolidates field errors into a structured error response.
     *
     * @param ex The validation exception containing field errors
     * @param request The current HTTP request
     * @return ResponseEntity containing ApiErrorDTO with validation error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorDTO> handleValidationErrors(MethodArgumentNotValidException ex, 
                                                            HttpServletRequest request) {
        log.warn("Validation error occurred for request to {}: {}", request.getRequestURI(), ex.getMessage());
        List<String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        ApiErrorDTO error = new ApiErrorDTO(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Validation Error",
                "Error de validación",
                request.getRequestURI(),
                details
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles constraint violation errors from validated beans.
     * Typically triggered by @Validated annotations on method parameters.
     *
     * @param ex The constraint violation exception
     * @param request The current HTTP request
     * @return ResponseEntity containing ApiErrorDTO with constraint violation details
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorDTO> handleConstraintViolation(ConstraintViolationException ex,
                                                               HttpServletRequest request) {
        log.warn("Constraint violation occurred for request to {}: {}", request.getRequestURI(), ex.getMessage());
        List<String> details = ex.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());

        ApiErrorDTO error = new ApiErrorDTO(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Constraint Violation",
                "Error de validación",
                request.getRequestURI(),
                details
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles errors that occur during HTTP message conversion, specifically when the request body
     * cannot be properly read or parsed into JSON.
     * 
     *
     * @param ex The HttpMessageNotReadableException thrown during message conversion.
     *           Contains details about the specific JSON parsing error that occurred.
     * @param request The current HTTP request being processed. Used to include the
     *                request URI in the error response for debugging purposes.
     * @return ResponseEntity containing ApiErrorDTO with JSON parsing error details
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorDTO> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpServletRequest request) {
        log.warn("Message not readable error for request to {}: {}", request.getRequestURI(), ex.getMessage());
        String message = "Error en el formato JSON";
        List<String> details = new ArrayList<>();
        details.add(ex.getMessage());

        ApiErrorDTO error = new ApiErrorDTO(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "JSON Parse Error",
                message,
                request.getRequestURI(),
                details
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles custom API exceptions thrown by the application.
     * Maintains the specific HTTP status code defined in the exception.
     *
     * @param ex The custom API exception
     * @param request The current HTTP request
     * @return ResponseEntity containing ApiErrorDTO with exception details
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorDTO> handleApiException(ApiException ex, HttpServletRequest request) {
        log.error("API exception occurred for request to {} with status {}: {}", 
            request.getRequestURI(), ex.getStatus(), ex.getMessage());
        ApiErrorDTO error = new ApiErrorDTO(
                LocalDateTime.now(),
                ex.getStatus().value(),
                ex.getStatus().getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                new ArrayList<>()
        );

        return ResponseEntity.status(ex.getStatus()).body(error);
    }

    /**
     * Handles Spring Security authentication exceptions.
     * Triggered when authentication fails for any reason.
     *
     * @param ex The authentication exception
     * @param request The current HTTP request
     * @return ResponseEntity containing ApiErrorDTO with authentication error details
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorDTO> handleAuthenticationException(AuthenticationException ex,
                                                                   HttpServletRequest request) {
        log.warn("Authentication failed for request to {}: {}", request.getRequestURI(), ex.getMessage());
        ApiErrorDTO error = new ApiErrorDTO(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "Authentication Error",
                ex.getMessage(),
                request.getRequestURI(),
                new ArrayList<>()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handles access denied exceptions from Spring Security.
     * Triggered when an authenticated user lacks required permissions.
     *
     * @param ex The access denied exception
     * @param request The current HTTP request
     * @return ResponseEntity containing ApiErrorDTO with access denied details
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorDTO> handleAccessDeniedException(AccessDeniedException ex,
                                                                 HttpServletRequest request) {
        log.warn("Access denied for request to {}: {}", request.getRequestURI(), ex.getMessage());
        ApiErrorDTO error = new ApiErrorDTO(
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                "Access Denied",
                "No tiene permisos para realizar esta operación",
                request.getRequestURI(),
                new ArrayList<>()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Fallback handler for all uncaught exceptions.
     * Logs the full exception details and returns a generic error message.
     *
     * @param ex The uncaught exception
     * @param request The current HTTP request
     * @return ResponseEntity containing ApiErrorDTO with generic error details
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorDTO> handleAllUncaughtException(Exception ex,
                                                                HttpServletRequest request) {
        log.error("Unexpected error occurred for request to {}", request.getRequestURI(), ex);
        
        ApiErrorDTO error = new ApiErrorDTO(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Ha ocurrido un error inesperado",
                request.getRequestURI(),
                new ArrayList<>()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Handles type mismatch exceptions in request parameters.
     * Provides detailed information about the expected and received parameter types.
     *
     * @param ex The type mismatch exception
     * @param request The current HTTP request
     * @return ResponseEntity containing ApiErrorDTO with type mismatch details
     */
    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<ApiErrorDTO> handleTypeMismatch(TypeMismatchException ex,
                                                        HttpServletRequest request) {
        log.warn("Type mismatch error for request to {}: {}", request.getRequestURI(), ex.getMessage());
        List<String> details = new ArrayList<>();
        String message = "Error en el tipo de dato";
        
        if (ex instanceof MethodArgumentTypeMismatchException) {
            MethodArgumentTypeMismatchException matex = (MethodArgumentTypeMismatchException) ex;
            String paramName = matex.getParameter().getParameterName();
            String requiredType = matex.getRequiredType() != null ? 
                                matex.getRequiredType().getSimpleName() : 
                                "desconocido";
            String invalidValue = matex.getValue() != null ? 
                                matex.getValue().toString() : 
                                "null";
                                
            message = String.format("El parámetro '%s' debe ser de tipo %s", paramName, requiredType);
            details.add(String.format("Valor recibido: '%s'", invalidValue));
            details.add(String.format("Tipo esperado: %s", requiredType));
        }

        ApiErrorDTO error = new ApiErrorDTO(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Type Mismatch",
                message,
                request.getRequestURI(),
                details
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles database integrity violation exceptions.
     * Provides specific error messages for common database constraints.
     *
     * @param ex The data integrity violation exception
     * @param request The current HTTP request
     * @return ResponseEntity containing ApiErrorDTO with database error details
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorDTO> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {
        log.error("Data integrity violation for request to {}: {}", 
            request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        
        String message = "Error de integridad de datos";
        List<String> details = new ArrayList<>();
        
        // Extract specific database error
        String errorMessage = ex.getMostSpecificCause().getMessage();
        
        // Handle specific cases
        if (errorMessage.contains("Duplicate entry")) {
            message = "Ya existe un registro con estos datos";
            // Extract the duplicate value from the error message
            String duplicateValue = extractDuplicateValue(errorMessage);
            details.add("Valor duplicado: " + duplicateValue);
        } else if (errorMessage.contains("cannot be null")) {
            message = "Datos requeridos no proporcionados";
            // Extract the column name from the error message
            String columnName = extractColumnName(errorMessage);
            details.add("Campo requerido: " + columnName);
        } else if (errorMessage.contains("foreign key constraint")) {
            message = "Violación de integridad referencial";
            details.add("No se puede realizar la operación porque afectaría datos relacionados");
        }

        ApiErrorDTO error = new ApiErrorDTO(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                "Data Integrity Violation",
                message,
                request.getRequestURI(),
                details
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Extracts the duplicate value from a database error message.
     *
     * @param errorMessage The database error message
     * @return The extracted duplicate value or an error message
     */
    private String extractDuplicateValue(String errorMessage) {
        try {
            int startIndex = errorMessage.indexOf("'") + 1;
            int endIndex = errorMessage.indexOf("'", startIndex);
            return errorMessage.substring(startIndex, endIndex);
        } catch (Exception e) {
            return "No se pudo extraer el valor duplicado";
        }
    }

    /**
     * Extracts the column name from a database error message.
     *
     * @param errorMessage The database error message
     * @return The extracted column name or an error message
     */
    private String extractColumnName(String errorMessage) {
        try {
            int startIndex = errorMessage.indexOf("'") + 1;
            int endIndex = errorMessage.indexOf("'", startIndex);
            return errorMessage.substring(startIndex, endIndex);
        } catch (Exception e) {
            return "No se pudo extraer el nombre de la columna";
        }
    }
}
