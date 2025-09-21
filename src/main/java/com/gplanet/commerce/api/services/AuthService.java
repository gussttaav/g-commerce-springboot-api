package com.gplanet.commerce.api.services;

import java.text.ParseException;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gplanet.commerce.api.configs.app.props.JwtProperties;
import com.gplanet.commerce.api.dtos.auth.LoginDTO;
import com.gplanet.commerce.api.dtos.auth.LoginResponseDTO;
import com.gplanet.commerce.api.dtos.auth.LogoutRequestDTO;
import com.gplanet.commerce.api.dtos.auth.TokenValidationResponseDTO;
import com.gplanet.commerce.api.dtos.usuario.UsuarioMapper;
import com.gplanet.commerce.api.entities.Usuario;
import com.gplanet.commerce.api.exceptions.InvalidPasswordException;
import com.gplanet.commerce.api.exceptions.InvalidTokenException;
import com.gplanet.commerce.api.exceptions.LogoutFailedException;
import com.gplanet.commerce.api.exceptions.ResourceNotFoundException;
import com.gplanet.commerce.api.exceptions.TokenBlacklistException;
import com.gplanet.commerce.api.exceptions.TokenStorageException;
import com.gplanet.commerce.api.repositories.UsuarioRepository;
import com.nimbusds.jwt.SignedJWT;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service class that handles authentication-related operations including
 * user login, token refresh, logout, and token validation.
 * 
 * @author Gustavo
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String BLACKLIST_PREFIX = "token:blacklist:";
    private static final String REFRESH_TOKEN_PREFIX = "token:refresh:";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;
    private final UsuarioDetallesService usuarioDetallesService;
    private final OAuth2TokenService oauth2TokenService;
    private final JwtProperties jwtProperties;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Authenticates a user using email and password.
     * 
     * @param loginDTO Data transfer object containing login credentials
     * @return LoginResponseDTO containing the authenticated user's information and JWT token
     * @throws UsernameNotFoundException if user is not found
     * @throws InvalidPasswordException if password is incorrect
     */
    public LoginResponseDTO login(LoginDTO loginDTO) {
        log.info("Login attempt for user: {}", loginDTO.email());
        try {
            UserDetails userDetails = usuarioDetallesService.loadUserByUsername(loginDTO.email());

            Usuario usuario = usuarioRepository.findByEmail(loginDTO.email())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "No existe ningún usuario con el email proporcionado"
                ));

            if (!passwordEncoder.matches(loginDTO.password(), usuario.getPassword())) {
                throw new InvalidPasswordException("Contraseña incorrecta.");
            }

            String token = oauth2TokenService.generateAccessToken(userDetails);
            String refresh = oauth2TokenService.generateRefreshToken(userDetails);

            log.info("Successful login for user: {}", loginDTO.email());
            return new LoginResponseDTO(
                usuarioMapper.toUsuarioResponseDTO(usuario),
                token,
                "Bearer",
                jwtProperties.getAccessExpirationMs() / 1000,
                refresh,
                jwtProperties.getRefreshExpirationMs() / 1000
            );
            
        } catch (UsernameNotFoundException | InvalidPasswordException e) {
            log.warn("Login failed for user: {} - {}", loginDTO.email(), e.getMessage());
            throw e;
        }
    }

    /**
     * Refreshes an access token using a valid refresh token.
     * Generates new access and refresh tokens while invalidating the old refresh token.
     * 
     * @param authorizationHeader the Authorization header containing the refresh token
     * @return LoginResponseDTO containing the new access and refresh tokens
     * @throws UsernameNotFoundException if user is not found
     * @throws InvalidTokenException if the refresh token is invalid or expired
     * @throws TokenBlacklistException if refresh token is blacklisted
     * @throws TokenStorageException if there's an error storing the new refresh token
     */
    @Transactional
    public LoginResponseDTO refresh(String authorizationHeader) {

        try {
            String token = extractToken(authorizationHeader);
            
            // Validate refresh token
            validateRefreshToken(token);
            
            String username = SignedJWT.parse(token).getJWTClaimsSet().getSubject();
            UserDetails userDetails = usuarioDetallesService.loadUserByUsername(username);
            
            String access = oauth2TokenService.generateAccessToken(userDetails);
            String refresh = oauth2TokenService.generateRefreshToken(userDetails);
            
            // Store new refresh token
            storeRefreshToken(refresh, username);

            Usuario usuario = usuarioRepository.findByEmail(username)
                    .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe ningún usuario con el email proporcionado"
                    ));
            
            return new LoginResponseDTO(
                usuarioMapper.toUsuarioResponseDTO(usuario),
                access,
                "Bearer",
                jwtProperties.getAccessExpirationMs() / 1000,
                refresh,
                jwtProperties.getRefreshExpirationMs() / 1000
            );
        } catch (ParseException e) {
            log.error("Failed to parse refresh token", e);
            throw new InvalidTokenException("Token inválido");
        }
    }

    /**
     * Logs out a user by blacklisting their tokens.
     * Invalidates both access and refresh tokens to prevent further use.
     * 
     * @param logoutRequest the logout request containing refresh token information
     * @param authorizationHeader the Authorization header containing the access token
     * @throws InvalidTokenException if the provided token is invalid
     * @throws LogoutFailedException if logout operations fail
     */
    @Transactional
    public void logout(LogoutRequestDTO logoutRequest, String authorizationHeader) {
        String accessToken = extractToken(authorizationHeader);
        String refreshToken = logoutRequest.refreshToken();

        try {
            // Blacklist access token
            blacklistToken(accessToken);
            
            // Blacklist refresh token
            blacklistToken(refreshToken);
            
            // Remove refresh token from valid tokens
            stringRedisTemplate.delete(REFRESH_TOKEN_PREFIX + refreshToken);
            
            log.info("User logged out successfully. Tokens blacklisted.");
        } catch (Exception e) {
            log.error("Error during logout", e);
            throw new LogoutFailedException("Logout failed");
        }
    }

    /**
     * Validates the structure and status of a JWT token.
     * Checks if the token is properly formatted and not blacklisted.
     * 
     * @param authorizationHeader the Authorization header containing the token to validate
     * @return TokenValidationResponseDTO containing validation results and token status
     * @throws InvalidTokenException if the token format is invalid
     */
    public TokenValidationResponseDTO validateToken(String authorizationHeader) {
        try {
            String token = extractToken(authorizationHeader);
            
            // Check if token is blacklisted
            if (isTokenBlacklisted(token)) {
                return new TokenValidationResponseDTO(false, "Token is blacklisted");
            }
            
            // Parse token to validate structure
            SignedJWT.parse(token);
            return new TokenValidationResponseDTO(true, "Token is valid");
            
        } catch (ParseException ex) {
            throw new InvalidTokenException("Token inválido");
        }
    }

    /**
     * Stores refresh token in Redis with expiration.
     * 
     * @param refreshToken the refresh token to store
     * @param username the username associated with the token
     * @throws TokenStorageException if token storage fails
     */
    private void storeRefreshToken(String refreshToken, String username) {
        try {
            SignedJWT jwt = SignedJWT.parse(refreshToken);
            long expirationMs = jwt.getJWTClaimsSet().getExpirationTime().getTime() - System.currentTimeMillis();
            
            stringRedisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + refreshToken,
                username,
                expirationMs,
                TimeUnit.MILLISECONDS
            );
        } catch (Exception e) {
            log.error("Failed to store refresh token", e);
            throw new TokenStorageException("Failed to store refresh token");
        }
    }

    /**
     * Validates that a refresh token exists and is not blacklisted.
     * 
     * @param refreshToken the refresh token to validate
     * @throws InvalidTokenException if refresh token is not found or expired
     * @throws TokenBlacklistException if refresh token is blacklisted
     */
    private void validateRefreshToken(String refreshToken) {
        // Check if refresh token exists in valid tokens
        String username = stringRedisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + refreshToken);
        if (username == null) {
            throw new InvalidTokenException("Refresh token not found or expired");
        }
        
        // Check if refresh token is blacklisted
        if (isTokenBlacklisted(refreshToken)) {
            throw new TokenBlacklistException("Refresh token is blacklisted");
        }
    }

    /**
     * Blacklists a token by storing it in Redis with TTL.
     * 
     * @param token the token to blacklist
     * @throws TokenBlacklistException if token blacklisting fails
     */
    private void blacklistToken(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            long expirationMs = jwt.getJWTClaimsSet().getExpirationTime().getTime() - System.currentTimeMillis();
            
            if (expirationMs > 0) {
                stringRedisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + token,
                    "blacklisted",
                    expirationMs,
                    TimeUnit.MILLISECONDS
                );
            }
        } catch (Exception e) {
            log.error("Failed to blacklist token", e);
            throw new TokenBlacklistException("Failed to blacklist token");
        }
    }

    /**
     * Checks if a token is blacklisted.
     * 
     * @param token the token to check
     * @return boolean indicating if the token is blacklisted
     */
    private boolean isTokenBlacklisted(String token) {
        return stringRedisTemplate.hasKey(BLACKLIST_PREFIX + token);
    }

    /**
     * Extracts token from Authorization header.
     * 
     * @param authorization the Authorization header value
     * @return the extracted token
     * @throws InvalidTokenException if Authorization header is missing or malformed
     */
    private String extractToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new InvalidTokenException("Missing or invalid Bearer token");
        }
        return authorization.substring("Bearer ".length());
    }
}
