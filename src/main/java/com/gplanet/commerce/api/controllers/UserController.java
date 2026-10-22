package com.gplanet.commerce.api.controllers;

import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gplanet.commerce.api.configs.app.docs.ApiDocs;
import com.gplanet.commerce.api.configs.security.AuthenticatedUser;
import com.gplanet.commerce.api.dtos.api.PaginatedResponse;
import com.gplanet.commerce.api.dtos.user.UserDTO;
import com.gplanet.commerce.api.dtos.user.UserResponseDTO;
import com.gplanet.commerce.api.dtos.user.UserRoleUpdateDTO;
import com.gplanet.commerce.api.dtos.user.UserUpdateDTO;
import com.gplanet.commerce.api.entities.User;
import com.gplanet.commerce.api.exceptions.ApiException;
import com.gplanet.commerce.api.exceptions.PasswordMismatchException;
import com.gplanet.commerce.api.exceptions.ProtectedAccountException;
import com.gplanet.commerce.api.exceptions.ResourceNotFoundException;
import com.gplanet.commerce.api.services.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;

/**
 * REST Controller for managing user operations.
 * 
 * <p>
 * This controller handles all user-related operations including:
 * <ul>
 *   <li>User registration</li>
 *   <li>Profile retrieve and update</li>
 *   <li>Password management</li>
 *   <li>Admin user registration (admin only)</li>
 *   <li>List of all users (admin only)</li>
 *   <li>Change user role (admin only)</li>
 * </ul>
 * </p>
 * 
 * <p> Most operations require authentication except for registration.
 * Admin operations are restricted to users with administrative privileges.</p>
 *
 * @author Gustavo
 * @version 1.0
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = ApiDocs.TAG_USERS)
@RequiredArgsConstructor
@Validated
public class UserController {

    private static final String SORT_FIELDS = "email|name|creationDate";

    private final UserService userService;

    /**
     * Registers a new user in the system. 
     * If the requested role in the payload is {@code ADMIN}, the caller must be authenticated 
     * with ADMIN role; otherwise an {@link AccessDeniedException} will be thrown.
     *
     * @param authentication the current authentication object; may be null for unauthenticated requests
     * @param userDTO the user registration data containing email, password, and personal information
     * @return UserResponseDTO containing the created user's information
     * @throws ApiException if the user could not be registered due to a constraint violation
     * @throws AccessDeniedException if a non-admin user attempts to create an ADMIN user
     */
    // OpenAPIConfig documents the access token of this endpoint as optional
    @Operation(operationId = "registerUser", summary = "Register a user",
               description = "Creates a user account; sign in with it at `POST /api/auth/login`. Anyone can "
                           + "register a `USER`, but registering an `ADMIN` requires an administrator's access "
                           + "token.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "The new user"),
        @ApiResponse(responseCode = "400", ref = ApiDocs.INVALID_INPUT),
        @ApiResponse(responseCode = "401", ref = ApiDocs.AUTHENTICATION_REQUIRED),
        @ApiResponse(responseCode = "403", ref = ApiDocs.ADMIN_REGISTRATION_FORBIDDEN),
        @ApiResponse(responseCode = "409", ref = ApiDocs.DUPLICATED_EMAIL),
        @ApiResponse(responseCode = "429", ref = ApiDocs.RATE_LIMIT_EXCEEDED)
    })
    @PostMapping
    public UserResponseDTO register(
            @Parameter(hidden = true) Authentication authentication,
            @Valid @RequestBody UserDTO userDTO) {

        // If the request explicitly asks for ADMIN role, require caller to be ADMIN
        if (userDTO.role() == User.Role.ADMIN) {
            if (authentication == null || authentication.getAuthorities().stream()
                    .noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                throw new AccessDeniedException("Only administrators can create admin users");
            }
        }

        return userService.registerUser(userDTO);
    }


    /**
     * Retrieves the profile information of the authenticated user.
     * 
     * @param authentication the current user's authentication object
     * @return UserResponseDTO containing the user's information
     * @throws UsernameNotFoundException if the user is not found
     */
    @Operation(operationId = "getProfile", summary = "Retrieve your profile",
               description = "Returns the signed-in user.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "The signed-in user"),
        @ApiResponse(responseCode = "401", ref = ApiDocs.AUTHENTICATION_REQUIRED),
        @ApiResponse(responseCode = "429", ref = ApiDocs.RATE_LIMIT_EXCEEDED)
    })
    @GetMapping("/me")
    public UserResponseDTO getProfile(@Parameter(hidden = true) Authentication authentication) {
        return userService.getProfile(AuthenticatedUser.idOf(authentication));
    }


    /**
     * Updates the profile information of the authenticated user.
     * 
     * @param authentication the current user's authentication object
     * @param profileDTO the profile information to be updated
     * @return UserResponseDTO containing the updated user information
     * @throws UsernameNotFoundException if the user is not found
     * @throws PasswordMismatchException if the new password is not confirmed correctly
     * @throws ProtectedAccountException if the update would change the email or password of a demo account
     * @throws ApiException if the user could not be updated due to a constraint violation
     */
    @Operation(operationId = "updateProfile", summary = "Update your profile",
               description = "Changes the name, email or password of the signed-in user; omitted fields keep "
                           + "their value. A password change needs `currentPassword`, `newPassword` and "
                           + "`confirmPassword`. The user's tokens keep working after an email change. The email "
                           + "and password of the demo accounts can't be changed.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "The updated user"),
        @ApiResponse(responseCode = "400", ref = ApiDocs.INVALID_PROFILE_UPDATE),
        @ApiResponse(responseCode = "401", ref = ApiDocs.AUTHENTICATION_REQUIRED),
        @ApiResponse(responseCode = "403", ref = ApiDocs.DEMO_ACCOUNT_PROTECTED),
        @ApiResponse(responseCode = "409", ref = ApiDocs.DUPLICATED_EMAIL),
        @ApiResponse(responseCode = "429", ref = ApiDocs.RATE_LIMIT_EXCEEDED)
    })
    @PatchMapping("/me")
    public UserResponseDTO updateProfile(
            @Parameter(hidden = true) Authentication authentication,
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                schema = @Schema(implementation = UserUpdateDTO.class),
                examples = {
                    @ExampleObject(name = "rename", summary = "Change the name",
                        value = "{\"name\": \"John A. Doe\"}"),
                    @ExampleObject(name = "changeEmail", summary = "Change the email",
                        value = "{\"newEmail\": \"john.doe@example.com\"}"),
                    @ExampleObject(name = "changePassword", summary = "Change the password",
                        value = "{\"currentPassword\": \"your-current-password\", "
                            + "\"newPassword\": \"your-new-password\", \"confirmPassword\": \"your-new-password\"}")
                }))
            UserUpdateDTO profileDTO) {
        return userService.updateProfile(AuthenticatedUser.idOf(authentication), profileDTO);
    }


    /**
     * Lists all users in the system with pagination support. Only accessible by administrators.
     * 
     * @param page The page number (zero-based)
     * @param size The page size, at most {@value PaginatedResponse#MAX_PAGE_SIZE}
     * @param sort The field to sort by: email, name or creationDate
     * @param direction The sort direction (ASC or DESC)
     * @return PaginatedResponse of UserResponseDTO containing paginated users' information
     * @throws AccessDeniedException if the current user is not an administrator
     */
    @Operation(operationId = "listUsers", summary = "List users",
               description = "Returns a page of all the users. Requires the `ADMIN` role.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "A page of users",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = ApiDocs.PAGINATED_USERS))),
        @ApiResponse(responseCode = "400", ref = ApiDocs.INVALID_INPUT),
        @ApiResponse(responseCode = "401", ref = ApiDocs.AUTHENTICATION_REQUIRED),
        @ApiResponse(responseCode = "403", ref = ApiDocs.ADMIN_ROLE_REQUIRED),
        @ApiResponse(responseCode = "429", ref = ApiDocs.RATE_LIMIT_EXCEEDED)
    })
    @GetMapping
    public PaginatedResponse<UserResponseDTO> listUsers(
            @Parameter(description = "Page number, starting at 0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Users per page")
            @RequestParam(defaultValue = "10") @Min(1) @Max(PaginatedResponse.MAX_PAGE_SIZE) int size,
            @Parameter(description = "Field to sort by")
            @Schema(allowableValues = {"email", "name", "creationDate"})
            @RequestParam(defaultValue = "email")
            @Pattern(regexp = SORT_FIELDS, message = "must be one of " + SORT_FIELDS) String sort,
            @Parameter(description = "Sort direction")
            @Schema(allowableValues = {"ASC", "DESC"})
            @RequestParam(defaultValue = "ASC") String direction) {
        
        Page<UserResponseDTO> pageResult = userService.listUsers(page, size, sort, direction);
        return PaginatedResponse.fromPage(pageResult);
    }
    

    /**
     * Updates a user's role. Only accessible by administrators.
     * 
     * @param userId the ID of the user whose role should be updated
     * @param roleDTO the payload containing the new role to assign to the user
     * @throws AccessDeniedException if the current user is not an administrator
     * @throws ResourceNotFoundException if the user is not found
     * @throws ProtectedAccountException if the user is a demo account and the role is different
     */
    @Operation(operationId = "changeUserRole", summary = "Change a user's role",
               description = "Gives a user the `USER` or `ADMIN` role. Requires the `ADMIN` role. The user's "
                           + "current access token keeps the old role until it expires or is refreshed. The role "
                           + "of the demo accounts can't be changed.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Role changed; the response has no body"),
        @ApiResponse(responseCode = "400", ref = ApiDocs.INVALID_INPUT),
        @ApiResponse(responseCode = "401", ref = ApiDocs.AUTHENTICATION_REQUIRED),
        @ApiResponse(responseCode = "403", ref = ApiDocs.ROLE_CHANGE_FORBIDDEN),
        @ApiResponse(responseCode = "404", ref = ApiDocs.USER_NOT_FOUND),
        @ApiResponse(responseCode = "429", ref = ApiDocs.RATE_LIMIT_EXCEEDED)
    })
    @PatchMapping("/{id}/role")
    public void changeRole(
            @Parameter(description = "ID of the user", example = "5")
            @NotNull @PathVariable("id") Long userId,
            @Valid @RequestBody UserRoleUpdateDTO roleDTO) {
        userService.changeRole(userId, roleDTO.role());
    }
}
