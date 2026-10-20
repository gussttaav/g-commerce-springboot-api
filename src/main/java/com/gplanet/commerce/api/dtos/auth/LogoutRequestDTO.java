package com.gplanet.commerce.api.dtos.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for handling user logout requests.
 * This record encapsulates the refresh token required
 * for invalidating user sessions during logout operations.
 *
 * @param refreshToken The refresh token to be invalidated and blacklisted.
 * 
 * @author Gustavo
 * @version 1.0
 */
@Schema(name = "LogoutRequest", description = "Logout request data")
public record LogoutRequestDTO(
    @Schema(description = "Refresh token of the session, revoked together with the access token",
            example = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9...")
    @NotBlank(message = "Refresh token is required")
    String refreshToken
) {}
