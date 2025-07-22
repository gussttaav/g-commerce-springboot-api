package com.gplanet.commerce.api.dtos.usuario;

import java.time.LocalDateTime;

import com.gplanet.commerce.api.entities.Usuario;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for returning user information in API responses.
 * This record represents the user data that is sent back to clients,
 * containing non-sensitive user information such as ID, name, email,
 * role, and creation date.
 *
 * @author Gustavo
 * @version 1.0
 * @param id The unique identifier of the user in the database.
 * @param nombre The full name of the user.
 * @param email The email address associated with the user account.
 * @param rol The role assigned to the user in the system.
 * @param fechaCreacion The date and time when the user account was created.
 */
@Schema(name = "User", 
        description = "User data object")
public record UsuarioResponseDTO(
    @Schema(description = "User's unique identifier", example = "1")
    Long id,

    @Schema(description = "User's full name", example = "John Doe")
    String nombre,

    @Schema(description = "User's email address", example = "john.doe@example.com")
    String email,

    @Schema(description = "User's role in the system", example = "USER")
    Usuario.Role rol,

    @Schema(description = "Account creation timestamp", example = "2023-01-01T10:00:00")
    LocalDateTime fechaCreacion
) {}
