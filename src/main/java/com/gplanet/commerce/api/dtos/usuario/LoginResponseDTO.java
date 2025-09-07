package com.gplanet.commerce.api.dtos.usuario;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for handling user login responses.
 * <p>
 * This record encapsulates the authenticated user information, the JWT token,
 * the token type, and the token expiration time in seconds.
 * </p>
 *
 * @param usuario   The authenticated user's information.
 * @param token     The JWT token issued after successful authentication.
 * @param tokenType The type of the token (e.g., "Bearer").
 * @param expiresIn The number of seconds until the token expires.
 * 
 * @author Gustavo
 * @version 1.0
 */
@Schema(name = "LoginResponse", description = "Login response data")
public record LoginResponseDTO(
    @Schema(description = "Authenticated user information")
    UsuarioResponseDTO usuario,

    @Schema(description = "JWT token issued after successful authentication", 
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    String token,

    @Schema(description = "Type of the token", example = "Bearer")
    String tokenType,

    @Schema(description = "Token expiration time in seconds", example = "3600")
    long expiresIn
) {}
