package com.gplanet.commerce.api.dtos.usuario;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for handling password change requests.
 * This record contains the current password and the new password information
 * required to process a password change request.
 *
 * @author Gustavo
 * @version 1.0
 * @param currentPassword The user's current password for verification.
 * @param newPassword The new password to be set for the user account.
 * @param confirmPassword Confirmation of the new password to prevent typing errors.
 */
@Schema(name = "PasswordChangeRequest", description = "Password change request data for the logged in user")
public record CambioPasswdDTO(
    @Schema(description = "User's current password", example = "oldPassword123")
    @NotBlank(message = "La contraseña actual es obligatoria")
    String currentPassword,

    @Schema(description = "New password", example = "newPassword123")
    @NotBlank(message = "La nueva contraseña es obligatoria")
    String newPassword,

    @Schema(description = "Confirmation of new password", example = "newPassword123")
    @NotBlank(message = "La confirmación de contraseña es obligatoria")
    String confirmPassword
) {}
