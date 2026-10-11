package com.gplanet.commerce.api.dtos.jwks;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO record representing a JSON Web Key Set (JWKS) response.
 * Contains a collection of public keys used for JWT signature verification.
 *
 * @param keys List of JSON Web Keys
 * 
 * @author Gustavo
 * @version 1.0
 */
@Schema(description = "JSON Web Key Set containing public keys for JWT verification")
public record JwksResponseDTO(
    @Schema(description = "Array of JSON Web Keys", requiredMode = REQUIRED)
    List<JwkDTO> keys
) {
}
