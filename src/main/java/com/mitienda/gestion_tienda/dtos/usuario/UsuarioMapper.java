package com.mitienda.gestion_tienda.dtos.usuario;

import org.mapstruct.Mapper;

import com.mitienda.gestion_tienda.entities.Usuario;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {
    UsuarioResponseDTO toUsuarioResponseDTO(Usuario usuario);
}
