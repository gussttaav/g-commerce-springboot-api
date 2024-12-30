package com.mitienda.gestion_tienda.dtos;

import com.mitienda.gestion_tienda.entities.Usuario;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UsuarioAdminDTO extends UsuarioDTO {
    private Usuario.Role rol = Usuario.Role.USER;
}
