package com.gplanet.commerce.api.dtos.user;

import com.gplanet.commerce.api.entities.User;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for user registration operations.
 * This record contains the basic user information required for registration,
 * including name, email, and password.
 * Only users with ADMIN role may create new users with ADMIN role.
 *
 * @author Gustavo
 * @version 1.0
 * 
 * @param name The full name of the user.
 * @param email The email address for the user account, used as username.
 * @param password The password for the user account.
 * @param role The role assigned to the user in the system. Possible values: USER, ADMIN. Defaults to USER role.
 */
@Schema(name = "UserRegistration", 
        description = "User registration data")
public record UserDTO(
    @Schema(description = "User's full name", example = "John Doe")
    @NotBlank(message = "Name is required")
    String name,

    @Schema(description = "User's email address", example = "john.doe@example.com")
    @Email(message = "Email must be valid")
    @NotBlank(message = "Email is required")
    String email,

    @Schema(description = "User's password", example = "password123")
    @NotBlank(message = "Password is required")
    String password,

    @Schema(
        description = "User role. Allowed values: USER, ADMIN. "
                    + "Only ADMIN users may create users with ADMIN role.",
        example = "USER",
        defaultValue = "USER",
        allowableValues = {"USER", "ADMIN"}
    )
    User.Role role
) {
    /**
     * Creates a UserDTO, giving the USER role to a registration that doesn't request a role.
     *
     * @param name The full name of the user.
     * @param email The email address for the user account, used as username.
     * @param password The password for the user account.
     * @param role The role assigned to the user, or {@code null} for the USER role.
     */
    public UserDTO {
        if (role == null) {
            role = User.Role.USER;
        }
    }

    /**
     * Creates a UsuarioDTO with default USER role.
     *
     * @param nam The full name of the user.
     * @param email The email address for the user account, used as username.
     * @param password The password for the user account.
     */
    public UserDTO(String name, String email, String password) {
        this(name, email, password, User.Role.USER);
    }
}
