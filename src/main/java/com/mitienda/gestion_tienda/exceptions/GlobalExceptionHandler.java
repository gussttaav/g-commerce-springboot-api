package com.mitienda.gestion_tienda.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.mitienda.gestion_tienda.dtos.ApiErrorDTO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorDTO> handleValidationErrors(MethodArgumentNotValidException ex, 
                                                            HttpServletRequest request) {
        List<String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        ApiErrorDTO error = ApiErrorDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Error")
                .message("Error de validación")
                .path(request.getRequestURI())
                .details(details)
                .build();

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorDTO> handleConstraintViolation(ConstraintViolationException ex,
                                                               HttpServletRequest request) {
        List<String> details = ex.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());

        ApiErrorDTO error = ApiErrorDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Constraint Violation")
                .message("Error de validación")
                .path(request.getRequestURI())
                .details(details)
                .build();

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorDTO> handleApiException(ApiException ex, HttpServletRequest request) {
        ApiErrorDTO error = ApiErrorDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(ex.getStatus().value())
                .error(ex.getStatus().getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .details(new ArrayList<>())
                .build();

        return new ResponseEntity<>(error, ex.getStatus());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorDTO> handleAuthenticationException(AuthenticationException ex,
                                                                   HttpServletRequest request) {
        ApiErrorDTO error = ApiErrorDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Authentication Error")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .details(new ArrayList<>())
                .build();

        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorDTO> handleAccessDeniedException(AccessDeniedException ex,
                                                                 HttpServletRequest request) {
        ApiErrorDTO error = ApiErrorDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Access Denied")
                .message("No tiene permisos para realizar esta operación")
                .path(request.getRequestURI())
                .details(new ArrayList<>())
                .build();

        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorDTO> handleAllUncaughtException(Exception ex,
                                                                HttpServletRequest request) {
        log.error("Unexpected error occurred", ex);
        
        ApiErrorDTO error = ApiErrorDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("Ha ocurrido un error inesperado")
                .path(request.getRequestURI())
                .details(new ArrayList<>())
                .build();

        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<ApiErrorDTO> handleTypeMismatch(TypeMismatchException ex,
                                                        HttpServletRequest request) {
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

        ApiErrorDTO error = ApiErrorDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Type Mismatch")
                .message(message)
                .path(request.getRequestURI())
                .details(details)
                .build();

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorDTO> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {
            
        log.error("Data integrity violation occurred", ex);
        
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

        ApiErrorDTO error = ApiErrorDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Data Integrity Violation")
                .message(message)
                .path(request.getRequestURI())
                .details(details)
                .build();

        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    private String extractDuplicateValue(String errorMessage) {
        try {
            int startIndex = errorMessage.indexOf("'") + 1;
            int endIndex = errorMessage.indexOf("'", startIndex);
            return errorMessage.substring(startIndex, endIndex);
        } catch (Exception e) {
            return "No se pudo extraer el valor duplicado";
        }
    }

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
