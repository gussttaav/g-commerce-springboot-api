package com.gplanet.commerce.api.dtos.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * DTO for user profile update operations. This record supports updating
 * the user's name and email, and optionally changing the password in the
 * same request.
 * <p> 
 * Password fields are optional (nullable). If password fields are provided,
 * the service will attempt to change the password; otherwise only the name
 * and/or email are updated. 
 * </p>
 *
 * @author Gustavo
 * @version 1.0
 * 
 * @param name The updated full name of the user.
 * @param newEmail The new email address for the user account.
 * @param currentPassword The user's current password for verification (optional).
 * @param newPassword The new password to be set (optional).
 * @param confirmPassword Confirmation of the new password (optional).
 */
@Schema(name = "UpdateUserInfo", description = "The profile fields to change; omitted fields keep their value. "
        + "To change the password, send the current password, the new one and its confirmation.")
public record UserUpdateDTO(
    @Schema(description = "New full name", example = "John A. Doe", nullable = true)
    @Size(min = 1, max = 100, message = "Name cannot be empty")
    String name,

    @Schema(description = "New email address; it must not belong to another user", example = "john.doe@example.com",
        nullable = true)
    @Email(message = "Email must be valid")
    String newEmail,

    @Schema(description = "Current password, required to change the password", example = "your-current-password",
        nullable = true)
    String currentPassword,

    @Schema(description = "New password, at least 8 characters and different from the current one",
        example = "your-new-password", nullable = true)
    @Size(min = 8, message = "New password must be at least 8 characters long")
    String newPassword,

    @Schema(description = "The new password again", example = "your-new-password", nullable = true)
    String confirmPassword
) {

    /**
     * Convenience constructor for updating only name and email.
     *
     * @param name updated name
     * @param newEmail updated email
     */
    public UserUpdateDTO(String name, String newEmail) {
        this(name, newEmail, null, null, null);
    }

    /**
     * Convenience constructor for changing password only.
     *
     * @param currentPassword the current password
     * @param newPassword the new password
     * @param confirmPassword confirmation of the new password
     */
    public UserUpdateDTO(String currentPassword, String newPassword, String confirmPassword) {
        this(null, null, currentPassword, newPassword, confirmPassword);
    }

}
