package com.gplanet.commerce_api.dtos.usuario;

import com.gplanet.commerce_api.entities.Usuario;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * DTO for administrative user operations.
 * Extends the basic user DTO and adds role information for administrative purposes.
 * This class is used when administrators create or modify user accounts with specific roles.
 * 
 * @author Gustavo
 * @version 1.0
 */
@Schema(name = "AdmiUserRegistration", description = "Administrative user information for registration")
@Data
@EqualsAndHashCode(callSuper = true)
public class UsuarioAdminDTO extends UsuarioDTO {

    /**
     * The role assigned to the user in the system. Defaults to USER role.
     */
    @Schema(description = "User role", example = "USER", defaultValue = "USER")
    private Usuario.Role rol = Usuario.Role.USER;
}
