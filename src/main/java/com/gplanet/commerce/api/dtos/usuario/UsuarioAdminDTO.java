package com.gplanet.commerce.api.dtos.usuario;

import com.gplanet.commerce.api.entities.Usuario;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for administrative user operations.
 * This record contains all user information plus role information for administrative purposes.
 * This record is used when administrators create or modify user accounts with specific roles.
 *
 * @author Gustavo
 * @version 1.0
 * @param nombre The full name of the user.
 * @param email The email address for the user account, used as username.
 * @param password The password for the user account.
 * @param rol The role assigned to the user in the system. Defaults to USER role.
 */
@Schema(name = "AdmiUserRegistration", description = "Administrative user information for registration")
public record UsuarioAdminDTO(
    @Schema(description = "User's full name", example = "John Doe")
    @NotBlank(message = "El nombre es obligatorio")
    String nombre,

    @Schema(description = "User's email address", example = "john.doe@example.com")
    @Email(message = "El email debe ser válido")
    @NotBlank(message = "El email es obligatorio")
    String email,

    @Schema(description = "User's password", example = "password123")
    @NotBlank(message = "La contraseña es obligatoria")
    String password,

    @Schema(description = "User role", example = "USER", defaultValue = "USER")
    Usuario.Role rol
) {
    /**
     * Creates a UsuarioAdminDTO with default USER role.
     *
     * @param nombre The full name of the user.
     * @param email The email address for the user account, used as username.
     * @param password The password for the user account.
     */
    public UsuarioAdminDTO(String nombre, String email, String password) {
        this(nombre, email, password, Usuario.Role.USER);
    }
}
