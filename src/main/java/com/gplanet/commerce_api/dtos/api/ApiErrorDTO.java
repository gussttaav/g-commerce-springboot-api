package com.gplanet.commerce_api.dtos.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object (DTO) for API error responses.
 * This class provides a standardized structure for error responses
 * across the API, including timestamp, status code, error messages,
 * and additional details about the error.
 *
 * @author Gustavo
 * @version 1.0
 */
@Schema(description = "API error response data transfer object")
@Data
@Builder
@AllArgsConstructor
public class ApiErrorDTO {
    /**
     * The timestamp when the error occurred.
     */
    @Schema(description = "Timestamp when the error occurred")
    private LocalDateTime timestamp;

    /**
     * The HTTP status code of the error.
     */
    @Schema(description = "HTTP status code", example = "400")
    private int status;

    /**
     * The type or category of the error.
     */
    @Schema(description = "Error type", example = "Bad Request")
    private String error;

    /**
     * The main error message.
     */
    @Schema(description = "Error message", example = "Invalid input")
    private String message;

    /**
     * The API endpoint path where the error occurred.
     */
    @Schema(description = "Request path", example = "/api/productos/crear")
    private String path;

    /**
     * Additional detailed error messages or validation errors.
     */
    @Schema(description = "Detailed error messages")
    private List<String> details;
}
