package com.gplanet.commerce.api.dtos.user;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDateTime;

import com.gplanet.commerce.api.entities.User;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for returning user information in API responses.
 * This record represents the user data that is sent back to clients,
 * containing non-sensitive user information such as ID, name, email,
 * role, and creation date.
 *
 * @author Gustavo
 * @version 1.0
 * 
 * @param id The unique identifier of the user in the database.
 * @param name The full name of the user.
 * @param email The email address associated with the user account.
 * @param role The role assigned to the user in the system.
 * @param creationDate The date and time when the user account was created.
 */
@Schema(name = "User", description = "A user account")
public record UserResponseDTO(
    @Schema(description = "User ID", example = "2", requiredMode = REQUIRED)
    Long id,

    @Schema(description = "Full name", example = "John Doe", requiredMode = REQUIRED)
    String name,

    @Schema(description = "Email address, used to sign in", example = "john@example.com", requiredMode = REQUIRED)
    String email,

    @Schema(description = "Role: `USER` can buy products, `ADMIN` can manage products and users", example = "USER",
            requiredMode = REQUIRED)
    User.Role role,

    @Schema(description = "When the account was created", example = "2026-10-01T09:30:00", requiredMode = REQUIRED)
    LocalDateTime creationDate
) {}
