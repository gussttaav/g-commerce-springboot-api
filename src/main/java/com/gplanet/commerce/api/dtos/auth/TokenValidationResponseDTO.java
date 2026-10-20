package com.gplanet.commerce.api.dtos.auth;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

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
@Schema(name = "TokenValidationResponse", description = "Whether the token would be accepted as an access token")
public record TokenValidationResponseDTO(
    @Schema(description = "Whether the token is a valid access token", example = "true", requiredMode = REQUIRED)
    boolean valid,

    @Schema(description = "`Token is valid`, or why the token is not valid", example = "Token is valid",
            requiredMode = REQUIRED)
    String message
) {}
