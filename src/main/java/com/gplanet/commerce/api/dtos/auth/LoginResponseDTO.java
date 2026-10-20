package com.gplanet.commerce.api.dtos.auth;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import com.gplanet.commerce.api.dtos.user.UserResponseDTO;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for handling user login responses.
 * <p>
 * This record encapsulates the authenticated user information, the JWT token,
 * the token type, and the token expiration time in seconds.
 * </p>
 *
 * @param user   The authenticated user's information.
 * @param token     The JWT token issued after successful authentication.
 * @param tokenType The type of the token (e.g., "Bearer").
 * @param expiresIn The number of seconds until the access token expires.
 * @param refreshToken The JWT refresh token issued alongside the access token.
 * @param refreshExpiresIn The number of seconds until the refresh token expires.
 * 
 * @author Gustavo
 * @version 1.0
 */
@Schema(name = "LoginResponse", description = "The signed-in user and a new pair of tokens")
public record LoginResponseDTO(
    @Schema(description = "The signed-in user", requiredMode = REQUIRED)
    UserResponseDTO user,

    @Schema(description = "Access token: send it as `Authorization: Bearer <token>`",
            example = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9...", requiredMode = REQUIRED)
    String token,

    @Schema(description = "Type of the tokens", example = "Bearer", requiredMode = REQUIRED)
    String tokenType,

    @Schema(description = "Seconds until the access token expires", example = "3600", requiredMode = REQUIRED)
    long expiresIn,

    @Schema(description = "Refresh token: exchange it for a new pair with `POST /api/auth/refresh`; it works once",
            example = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9...",
            requiredMode = REQUIRED)
    String refreshToken,

    @Schema(description = "Seconds until the refresh token expires", example = "86400", requiredMode = REQUIRED)
    long refreshExpiresIn
) {}
