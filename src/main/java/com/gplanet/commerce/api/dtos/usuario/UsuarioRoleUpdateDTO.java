package com.gplanet.commerce.api.dtos.usuario;

import com.gplanet.commerce.api.entities.Usuario;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for updating a user's role.
 * <p>
 * The payload contains the new role to assign to the user. Valid values are
 * {@code ADMIN} and {@code USER}. Only users with ADMIN privileges should be
 * allowed to perform this operation.
 * </p>
 *
 * @author Gustavo
 * @version 1.0
 * 
 * @param rol The new role to assign to the user (ADMIN or USER).
 */
@Schema(name = "RoleUpdate", description = "Payload to update a user's role. Valid values: ADMIN, USER")
public record UsuarioRoleUpdateDTO(
    @Schema(description = "New role to assign (ADMIN or USER)", example = "ADMIN", nullable = false,
            allowableValues = {"ADMIN", "USER"})
    @NotNull(message = "El rol es obligatorio")
    Usuario.Role rol
) {}
