package com.mitienda.gestion_tienda.dtos.usuario;

import org.mapstruct.Mapper;

import com.mitienda.gestion_tienda.entities.Usuario;

/**
 * Mapper interface for converting between Usuario entity and DTOs.
 * This interface uses MapStruct to generate the implementation for
 * converting Usuario entities to UsuarioResponseDTO objects.
 *
 * @author Gustavo
 * @version 1.0
 */
@Mapper(componentModel = "spring")
public interface UsuarioMapper {
    /**
     * Converts a Usuario entity to a UsuarioResponseDTO.
     *
     * @param usuario the Usuario entity to convert
     * @return the corresponding UsuarioResponseDTO
     */
    UsuarioResponseDTO toUsuarioResponseDTO(Usuario usuario);
}
