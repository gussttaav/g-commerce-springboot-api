package com.gplanet.commerce.api.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gplanet.commerce.api.configs.app.props.JwtProperties;
import com.gplanet.commerce.api.configs.security.SecurityConfig;
import com.gplanet.commerce.api.dtos.auth.LoginDTO;
import com.gplanet.commerce.api.dtos.auth.LoginResponseDTO;
import com.gplanet.commerce.api.dtos.auth.LogoutRequestDTO;
import com.gplanet.commerce.api.dtos.auth.TokenValidationResponseDTO;
import com.gplanet.commerce.api.dtos.user.UserMapper;
import com.gplanet.commerce.api.entities.User;
import com.gplanet.commerce.api.exceptions.InvalidCredentialsException;
import com.gplanet.commerce.api.exceptions.InvalidTokenException;
import com.gplanet.commerce.api.exceptions.LogoutFailedException;
import com.gplanet.commerce.api.exceptions.TokenStorageException;
import com.gplanet.commerce.api.repositories.UserRepository;
import com.nimbusds.jwt.SignedJWT;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service class that handles authentication-related operations including
 * user login, token refresh, logout, and token validation.
 * <p>
 * Tokens identify the user by ID. Refresh tokens are stored in Redis by their {@code jti} claim, with the ID of
 * their user, until they expire, are used or are revoked. Access tokens are not stored; they are revoked by
 * blacklisting them with {@link TokenBlacklistService}.
 * </p>
 * 
 * @author Gustavo
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String REFRESH_TOKEN_PREFIX = "token:refresh:";
    private static final String INVALID_CREDENTIALS_MSG = "Invalid email or password";
    private static final long SCAN_BATCH_SIZE = 500;

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;
    private final OAuth2TokenService oauth2TokenService;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtDecoder accessTokenDecoder;
    @Qualifier(SecurityConfig.REFRESH_TOKEN_DECODER)
    private final JwtDecoder refreshTokenDecoder;
    private final JwtProperties jwtProperties;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Authenticates a user using email and password.
     * 
     * @param loginDTO Data transfer object containing login credentials
     * @return LoginResponseDTO containing the authenticated user's information and JWT token
     * @throws InvalidCredentialsException if the email is not registered or the password is incorrect
     */
    public LoginResponseDTO login(LoginDTO loginDTO) {
        log.info("Login attempt for user: {}", loginDTO.email());
        UserDetails userDetails = authenticate(loginDTO);
        // Only fails if the user was deleted since authenticating
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException(INVALID_CREDENTIALS_MSG));

        LoginResponseDTO response = issueTokens(user);
        log.info("Successful login for user: {}", loginDTO.email());
        return response;
    }

    /**
     * Refreshes an access token using a valid refresh token.
     * Generates new access and refresh tokens and invalidates the used refresh token, so each refresh token
     * can only be used once.
     * 
     * @param authorizationHeader the Authorization header containing the refresh token
     * @return LoginResponseDTO containing the new access and refresh tokens
     * @throws InvalidTokenException if the refresh token is invalid, expired, already used or revoked, or its user
     *         no longer exists
     * @throws TokenStorageException if there's an error storing the new refresh token
     */
    @Transactional
    public LoginResponseDTO refresh(String authorizationHeader) {
        Jwt refreshToken = decodeRefreshToken(extractToken(authorizationHeader));

        // Deleting is atomic, so if the same token is sent again, even concurrently, only one request succeeds
        if (!Boolean.TRUE.equals(stringRedisTemplate.delete(REFRESH_TOKEN_PREFIX + refreshToken.getId()))) {
            log.warn("Rejected a refresh token that was already used or revoked, user: {}",
                    refreshToken.getSubject());
            throw new InvalidTokenException("Refresh token has already been used or revoked");
        }

        // The decoder only accepts tokens whose subject is a user ID
        User user = userRepository.findById(Long.valueOf(refreshToken.getSubject()))
                .orElseThrow(() -> new InvalidTokenException("The user of the refresh token no longer exists"));

        return issueTokens(user);
    }

    /**
     * Logs out a session by revoking its tokens: the access token that authenticated the request is blacklisted
     * and the refresh token is invalidated.
     * <p>
     * As in token revocation (RFC 7009), a refresh token that is invalid, expired or issued to another user
     * doesn't make the logout fail: it can't be used by the caller, so there is nothing to revoke.
     * </p>
     * 
     * @param logoutRequest the logout request containing the session's refresh token
     * @param accessToken the access token that authenticated the request
     * @throws LogoutFailedException if the tokens cannot be revoked
     */
    @Transactional
    public void logout(LogoutRequestDTO logoutRequest, Jwt accessToken) {
        try {
            tokenBlacklistService.blacklist(accessToken);
            revokeRefreshToken(logoutRequest.refreshToken(), accessToken.getSubject());
        } catch (DataAccessException e) {
            log.error("Error during logout", e);
            throw new LogoutFailedException("Logout failed");
        }
        log.info("User {} logged out successfully. Tokens revoked.", accessToken.getSubject());
    }

    /**
     * Checks whether a token is a valid access token, with the same checks the API applies to every request:
     * signature, expiration, issuer, audience, token type and revocation.
     * 
     * @param authorizationHeader the Authorization header containing the token to validate
     * @return TokenValidationResponseDTO containing validation results and token status
     * @throws InvalidTokenException if the Authorization header doesn't contain a Bearer token
     */
    public TokenValidationResponseDTO validateToken(String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        try {
            accessTokenDecoder.decode(token);
            return new TokenValidationResponseDTO(true, "Token is valid");
        } catch (JwtValidationException e) {
            String reasons = e.getErrors().stream()
                    .map(OAuth2Error::getDescription)
                    .collect(Collectors.joining("; "));
            return new TokenValidationResponseDTO(false, reasons);
        } catch (BadJwtException e) {
            return new TokenValidationResponseDTO(false, "Token is malformed or its signature is invalid");
        }
    }

    /**
     * Revokes every stored refresh token, so all sessions end when their access tokens expire. Access tokens
     * are not stored, so they stay valid until then.
     *
     * @return the number of refresh tokens revoked
     */
    public long revokeAllRefreshTokens() {
        ScanOptions options = ScanOptions.scanOptions()
                .match(REFRESH_TOKEN_PREFIX + "*")
                .count(SCAN_BATCH_SIZE)
                .build();
        List<String> keys = new ArrayList<>();
        try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
            cursor.forEachRemaining(keys::add);
        }
        long deleted = 0;
        if (!keys.isEmpty()) {
            deleted = Optional.ofNullable(stringRedisTemplate.delete(keys)).orElse(0L);
        }
        log.info("Revoked {} refresh tokens", deleted);
        return deleted;
    }

    /**
     * Checks the email and password of a login.
     * <p>
     * An unknown email and a wrong password are reported with the same error, so the response doesn't reveal
     * which emails are registered.
     * </p>
     *
     * @param loginDTO the login credentials
     * @return the security details of the authenticated user
     * @throws InvalidCredentialsException if the email is not registered or the password is incorrect
     */
    private UserDetails authenticate(LoginDTO loginDTO) {
        try {
            return (UserDetails) authenticationManager.authenticate(UsernamePasswordAuthenticationToken
                    .unauthenticated(loginDTO.email(), loginDTO.password())).getPrincipal();
        } catch (BadCredentialsException e) {
            log.warn("Login failed for user: {} - unknown email or wrong password", loginDTO.email());
            throw new InvalidCredentialsException(INVALID_CREDENTIALS_MSG);
        }
    }

    /**
     * Generates a new access and refresh token pair for the user and stores the refresh token.
     *
     * @param user the user the tokens are issued to
     * @return LoginResponseDTO containing the user's information and the new tokens
     * @throws TokenStorageException if the refresh token cannot be stored
     */
    private LoginResponseDTO issueTokens(User user) {
        String access = oauth2TokenService.generateAccessToken(user);
        String refresh = oauth2TokenService.generateRefreshToken(user);

        storeRefreshToken(refresh, user.getId());

        return new LoginResponseDTO(
            userMapper.toUserResponseDTO(user),
            access,
            "Bearer",
            jwtProperties.getAccessExpirationMs() / 1000,
            refresh,
            jwtProperties.getRefreshExpirationMs() / 1000
        );
    }

    /**
     * Stores refresh token in Redis with expiration.
     * 
     * @param refreshToken the refresh token to store
     * @param userId the ID of the user the token was issued to
     * @throws TokenStorageException if token storage fails
     */
    private void storeRefreshToken(String refreshToken, Long userId) {
        try {
            SignedJWT jwt = SignedJWT.parse(refreshToken);
            long expirationMs = jwt.getJWTClaimsSet().getExpirationTime().getTime() - System.currentTimeMillis();
            
            stringRedisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + jwt.getJWTClaimsSet().getJWTID(),
                userId.toString(),
                expirationMs,
                TimeUnit.MILLISECONDS
            );
        } catch (Exception e) {
            log.error("Failed to store refresh token", e);
            throw new TokenStorageException("Failed to store refresh token");
        }
    }

    /**
     * Decodes a refresh token, verifying its signature, timestamps, issuer, audience and type.
     * 
     * @param token the refresh token
     * @return the decoded refresh token
     * @throws InvalidTokenException if the token is not a valid refresh token
     */
    private Jwt decodeRefreshToken(String token) {
        try {
            return refreshTokenDecoder.decode(token);
        } catch (BadJwtException e) {
            log.warn("Rejected refresh token: {}", e.getMessage());
            throw new InvalidTokenException("Invalid refresh token");
        }
    }

    /**
     * Invalidates a refresh token if it is a valid refresh token of the user.
     *
     * @param token the refresh token
     * @param userId the ID of the user whose session is being closed
     */
    private void revokeRefreshToken(String token, String userId) {
        Jwt refreshToken;
        try {
            refreshToken = refreshTokenDecoder.decode(token);
        } catch (BadJwtException e) {
            log.info("Logout of user {} with an invalid refresh token: {}", userId, e.getMessage());
            return;
        }

        if (userId.equals(refreshToken.getSubject())) {
            stringRedisTemplate.delete(REFRESH_TOKEN_PREFIX + refreshToken.getId());
        } else {
            log.warn("User {} sent a refresh token of another user on logout", userId);
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
