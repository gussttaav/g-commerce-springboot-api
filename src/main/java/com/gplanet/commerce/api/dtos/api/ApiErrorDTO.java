package com.gplanet.commerce.api.dtos.api;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Record for API error responses.
 * Provides a standardized structure for error responses
 * across the API, including timestamp, status code, error messages,
 * and additional details about the error.
 *
 * @author Gustavo
 * @version 1.0
 * @param timestamp The timestamp when the error occurred.
 * @param status The HTTP status code of the error.
 * @param error The type or category of the error.
 * @param message The main error message.
 * @param path The API endpoint path where the error occurred.
 * @param details Additional detailed error messages or validation errors.
 */
@Schema(description = "API error response data transfer object")
public record ApiErrorDTO(
    @Schema(description = "Timestamp when the error occurred")
    LocalDateTime timestamp,

    @Schema(description = "HTTP status code", example = "400")
    int status,

    @Schema(description = "Error type", example = "Bad Request")
    String error,

    @Schema(description = "Error message", example = "Invalid input")
    String message,

    @Schema(description = "Request path", example = "/api/productos/crear")
    String path,

    @Schema(description = "Detailed error messages")
    List<String> details
) {}
