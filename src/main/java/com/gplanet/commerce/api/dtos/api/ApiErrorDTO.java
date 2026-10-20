package com.gplanet.commerce.api.dtos.api;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDateTime;
import java.util.List;

import com.gplanet.commerce.api.configs.app.docs.ApiDocs;

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
@Schema(name = ApiDocs.ERROR_SCHEMA, description = "The body of every error response")
public record ApiErrorDTO(
    @Schema(description = "When the error occurred, in server time without a time zone offset",
            example = "2026-10-20T09:30:00", requiredMode = REQUIRED)
    LocalDateTime timestamp,

    @Schema(description = "HTTP status code", example = "404", requiredMode = REQUIRED)
    int status,

    @Schema(description = "Short name of the error", example = "Not Found", requiredMode = REQUIRED)
    String error,

    @Schema(description = "What went wrong", example = "Product not found", requiredMode = REQUIRED)
    String message,

    @Schema(description = "Path of the request", example = "/api/products/42", requiredMode = REQUIRED)
    String path,

    @Schema(description = "Specific problems, such as one entry per invalid field; may be empty",
            requiredMode = REQUIRED)
    List<String> details
) {}
