package com.gplanet.commerce_api.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import com.gplanet.commerce_api.dtos.api.PaginatedResponse;
import com.gplanet.commerce_api.dtos.usuario.ActualizacionUsuarioDTO;
import com.gplanet.commerce_api.dtos.usuario.CambioPasswdDTO;
import com.gplanet.commerce_api.dtos.usuario.LoginDTO;
import com.gplanet.commerce_api.dtos.usuario.PaginatedResponseUsuarioDTO;
import com.gplanet.commerce_api.dtos.usuario.UsuarioAdminDTO;
import com.gplanet.commerce_api.dtos.usuario.UsuarioDTO;
import com.gplanet.commerce_api.dtos.usuario.UsuarioResponseDTO;
import com.gplanet.commerce_api.entities.Usuario;
import com.gplanet.commerce_api.exceptions.ApiException;
import com.gplanet.commerce_api.exceptions.InvalidPasswordException;
import com.gplanet.commerce_api.exceptions.PasswordMismatchException;
import com.gplanet.commerce_api.exceptions.ResourceNotFoundException;
import com.gplanet.commerce_api.services.UsuarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST Controller for managing user operations and authentication.
 * 
 * This controller handles all user-related operations including:
 * <ul>
 *   <li>User registration and authentication</li>
 *   <li>Profile retrieve and update</li>
 *   <li>Password management</li>
 *   <li>Admin user registration (admin only)</li>
 *   <li>List of all users (admin only)</li>
 *   <li>Change user role (admin only)</li>
 * </ul>
 * 
 * Most operations require authentication except for registration and login.
 * Admin operations are restricted to users with administrative privileges.
 * 
 * @author Gustavo
 * @version 1.0
 */
