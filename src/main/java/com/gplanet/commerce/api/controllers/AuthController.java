package com.gplanet.commerce.api.controllers;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gplanet.commerce.api.dtos.auth.LoginDTO;
import com.gplanet.commerce.api.dtos.auth.LoginResponseDTO;
import com.gplanet.commerce.api.dtos.auth.LogoutRequestDTO;
import com.gplanet.commerce.api.dtos.auth.TokenValidationResponseDTO;
import com.gplanet.commerce.api.dtos.user.UserResponseDTO;
import com.gplanet.commerce.api.exceptions.InvalidPasswordException;
import com.gplanet.commerce.api.exceptions.InvalidTokenException;
import com.gplanet.commerce.api.exceptions.LogoutFailedException;
import com.gplanet.commerce.api.exceptions.TokenStorageException;
import com.gplanet.commerce.api.services.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for managing user authentication and token operations.
 * 
 * <p>This controller handles all authentication-related operations including:
 * <ul>
 *   <li>User login with credentials</li>
 *   <li>Token refresh operations</li>
 *   <li>User logout and token blacklisting</li>
 *   <li>Token validation</li>
 * </ul>
 * </p>
 * 
 * <p>Most endpoints in this controller do not require authentication except where noted.
 * Token management operations are available to authenticated users.</p>
 *
 * @author Gustavo
 * @version 1.0
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", 
    description = "API endpoints for user authentication and token management")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Authenticates a user with their credentials.
     * 
     * @param loginDTO the login credentials containing email and password
     * @return LoginResponseDTO containing the authenticated user's information
     * @throws InvalidPasswordException if the user password is incorrect
     * @throws UsernameNotFoundException if the user cannot be loaded
     */
    @SecurityRequirements
    @Operation(summary = "Authenticate user", description = "Authenticates a user with email and password")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully authenticated",
            content = @Content(schema = @Schema(implementation = UserResponseDTO.class))),
        @ApiResponse(responseCode = "400", ref = "#/components/responses/InvalidInput"),
        @ApiResponse(responseCode = "401", ref = "#/components/responses/InvalidPassword"),
        @ApiResponse(responseCode = "429", ref = "#/components/responses/UnauthenticatedRateLimitExceeded")
    })
    @PostMapping("/login")
    public LoginResponseDTO login(
            @Valid @RequestBody 
            @Parameter(description = "User credentials containing email and password", required = true) 
            LoginDTO loginDTO) {
        return authService.login(loginDTO);
    }

    /**
     * Refreshes an access token using a valid refresh token.
     * Generates new access and refresh tokens while invalidating the old refresh token.
     *
     * @param authorization the Authorization header containing the refresh token
     * @return LoginResponseDTO containing the new access and refresh tokens
     * @throws InvalidTokenException if the refresh token is invalid, expired, already used or revoked
     * @throws UsernameNotFoundException if the user cannot be loaded
     * @throws TokenStorageException if there's an error storing the new refresh token
     */
    @SecurityRequirements
    @Operation(summary = "Refresh access token", 
               description = "Generates new access and refresh tokens using a valid refresh token. "
                           + "The old refresh token becomes invalid after successful refresh.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tokens refreshed successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = LoginResponseDTO.class))),
        @ApiResponse(responseCode = "400", ref = "#/components/responses/InvalidInput"),
        @ApiResponse(responseCode = "401", ref = "#/components/responses/InvalidRefreshToken"),
        @ApiResponse(responseCode = "429", ref = "#/components/responses/UnauthenticatedRateLimitExceeded")
    })
    @PostMapping("/refresh")
    public LoginResponseDTO refresh(
            @RequestHeader("Authorization") 
            @Parameter(description = "Bearer refresh token", required = true, 
                      example = "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...") 
            String authorization) {
        return authService.refresh(authorization);
    }

    /**
     * Logs out a user by revoking the tokens of their session.
     * Invalidates both access and refresh tokens to prevent further use.
     *
     * @param logoutRequest the logout request containing token information
     * @param accessToken the access token that authenticated the request
     * @throws LogoutFailedException if the logout operation fails
     */
    @Operation(summary = "User logout", 
               description = "Logs out the authenticated user by revoking the access token used for this request "
                           + "and the given refresh token, preventing their future use. A refresh token that is "
                           + "invalid, expired or issued to another user is ignored.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User logged out successfully"),
        @ApiResponse(responseCode = "400", ref = "#/components/responses/InvalidInput"),
        @ApiResponse(responseCode = "401", ref = "#/components/responses/InvalidToken"),
        @ApiResponse(responseCode = "429", ref = "#/components/responses/UserRateLimitExceeded")
    })
    @PostMapping("/logout")
    public void logout(
            @Valid @RequestBody 
            @Parameter(description = "Logout request containing refresh token to invalidate", required = true) 
            LogoutRequestDTO logoutRequest, 
            @AuthenticationPrincipal @Parameter(hidden = true) Jwt accessToken) {
        authService.logout(logoutRequest, accessToken);
    }

    /**
     * Checks whether a token is a valid access token.
     * Verifies its signature, expiration, issuer, audience, type and revocation status.
     *
     * @param authorization the Authorization header containing the token to validate
     * @return TokenValidationResponseDTO containing validation results and token status
     * @throws InvalidTokenException if the Authorization header doesn't contain a Bearer token
     */
    @SecurityRequirements
    @Operation(summary = "Validate JWT token", 
               description = "Checks whether a token would be accepted as an access token: verifies its "
                           + "signature, expiration, issuer, audience, type and revocation status. An invalid "
                           + "token is reported with valid=false and the reason in the message.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token validation completed",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = TokenValidationResponseDTO.class))),
        @ApiResponse(responseCode = "400", ref = "#/components/responses/InvalidInput"),
        @ApiResponse(responseCode = "401", ref = "#/components/responses/InvalidToken"),
        @ApiResponse(responseCode = "429", ref = "#/components/responses/UnauthenticatedRateLimitExceeded")
    })
    @GetMapping("/validate")
    public TokenValidationResponseDTO validate(
            @RequestHeader("Authorization") 
            @Parameter(description = "Bearer JWT token to validate", required = true,
                      example = "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...")
            String authorization) {
        return authService.validateToken(authorization);
    }
}


