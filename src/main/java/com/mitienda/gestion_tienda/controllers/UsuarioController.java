package com.mitienda.gestion_tienda.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import com.mitienda.gestion_tienda.dtos.usuario.ActualizacionUsuarioDTO;
import com.mitienda.gestion_tienda.dtos.usuario.CambioPasswdDTO;
import com.mitienda.gestion_tienda.dtos.usuario.LoginDTO;
import com.mitienda.gestion_tienda.dtos.usuario.UsuarioAdminDTO;
import com.mitienda.gestion_tienda.dtos.usuario.UsuarioDTO;
import com.mitienda.gestion_tienda.dtos.usuario.UsuarioResponseDTO;
import com.mitienda.gestion_tienda.exceptions.ApiException;
import com.mitienda.gestion_tienda.exceptions.InvalidPasswordException;
import com.mitienda.gestion_tienda.exceptions.PasswordMismatchException;
import com.mitienda.gestion_tienda.exceptions.ResourceNotFoundException;
import com.mitienda.gestion_tienda.services.UsuarioService;

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
 *   <li>Admin user management</li>
 *   <li>Profile updates</li>
 *   <li>Password management</li>
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
            @ApiResponse(responseCode = "409", ref = "#/components/responses/DuplicatedEmail")
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
            @ApiResponse(responseCode = "401", ref = "#/components/responses/InvalidPassword")
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
            @ApiResponse(responseCode = "409", ref = "#/components/responses/DuplicatedEmail")
    })
    @PostMapping("/admin/registro")
    public UsuarioResponseDTO registrarAdmin(
            @Valid @RequestBody @Parameter(description = "Admin user credentials", required = true) 
            UsuarioAdminDTO usuarioDTO) {
        return usuarioService.registrarUsuario(usuarioDTO);
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
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AccessDenied")
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
            @ApiResponse(responseCode = "401", ref = "#/components/responses/InvalidPassword")
    })
    @PutMapping("/password")
    public void cambiarContraseña(
            Authentication authentication,
            @Valid @RequestBody @Parameter(description = "Password change details", required = true) 
            CambioPasswdDTO contraseñaDTO) {
        usuarioService.cambiarContraseña(authentication.getName(), contraseñaDTO);
    }
}
