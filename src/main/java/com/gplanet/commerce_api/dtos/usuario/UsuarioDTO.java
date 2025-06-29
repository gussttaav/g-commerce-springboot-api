package com.gplanet.commerce_api.dtos.usuario;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Base DTO for user registration operations.
 * This record contains the basic user information required for registration,
 * including name, email, and password.
 *
 * @author Gustavo
 * @version 1.0
 */
@Schema(name = "RegularUserRegistration", 
        description = "User registration data")
public record UsuarioDTO(
    /** 
     * The full name of the user. 
     */
    @Schema(description = "User's full name", example = "John Doe")
    @NotBlank(message = "El nombre es obligatorio")
    String nombre,
    
    /** 
     * The email address for the user account, used as username. 
     */
    @Schema(description = "User's email address", example = "john.doe@example.com")
    @Email(message = "El email debe ser válido")
    @NotBlank(message = "El email es obligatorio")
    String email,
    
    /** 
     * The password for the user account. 
     */
    @Schema(description = "User's password", example = "password123")
    @NotBlank(message = "La contraseña es obligatoria")
    String password
) {}
