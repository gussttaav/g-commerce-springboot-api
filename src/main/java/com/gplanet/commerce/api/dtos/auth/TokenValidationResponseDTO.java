package com.gplanet.commerce.api.dtos.auth;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for handling token validation responses.
 * <p>
 * This record encapsulates the result of token validation operations,
 * indicating whether the token is valid and providing additional
 * information about the validation outcome.
 * </p>
 *
 * @param valid   Indicates whether the token passed validation checks.
 * @param message Additional information about the validation result,
 *                such as error details or confirmation message.
 * 
 * @author Gustavo
 * @version 1.0
 */
@Schema(name = "TokenValidationResponse", description = "Token validation response data")
public record TokenValidationResponseDTO(
    @Schema(description = "Indicates if the token is valid", example = "true")
    boolean valid,

    @Schema(description = "Additional message about validation result", 
            example = "Token is valid and not blacklisted")
    String message
) {}
