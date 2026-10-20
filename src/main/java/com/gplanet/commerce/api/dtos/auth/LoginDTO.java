package com.gplanet.commerce.api.dtos.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for handling user login requests.
 * This record encapsulates the credentials (email and password)
 * required for user authentication.
 *
 * @param email The email address associated with the user account.
 * @param password The password for authentication.
 * 
 * @author Gustavo
 * @version 1.0
 */
@Schema(name = "Login", description = "Login request data")
public record LoginDTO(
    @Schema(description = "User's email address", example = "john@example.com")
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,

    @Schema(description = "User's password", example = "JohnDoe123!")
    @NotBlank(message = "Password is required")
    String password
) {}
