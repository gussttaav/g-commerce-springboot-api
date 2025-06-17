package com.gplanet.commerce_api.dtos.usuario;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Base DTO for user registration operations.
 * This class contains the basic user information required for registration,
 * including name, email, and password.
 *
 * @author Gustavo
 * @version 1.0
 */
@Schema(name = "RegularUserRegistration", 
        description = "User registration data")
@Data
public class UsuarioDTO {
    /** 
     * The full name of the user. 
     */
    @Schema(description = "User's full name", example = "John Doe")
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    
    /** 
     * The email address for the user account, used as username. 
     */
    @Schema(description = "User's email address", example = "john.doe@example.com")
    @Email(message = "El email debe ser válido")
    @NotBlank(message = "El email es obligatorio")
    private String email;
    
    /** 
     * The password for the user account. 
     */
    @Schema(description = "User's password", example = "password123")
    @NotBlank(message = "La contraseña es obligatoria")
    private String password; 
}
