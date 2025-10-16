package com.gplanet.commerce.api.dtos.usuario;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * DTO for user profile update operations. This record supports updating
 * the user's name and email, and optionally changing the password in the
 * same request.
 * <p> 
 * Password fields are optional (nullable). If password fields are provided,
 * the service will attempt to change the password; otherwise only the name
 * and/or email are updated. 
 * </p>
 *
 * @author Gustavo
 * @version 1.0
 * @param nombre The updated full name of the user.
 * @param nuevoEmail The new email address for the user account.
 * @param currentPassword The user's current password for verification (optional).
 * @param newPassword The new password to be set (optional).
 * @param confirmPassword Confirmation of the new password (optional).
 */
@Schema(name = "UpdateUserInfo", description = "Logged in user's information to be updated; optionally change password")
public record UsuarioUpdateDTO(
    @Schema(description = "User's name (optional; if present must not be empty)", 
            example = "John Doe", nullable = true)
    @Size(min = 1, max = 100, message = "El nombre no puede estar vacío")
    String nombre,

    @Schema(description = "User's new email (optional; if present must be valid)", 
            example = "john.doe@example.com", nullable = true)
    @Email(message = "El email debe ser válido")
    String nuevoEmail,

    @Schema(description = "Current password (required to change password)", example = "oldPassword123", nullable = true)
    String currentPassword,

    @Schema(description = "New password", example = "newPassword123", nullable = true)
    @Size(min = 8, message = "La nueva contraseña debe tener al menos 8 caracteres")
    String newPassword,

    @Schema(description = "Confirmation of new password", example = "newPassword123", nullable = true)
    String confirmPassword
) {

    /**
     * Convenience constructor for updating only name and email.
     *
     * @param nombre updated name
     * @param nuevoEmail updated email
     */
    public UsuarioUpdateDTO(String nombre, String nuevoEmail) {
        this(nombre, nuevoEmail, null, null, null);
    }

    /**
     * Convenience constructor for changing password only.
     *
     * @param currentPassword the current password
     * @param newPassword the new password
     * @param confirmPassword confirmation of the new password
     */
    public UsuarioUpdateDTO(String currentPassword, String newPassword, String confirmPassword) {
        this(null, null, currentPassword, newPassword, confirmPassword);
    }

}
