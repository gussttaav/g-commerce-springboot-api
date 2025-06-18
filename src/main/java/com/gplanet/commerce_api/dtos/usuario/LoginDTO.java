package com.gplanet.commerce_api.dtos.usuario;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for handling user login requests.
 * This record encapsulates the credentials (email and password)
 * required for user authentication.
 *
 * @author Gustavo
 * @version 1.0
 */
@Schema(name = "Login", description = "Login request data")
public record LoginDTO(
    /** 
     * The email address associated with the user account. 
     */
    @Schema(description = "User's email address", example = "john.doe@example.com")
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    String email,
    
    /** 
     * The password for authentication. 
     */
    @Schema(description = "User's password", example = "password123")
    @NotBlank(message = "La contraseña es obligatoria")
    String password
) {}
