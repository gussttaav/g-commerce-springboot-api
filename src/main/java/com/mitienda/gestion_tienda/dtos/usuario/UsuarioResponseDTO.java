package com.mitienda.gestion_tienda.dtos.usuario;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

import com.mitienda.gestion_tienda.entities.Usuario;

/**
 * DTO for returning user information in API responses.
 * This class represents the user data that is sent back to clients,
 * containing non-sensitive user information such as ID, name, email,
 * role, and creation date.
 *
 * @author Gustavo
 * @version 1.0
 */
@Schema(name= "User", 
        description = "User data object")
@Data
@AllArgsConstructor
@Builder
public class UsuarioResponseDTO {
    /** 
     * The unique identifier of the user in the database. 
     * */
    @Schema(description = "User's unique identifier", example = "1")
    private Long id;
    
    /** 
     * The full name of the user. 
     */
    @Schema(description = "User's full name", example = "John Doe")
    private String nombre;
    
    /** 
     * The email address associated with the user account. 
     */
    @Schema(description = "User's email address", example = "john.doe@example.com")
    private String email;
    
    /** 
     * The role assigned to the user in the system. 
     */
    @Schema(description = "User's role in the system", example = "USER")
    private Usuario.Role rol;
    
    /** 
     * The date and time when the user account was created. 
     */
    @Schema(description = "Account creation timestamp", example = "2023-01-01T10:00:00")
    private LocalDateTime fechaCreacion;
}
