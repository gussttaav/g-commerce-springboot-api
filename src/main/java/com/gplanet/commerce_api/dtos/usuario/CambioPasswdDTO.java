package com.gplanet.commerce_api.dtos.usuario;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO for handling password change requests.
 * This class contains the current password and the new password information
 * required to process a password change request.
 *
 * @author Gustavo
 * @version 1.0
 */
@Schema(name = "PasswordChangeRequest", description = "Password change request data for the logged in user")
@Data
public class CambioPasswdDTO {
    /** 
     * The user's current password for verification. 
     */
    @Schema(description = "User's current password", example = "oldPassword123")
    @NotBlank(message = "La contraseña actual es obligatoria")
    private String currentPassword;
    
    /** 
     * The new password to be set for the user account. 
     */
    @Schema(description = "New password", example = "newPassword123")
    @NotBlank(message = "La nueva contraseña es obligatoria")
    private String newPassword;
    
    /** 
     * Confirmation of the new password to prevent typing errors. 
     */
    @Schema(description = "Confirmation of new password", example = "newPassword123")
    @NotBlank(message = "La confirmación de contraseña es obligatoria")
    private String confirmPassword;
}
