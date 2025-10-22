package com.gplanet.commerce.api.dtos.user;

import org.mapstruct.Mapper;

import com.gplanet.commerce.api.entities.User;

/**
 * Mapper interface for converting between Usuario entity and DTOs.
 * This interface uses MapStruct to generate the implementation for
 * converting Usuario entities to UsuarioResponseDTO objects.
 *
 * @author Gustavo
 * @version 1.0
 */
@Mapper(componentModel = "spring")
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface UserMapper {
    /**
     * Converts a Usuario entity to a UsuarioResponseDTO.
     *
     * @param user the User entity to convert
     * @return the corresponding UsuarioResponseDTO
     */
    UserResponseDTO toUserResponseDTO(User user);
}
