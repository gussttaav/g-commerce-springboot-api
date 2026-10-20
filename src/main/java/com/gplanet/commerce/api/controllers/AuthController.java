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

import com.gplanet.commerce.api.configs.app.docs.ApiDocs;
import com.gplanet.commerce.api.dtos.auth.LoginDTO;
import com.gplanet.commerce.api.dtos.auth.LoginResponseDTO;
import com.gplanet.commerce.api.dtos.auth.LogoutRequestDTO;
import com.gplanet.commerce.api.dtos.auth.TokenValidationResponseDTO;
import com.gplanet.commerce.api.exceptions.InvalidCredentialsException;
import com.gplanet.commerce.api.exceptions.InvalidTokenException;
import com.gplanet.commerce.api.exceptions.LogoutFailedException;
import com.gplanet.commerce.api.exceptions.TokenStorageException;
import com.gplanet.commerce.api.services.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@Tag(name = ApiDocs.TAG_AUTHENTICATION)
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Authenticates a user with their credentials.
     * 
     * @param loginDTO the login credentials containing email and password
     * @return LoginResponseDTO containing the authenticated user's information
     * @throws InvalidCredentialsException if the email is not registered or the password is incorrect
     */
    @SecurityRequirements
    @Operation(operationId = "login", summary = "Log in",
               description = "Signs in with an email and a password, and returns the user with a new access token "
                           + "and refresh token. Login attempts are rate-limited per IP address.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Signed in"),
        @ApiResponse(responseCode = "400", ref = ApiDocs.INVALID_INPUT),
        @ApiResponse(responseCode = "401", ref = ApiDocs.INVALID_CREDENTIALS),
        @ApiResponse(responseCode = "429", ref = ApiDocs.LOGIN_RATE_LIMIT_EXCEEDED)
    })
    @PostMapping("/login")
    public LoginResponseDTO login(
            @Valid @RequestBody LoginDTO loginDTO) {
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
    @SecurityRequirement(name = ApiDocs.REFRESH_TOKEN_AUTH)
    @Operation(operationId = "refreshTokens", summary = "Refresh tokens",
               description = "Exchanges a refresh token, sent as `Authorization: Bearer <refresh token>`, for a new "
                           + "access token and refresh token. Each refresh token works once: the used one is "
                           + "revoked, and sending it again gets a 401.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "New tokens issued"),
        @ApiResponse(responseCode = "401", ref = ApiDocs.INVALID_REFRESH_TOKEN),
        @ApiResponse(responseCode = "429", ref = ApiDocs.RATE_LIMIT_EXCEEDED)
    })
    @PostMapping("/refresh")
    public LoginResponseDTO refresh(
            @RequestHeader("Authorization") @Parameter(hidden = true) String authorization) {
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
    @Operation(operationId = "logout", summary = "Log out",
               description = "Revokes the access token that authenticates the request and the refresh token in "
                           + "the body, so neither works again. A refresh token that is invalid, expired or issued "
                           + "to another user is ignored.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Logged out; the response has no body"),
        @ApiResponse(responseCode = "400", ref = ApiDocs.INVALID_INPUT),
        @ApiResponse(responseCode = "401", ref = ApiDocs.AUTHENTICATION_REQUIRED),
        @ApiResponse(responseCode = "429", ref = ApiDocs.RATE_LIMIT_EXCEEDED)
    })
    @PostMapping("/logout")
    public void logout(
            @Valid @RequestBody LogoutRequestDTO logoutRequest, 
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
    @SecurityRequirement(name = ApiDocs.BEARER_AUTH)
    @Operation(operationId = "validateToken", summary = "Validate a token",
               description = "Checks whether the token in the `Authorization` header would be accepted as an "
                           + "access token: its signature, expiration, issuer, audience, type and revocation. An "
                           + "invalid token is not an error: the response reports `valid: false` and the reason.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "The result of the check",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = TokenValidationResponseDTO.class),
                examples = {
                    @ExampleObject(name = "valid", summary = "Valid token",
                        value = "{\"valid\": true, \"message\": \"Token is valid\"}"),
                    @ExampleObject(name = "revoked", summary = "Revoked by a logout",
                        value = "{\"valid\": false, \"message\": \"The token has been revoked\"}"),
                    @ExampleObject(name = "malformed", summary = "Malformed or forged token",
                        value = "{\"valid\": false, \"message\": \"Token is malformed or its signature is invalid\"}")
                })),
        @ApiResponse(responseCode = "401", ref = ApiDocs.BEARER_TOKEN_REQUIRED),
        @ApiResponse(responseCode = "429", ref = ApiDocs.RATE_LIMIT_EXCEEDED)
    })
    @GetMapping("/validate")
    public TokenValidationResponseDTO validate(
            @RequestHeader("Authorization") @Parameter(hidden = true) String authorization) {
        return authService.validateToken(authorization);
    }
}


