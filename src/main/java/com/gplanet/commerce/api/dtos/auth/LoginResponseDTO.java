package com.gplanet.commerce.api.dtos.auth;

import com.gplanet.commerce.api.dtos.usuario.UsuarioResponseDTO;

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
 * @param expiresIn The number of seconds until the access token expires.
 * @param refreshToken The JWT refresh token issued alongside the access token.
 * @param refreshExpiresIn The number of seconds until the refresh token expires.
 * 
 * @author Gustavo
 * @version 1.0
 */
@Schema(name = "LoginResponse", description = "Login response data")
public record LoginResponseDTO(
    @Schema(description = "Authenticated user information")
    UsuarioResponseDTO usuario,

    @Schema(description = "JWT access token issued after successful authentication", 
            example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...")
    String token,

    @Schema(description = "Type of the token", example = "Bearer")
    String tokenType,

    @Schema(description = "Access token expiration time in seconds", example = "3600")
    long expiresIn,

    @Schema(description = "JWT refresh token")
    String refreshToken,

    @Schema(description = "Refresh token expiration time in seconds", example = "86400")
    long refreshExpiresIn
) {}