@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "User", 
     description = "API endpoints for user management")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    /**
     * Registers a new user in the system.
     * 
     * @param usuarioDTO the user registration data containing email, password, and personal information
     * @return UsuarioResponseDTO containing the created user's information
     * @throws ApiException if the user could not be registered due to a constraint violation
     */
    @SecurityRequirements(value = {})
    @Operation(summary = "Register a new user", description = "Creates a new user account with the provided information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User successfully registered", 
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioResponseDTO.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/InvalidInput"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/DuplicatedEmail"),
            @ApiResponse(responseCode = "429", ref = "#/components/responses/UnauthenticatedRateLimitExceeded")
    })
    @PostMapping("/registro")
    public UsuarioResponseDTO registrarUsuario(
            @Valid @RequestBody @Parameter(description = "User registration details", required = true) 
            UsuarioDTO usuarioDTO) {
        return usuarioService.registrarUsuario(usuarioDTO);
    }


    /**
     * Authenticates a user with their credentials.
     * 
     * @param loginDTO the login credentials containing email and password
     * @return UsuarioResponseDTO containing the authenticated user's information
     * @throws ResourceNotFoundException if the user email is not found
     * @throws InvalidPasswordException if the user password is incorrect
     */
    @SecurityRequirements(value = {})
    @Operation(summary = "Authenticate user", description = "Authenticates a user with email and password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully authenticated", 
                content = @Content(schema = @Schema(implementation = UsuarioResponseDTO.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/InvalidInput"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/UserNotFound"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/InvalidPassword"),
            @ApiResponse(responseCode = "429", ref = "#/components/responses/UnauthenticatedRateLimitExceeded")
    })
    @PostMapping("/login")
    public UsuarioResponseDTO login(
            @Valid @RequestBody @Parameter(description = "User credentials", required = true) 
            LoginDTO loginDTO) {
        return usuarioService.login(loginDTO);
    }


    /**
     * Creates a new administrator user account.
     * Only existing administrators can create new admin accounts.
     * 
     * @param usuarioDTO the admin user data containing email, password, and role information
     * @return UsuarioResponseDTO containing the created admin user's information
     * @throws ApiException if the user could not be registered due to a constraint violation
     */
    @Operation(summary = "Register admin user", description = "Creates a new admin user account (requires ADMIN role)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Admin user successfully registered", 
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioResponseDTO.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/InvalidInput"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AccessDenied"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/AccessDeniedUser"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/DuplicatedEmail"),
            @ApiResponse(responseCode = "429", ref = "#/components/responses/AdminRateLimitExceeded")
    })
    @PostMapping("/admin/registro")
    public UsuarioResponseDTO registrarAdmin(
            @Valid @RequestBody @Parameter(description = "Admin user credentials", required = true) 
            UsuarioAdminDTO usuarioDTO) {
        return usuarioService.registrarUsuario(usuarioDTO);
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
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioResponseDTO.class))),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AccessDenied"),
            @ApiResponse(responseCode = "429", ref = "#/components/responses/UserRateLimitExceeded")
    })
    @GetMapping("/perfil")
    public UsuarioResponseDTO obtenerPerfil(Authentication authentication) {
        return usuarioService.obtenerPerfil(authentication.getName());
    }


    /**
     * Updates the profile information of the authenticated user.
     * 
     * @param authentication the current user's authentication object
     * @param perfilDTO the profile information to be updated
     * @return UsuarioResponseDTO containing the updated user information
     * @throws UsernameNotFoundException if the user is not found
     * @throws ApiException if the user could not be updated due to a constraint violation
     */
    @Operation(summary = "Update user profile", description = "Updates the authenticated user's profile information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile successfully updated", 
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioResponseDTO.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/InvalidInput"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AccessDenied"),
            @ApiResponse(responseCode = "429", ref = "#/components/responses/UserRateLimitExceeded")
    })
    @PutMapping("/perfil")
    public UsuarioResponseDTO actualizarPerfil(
            Authentication authentication,
            @Valid @RequestBody @Parameter(description = "Updated profile information", required = true) 
            ActualizacionUsuarioDTO perfilDTO) {
        return usuarioService.actualizarPerfil(
                authentication.getName(), perfilDTO);
    }


    /**
     * Changes the password of the authenticated user.
     * 
     * @param authentication the current user's authentication object
     * @param contraseñaDTO the password change details containing old and new passwords
     * @throws UsernameNotFoundException if the user is not found
     * @throws InvalidPasswordException if the old password is incorrect or the new password is equal to the new one
     * @throws PasswordMismatchException if the new password is not confirmed correctly
     */
    @Operation(summary = "Change password", description = "Changes the authenticated user's password")    
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password successfully changed"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/InvalidInput"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/InvalidPassword"),
            @ApiResponse(responseCode = "429", ref = "#/components/responses/UserRateLimitExceeded")
    })
    @PutMapping("/password")
    public void cambiarContraseña(
            Authentication authentication,
            @Valid @RequestBody @Parameter(description = "Password change details", required = true) 
            CambioPasswdDTO contraseñaDTO) {
        usuarioService.cambiarContraseña(authentication.getName(), contraseñaDTO);
    }


    /**
     * Lists all users in the system with pagination support. Only accessible by administrators.
     * 
     * @param page The page number (zero-based)
     * @param size The page size
     * @param sort The field to sort by
     * @param direction The sort direction (ASC or DESC)
     * @return PaginatedResponse of UsuarioResponseDTO containing paginated users' information
     * @throws AccessDeniedException if the current user is not an administrator
     */
    @Operation(summary = "List all users with pagination", 
               description = "Retrieves a paginated list of all users in the system. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users successfully retrieved", 
                content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = PaginatedResponseUsuarioDTO.class))),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AccessDenied"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/AccessDeniedUser"),
            @ApiResponse(responseCode = "429", ref = "#/components/responses/AdminRateLimitExceeded")
    })
    @GetMapping("/admin/listar")
    public PaginatedResponse<UsuarioResponseDTO> listarUsuarios(
            @RequestParam(defaultValue = "0") @Schema(description = "Page number (zero-based)", example = "0") int page,
            @RequestParam(defaultValue = "10") @Schema(description = "Page size", example = "10") int size,
            @RequestParam(defaultValue = "email") @Schema(description = "Sort field", example = "email") String sort,
            @RequestParam(defaultValue = "ASC") @Schema(description = "Sort direction", example = "ASC", allowableValues = {"ASC", "DESC"}) String direction) {
        
        Page<UsuarioResponseDTO> pageResult = usuarioService.listarUsuarios(page, size, sort, direction);
        return PaginatedResponse.fromPage(pageResult);
    }
    

    /**
     * Updates a user's role. Only accessible by administrators.
     * 
     * @param userId the ID of the user whose role should be updated
     * @param newRole the new role to assign to the user
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
    @PutMapping("/admin/change-role")
    public void cambiarRol(
            @RequestParam @Parameter(description = "ID of the user", required = true) Long userId,
            @RequestParam @Parameter(description = "New role (ADMIN or USER)", required = true) Usuario.Role newRole) {
        usuarioService.cambiarRol(userId, newRole);
    }
}
