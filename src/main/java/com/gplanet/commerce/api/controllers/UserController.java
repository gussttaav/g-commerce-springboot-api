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

import com.gplanet.commerce.api.configs.security.AuthenticatedUser;
import com.gplanet.commerce.api.dtos.api.PaginatedResponse;
import com.gplanet.commerce.api.dtos.user.UserDTO;
import com.gplanet.commerce.api.dtos.user.UserResponseDTO;
import com.gplanet.commerce.api.dtos.user.UserRoleUpdateDTO;
import com.gplanet.commerce.api.dtos.user.UserUpdateDTO;
import com.gplanet.commerce.api.entities.User;
import com.gplanet.commerce.api.exceptions.ApiException;
import com.gplanet.commerce.api.exceptions.PasswordMismatchException;
import com.gplanet.commerce.api.exceptions.ResourceNotFoundException;
import com.gplanet.commerce.api.services.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
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
@Tag(name = "User", 
     description = "API endpoints for user management")
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
     * @return UsuarioResponseDTO containing the created user's information
     * @throws ApiException if the user could not be registered due to a constraint violation
     * @throws AccessDeniedException if a non-admin user attempts to create an ADMIN user
     */
    @SecurityRequirements(value = {})
    @Operation(summary = "Register a new user",
        description = "Creates a new user account. If the requested role is ADMIN, the caller must have ADMIN role.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User successfully registered",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserResponseDTO.class))),
        @ApiResponse(responseCode = "400", ref = "#/components/responses/InvalidInput"),
        @ApiResponse(responseCode = "401", ref = "#/components/responses/AccessDenied"),
        @ApiResponse(responseCode = "403", ref = "#/components/responses/AccessDeniedUser"),
        @ApiResponse(responseCode = "409", ref = "#/components/responses/DuplicatedEmail"),
        @ApiResponse(responseCode = "429", ref = "#/components/responses/UnauthenticatedRateLimitExceeded")
    })
    @PostMapping
    public UserResponseDTO register(
            Authentication authentication,
            @Valid @RequestBody @Parameter(description = "User registration details", required = true)
            UserDTO userDTO) {

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
     * @return UsuarioResponseDTO containing the user's information
     * @throws UsernameNotFoundException if the user is not found
     */
    @Operation(summary = "Get user profile", description = "Retrieves the authenticated user's profile information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profile successfully retrieved",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserResponseDTO.class))),
        @ApiResponse(responseCode = "401", ref = "#/components/responses/AccessDenied"),
        @ApiResponse(responseCode = "429", ref = "#/components/responses/UserRateLimitExceeded")
    })
    @GetMapping("/me")
    public UserResponseDTO getProfile(Authentication authentication) {
        return userService.getProfile(AuthenticatedUser.idOf(authentication));
    }


    /**
     * Updates the profile information of the authenticated user.
     * 
     * @param authentication the current user's authentication object
     * @param profileDTO the profile information to be updated
     * @return UsuarioResponseDTO containing the updated user information
     * @throws UsernameNotFoundException if the user is not found
     * @throws PasswordMismatchException if the new password is not confirmed correctly
     * @throws ApiException if the user could not be updated due to a constraint violation
     */
    @Operation(summary = "Update user profile", description = "Updates the authenticated user's profile information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profile successfully updated",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserResponseDTO.class))),
        @ApiResponse(responseCode = "400", ref = "#/components/responses/InvalidInput"),
        @ApiResponse(responseCode = "401", ref = "#/components/responses/AccessDenied"),
        @ApiResponse(responseCode = "429", ref = "#/components/responses/UserRateLimitExceeded")
    })
    @PatchMapping("/me")
    public UserResponseDTO updateProfile(
            Authentication authentication,
            @Valid @RequestBody @Parameter(description = "Updated profile information", required = true) 
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
     * @return PaginatedResponse of UsuarioResponseDTO containing paginated users' information
     * @throws AccessDeniedException if the current user is not an administrator
     */
    @Operation(summary = "List all users with pagination", 
               description = "Retrieves a paginated list of all users in the system. Requires ADMIN role.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Users successfully retrieved",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/PaginatedUsers"))),
        @ApiResponse(responseCode = "400", ref = "#/components/responses/InvalidInput"),
        @ApiResponse(responseCode = "401", ref = "#/components/responses/AccessDenied"),
        @ApiResponse(responseCode = "403", ref = "#/components/responses/AccessDeniedUser"),
        @ApiResponse(responseCode = "429", ref = "#/components/responses/AdminRateLimitExceeded")
    })
    @GetMapping
    public PaginatedResponse<UserResponseDTO> listUsers(
            @RequestParam(defaultValue = "0") @Min(0)
            @Schema(description = "Page number (zero-based)", example = "0") int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(PaginatedResponse.MAX_PAGE_SIZE)
            @Schema(description = "Page size", example = "10") int size,
            @RequestParam(defaultValue = "email")
            @Pattern(regexp = SORT_FIELDS, message = "must be one of " + SORT_FIELDS)
            @Schema(description = "Sort field", example = "email", allowableValues = {"email", "name", "creationDate"}) 
            String sort,
            @RequestParam(defaultValue = "ASC") 
            @Schema(description = "Sort direction", example = "ASC", allowableValues = {"ASC", "DESC"}) 
            String direction) {
        
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
     */
    @Operation(summary = "Change user role", description = "Updates a user's role in the system. Requires ADMIN role.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Role successfully updated"),
        @ApiResponse(responseCode = "400", ref = "#/components/responses/InvalidInput"),
        @ApiResponse(responseCode = "401", ref = "#/components/responses/AccessDenied"),
        @ApiResponse(responseCode = "403", ref = "#/components/responses/AccessDeniedUser"),
        @ApiResponse(responseCode = "404", ref = "#/components/responses/UserNotFound"),
        @ApiResponse(responseCode = "429", ref = "#/components/responses/AdminRateLimitExceeded")
    })
    @PatchMapping("/{id}/role")
    public void changeRole(
            @Parameter(description = "ID of the user", required = true)
            @NotNull @PathVariable("id") Long userId,
            @Valid @RequestBody
            @Parameter(description = "New role payload", required = true)
            UserRoleUpdateDTO roleDTO) {
        userService.changeRole(userId, roleDTO.role());
    }
}
