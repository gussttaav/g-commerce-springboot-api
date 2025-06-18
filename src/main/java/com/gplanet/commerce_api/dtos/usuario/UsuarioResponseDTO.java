package com.gplanet.commerce_api.dtos.usuario;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import com.gplanet.commerce_api.entities.Usuario;

/**
 * DTO for returning user information in API responses.
 * This record represents the user data that is sent back to clients,
 * containing non-sensitive user information such as ID, name, email,
 * role, and creation date.
 *
 * @author Gustavo
 * @version 1.0
 */
@Schema(name= "User", 
        description = "User data object")
public record UsuarioResponseDTO(
    /** 
     * The unique identifier of the user in the database. 
     */
    @Schema(description = "User's unique identifier", example = "1")
    Long id,
    
    /** 
     * The full name of the user. 
     */
    @Schema(description = "User's full name", example = "John Doe")
    String nombre,
    
    /** 
     * The email address associated with the user account. 
     */
    @Schema(description = "User's email address", example = "john.doe@example.com")
    String email,
    
    /** 
     * The role assigned to the user in the system. 
     */
    @Schema(description = "User's role in the system", example = "USER")
    Usuario.Role rol,
    
    /** 
     * The date and time when the user account was created. 
     */
    @Schema(description = "Account creation timestamp", example = "2023-01-01T10:00:00")
    LocalDateTime fechaCreacion
) {}
