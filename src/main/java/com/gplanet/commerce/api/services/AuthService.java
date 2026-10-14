package com.gplanet.commerce.api.services;

import java.text.ParseException;
import java.util.concurrent.TimeUnit;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gplanet.commerce.api.configs.app.props.JwtProperties;
import com.gplanet.commerce.api.dtos.auth.LoginDTO;
import com.gplanet.commerce.api.dtos.auth.LoginResponseDTO;
import com.gplanet.commerce.api.dtos.auth.LogoutRequestDTO;
import com.gplanet.commerce.api.dtos.auth.TokenValidationResponseDTO;
import com.gplanet.commerce.api.dtos.user.UserMapper;
import com.gplanet.commerce.api.entities.User;
import com.gplanet.commerce.api.exceptions.InvalidPasswordException;
import com.gplanet.commerce.api.exceptions.InvalidTokenException;
import com.gplanet.commerce.api.exceptions.LogoutFailedException;
import com.gplanet.commerce.api.exceptions.ResourceNotFoundException;
import com.gplanet.commerce.api.exceptions.TokenStorageException;
import com.gplanet.commerce.api.repositories.UserRepository;
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

    private static final String REFRESH_TOKEN_PREFIX = "token:refresh:";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final CustomUserDetailsService userDetallesService;
    private final OAuth2TokenService oauth2TokenService;
    private final TokenBlacklistService tokenBlacklistService;
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
            UserDetails userDetails = userDetallesService.loadUserByUsername(loginDTO.email());

            User user = userRepository.findByEmail(loginDTO.email())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "No user exists with the provided email"
                ));

            if (!passwordEncoder.matches(loginDTO.password(), user.getPassword())) {
                throw new InvalidPasswordException("Contraseña incorrecta.");
            }

            String token = oauth2TokenService.generateAccessToken(userDetails);
            String refresh = oauth2TokenService.generateRefreshToken(userDetails);

            // Store new refresh token
            storeRefreshToken(refresh, loginDTO.email());

            log.info("Successful login for user: {}", loginDTO.email());
            return new LoginResponseDTO(
                userMapper.toUserResponseDTO(user),
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
     * @throws TokenStorageException if there's an error storing the new refresh token
     */
    @Transactional
    public LoginResponseDTO refresh(String authorizationHeader) {

        try {
            String token = extractToken(authorizationHeader);
            
            // Validate refresh token
            validateRefreshToken(token);
            
            String username = SignedJWT.parse(token).getJWTClaimsSet().getSubject();
            UserDetails userDetails = userDetallesService.loadUserByUsername(username);
            
            String access = oauth2TokenService.generateAccessToken(userDetails);
            String refresh = oauth2TokenService.generateRefreshToken(userDetails);
            
            // Store new refresh token
            storeRefreshToken(refresh, username);

            User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new ResourceNotFoundException(
                        "No user found with email " + username
                    ));
            
            return new LoginResponseDTO(
                userMapper.toUserResponseDTO(user),
                access,
                "Bearer",
                jwtProperties.getAccessExpirationMs() / 1000,
                refresh,
                jwtProperties.getRefreshExpirationMs() / 1000
            );
        } catch (ParseException e) {
            log.error("Failed to parse refresh token", e);
            throw new InvalidTokenException("Invalid token");
        }
    }

    /**
     * Logs out a session by revoking its tokens: the access token that authenticated the request is blacklisted
     * and the refresh token is invalidated.
     * 
     * @param logoutRequest the logout request containing the session's refresh token
     * @param accessToken the access token that authenticated the request
     * @throws LogoutFailedException if the tokens cannot be revoked
     */
    @Transactional
    public void logout(LogoutRequestDTO logoutRequest, Jwt accessToken) {
        try {
            tokenBlacklistService.blacklist(accessToken);
            // Refresh only accepts stored refresh tokens
            stringRedisTemplate.delete(REFRESH_TOKEN_PREFIX + logoutRequest.refreshToken());
        } catch (DataAccessException e) {
            log.error("Error during logout", e);
            throw new LogoutFailedException("Logout failed");
        }
        log.info("User {} logged out successfully. Tokens revoked.", accessToken.getSubject());
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
            
            // Parse token to validate structure
            String tokenId = SignedJWT.parse(token).getJWTClaimsSet().getJWTID();

            // Check if token is blacklisted
            if (tokenBlacklistService.isBlacklisted(tokenId)) {
                return new TokenValidationResponseDTO(false, "Token is blacklisted");
            }
            return new TokenValidationResponseDTO(true, "Token is valid");
            
        } catch (ParseException ex) {
            throw new InvalidTokenException("Invalid token");
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
     * Validates that a refresh token is stored, which means it was issued by this API and has not expired
     * or been revoked.
     * 
     * @param refreshToken the refresh token to validate
     * @throws InvalidTokenException if refresh token is not found or expired
     */
    private void validateRefreshToken(String refreshToken) {
        String username = stringRedisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + refreshToken);
        if (username == null) {
            throw new InvalidTokenException("Refresh token not found or expired");
        }
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
