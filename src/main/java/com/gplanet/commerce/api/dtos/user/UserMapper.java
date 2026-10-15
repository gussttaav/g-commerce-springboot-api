package com.gplanet.commerce.api.dtos.user;

import org.mapstruct.Mapper;

import com.gplanet.commerce.api.entities.User;

/**
 * Mapper interface for converting between the User entity and DTOs.
 * This interface uses MapStruct to generate the implementation for
 * converting User entities to UserResponseDTO objects.
 *
 * @author Gustavo
 * @version 1.0
 */
@Mapper(componentModel = "spring")
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface UserMapper {
    /**
     * Converts a User entity to a UserResponseDTO.
     *
     * @param user the User entity to convert
     * @return the corresponding UserResponseDTO
     */
    UserResponseDTO toUserResponseDTO(User user);
}
