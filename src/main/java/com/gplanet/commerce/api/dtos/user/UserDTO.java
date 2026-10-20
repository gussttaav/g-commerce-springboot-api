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
@Schema(name = "UserRegistration", description = "The data of a new user account")
public record UserDTO(
    @Schema(description = "Full name", example = "Maria Garcia")
    @NotBlank(message = "Name is required")
    String name,

    @Schema(description = "Email address, used to sign in; it must not be registered yet",
            example = "maria.garcia@example.com")
    @Email(message = "Email must be valid")
    @NotBlank(message = "Email is required")
    String email,

    @Schema(description = "Password", example = "Secret123!")
    @NotBlank(message = "Password is required")
    String password,

    @Schema(
        description = "Role of the new user. Only an administrator can register a user with the ADMIN role.",
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
     * Creates a UserDTO with default USER role.
     *
     * @param nam The full name of the user.
     * @param email The email address for the user account, used as username.
     * @param password The password for the user account.
     */
    public UserDTO(String name, String email, String password) {
        this(name, email, password, User.Role.USER);
    }
}
