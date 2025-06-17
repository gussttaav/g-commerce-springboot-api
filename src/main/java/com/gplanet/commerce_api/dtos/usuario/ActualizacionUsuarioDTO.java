package com.gplanet.commerce_api.dtos.usuario;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DTO for user profile update operations.
 * This class encapsulates the user information that can be updated,
 * including the user's name and email address.
 *
 * @author Gustavo
 * @version 1.0
 */
@Schema(name = "UpdateUserInfo", 
        description = "Logged in user's information to be updated")
@Data
@AllArgsConstructor
public class ActualizacionUsuarioDTO {
    /** 
     * The updated full name of the user. 
     */
    @Schema(description = "User's name", example = "John Doe")
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    
    /** 
     * The new email address for the user account. 
     */
    @Schema(description = "User's new email", example = "john.doe@example.com")
    @Email(message = "El email debe ser válido")
    @NotBlank(message = "El email es obligatorio")
    private String nuevoEmail;
}
