package com.gplanet.commerce.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;

import com.gplanet.commerce.api.configs.app.props.JwtProperties;
import com.gplanet.commerce.api.dtos.auth.LoginDTO;
import com.gplanet.commerce.api.dtos.auth.LoginResponseDTO;
import com.gplanet.commerce.api.dtos.auth.LogoutRequestDTO;
import com.gplanet.commerce.api.dtos.auth.TokenValidationResponseDTO;
import com.gplanet.commerce.api.dtos.user.UserMapper;
import com.gplanet.commerce.api.dtos.user.UserResponseDTO;
import com.gplanet.commerce.api.entities.User;
import com.gplanet.commerce.api.exceptions.InvalidCredentialsException;
import com.gplanet.commerce.api.exceptions.InvalidTokenException;
import com.gplanet.commerce.api.exceptions.LogoutFailedException;
import com.gplanet.commerce.api.repositories.UserRepository;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String TEST_EMAIL = "test@example.com";
    private static final String OTHER_EMAIL = "other@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String ACCESS_TOKEN = "access.token.123";
    private static final String REFRESH_TOKEN = "refresh.token.123";
    private static final String REFRESH_TOKEN_ID = "refresh-token-id";
    private static final String NEW_REFRESH_TOKEN_ID = "test-jwt-id";
    private static final String TOKEN_TYPE = "Bearer";
    private static final String TOKEN_TYPE_PREFIX = "Bearer ";
    private static final String REFRESH_TOKEN_PREFIX = "token:refresh:";
    private static final Long TOKEN_ACCESS_EXPIRATION_MS = 3600000L;
    private static final Long REFRESH_TOKEN_EXPIRATION_MS = 86400000L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private OAuth2TokenService oauth2TokenService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private JwtDecoder accessTokenDecoder;

    @Mock
    private JwtDecoder refreshTokenDecoder;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private AuthService authService;

    private User user;
    private UserDetails userDetails;
    private LoginDTO loginDTO;
    private UserResponseDTO userResponseDTO;

    @BeforeEach
    void setUp() {
        // Built by hand: @InjectMocks can't tell the two decoders apart
        authService = new AuthService(userRepository, authenticationManager, userMapper, customUserDetailsService,
                oauth2TokenService, tokenBlacklistService, accessTokenDecoder, refreshTokenDecoder,
                jwtProperties, stringRedisTemplate);

        user = new User();
        user.setId(1L);
        user.setEmail(TEST_EMAIL);
        user.setPassword("encodedPassword");
        user.setName("Test User");

        userDetails = org.springframework.security.core.userdetails.User.withUsername(TEST_EMAIL)
                .password("encodedPassword")
                .authorities("ROLE_USER")
                .build();

        loginDTO = new LoginDTO(TEST_EMAIL, TEST_PASSWORD);

        userResponseDTO = new UserResponseDTO(
                1L, "Test User", TEST_EMAIL, User.Role.USER, null);

        // Use lenient() for properties that might not be used in all tests
        lenient().when(jwtProperties.getAccessExpirationMs()).thenReturn(TOKEN_ACCESS_EXPIRATION_MS);
        lenient().when(jwtProperties.getRefreshExpirationMs()).thenReturn(REFRESH_TOKEN_EXPIRATION_MS);
        
        // Setup Redis template mock
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void loginValidCredentialsReturnsLoginResponse() throws Exception {
        // Arrange
        String validRefreshToken = createValidJWT(); // Use valid JWT for refresh token
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(
                UsernamePasswordAuthenticationToken.authenticated(userDetails, null, userDetails.getAuthorities()));
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));
        when(oauth2TokenService.generateAccessToken(userDetails)).thenReturn(ACCESS_TOKEN);
        when(oauth2TokenService.generateRefreshToken(userDetails)).thenReturn(validRefreshToken); // Use valid JWT
        when(userMapper.toUserResponseDTO(user)).thenReturn(userResponseDTO);
        // Mock Redis set operation for storeRefreshToken
        doNothing().when(valueOperations).set(
            anyString(),
            anyString(),
            anyLong(),
            eq(TimeUnit.MILLISECONDS)
        );

        // Act
        LoginResponseDTO result = authService.login(loginDTO);

        // Assert
        assertNotNull(result);
        assertEquals(userResponseDTO, result.user());
        assertEquals(ACCESS_TOKEN, result.token());
        assertEquals(validRefreshToken, result.refreshToken());
        assertEquals(TOKEN_TYPE, result.tokenType());
        verify(authenticationManager).authenticate(argThat(authentication ->
                TEST_EMAIL.equals(authentication.getPrincipal())
                        && TEST_PASSWORD.equals(authentication.getCredentials())));
        // The refresh token is stored by its ID
        verify(valueOperations).set(
            eq(REFRESH_TOKEN_PREFIX + NEW_REFRESH_TOKEN_ID),
            eq(TEST_EMAIL),
            anyLong(),
            eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    void loginWithBadCredentialsThrowsGenericException() {
        // Arrange - Thrown for an unknown email as well as for a wrong password
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        InvalidCredentialsException exception =
                assertThrows(InvalidCredentialsException.class, () -> authService.login(loginDTO));
        assertEquals("Invalid email or password", exception.getMessage());
        verifyNoInteractions(oauth2TokenService);
        verifyNoInteractions(valueOperations);
    }

    @Test
    void refreshValidTokenReturnsNewTokensAndInvalidatesUsedToken() throws Exception {
        // Arrange
        String newRefreshToken = createValidJWT();
        when(refreshTokenDecoder.decode(REFRESH_TOKEN)).thenReturn(refreshToken(TEST_EMAIL));
        when(stringRedisTemplate.delete(REFRESH_TOKEN_PREFIX + REFRESH_TOKEN_ID)).thenReturn(true);
        when(customUserDetailsService.loadUserByUsername(TEST_EMAIL)).thenReturn(userDetails);
        when(oauth2TokenService.generateAccessToken(userDetails)).thenReturn("newAccessToken");
        when(oauth2TokenService.generateRefreshToken(userDetails)).thenReturn(newRefreshToken);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));
        when(userMapper.toUserResponseDTO(user)).thenReturn(userResponseDTO);
        
        // Act
        LoginResponseDTO result = authService.refresh(TOKEN_TYPE_PREFIX + REFRESH_TOKEN);

        // Assert
        assertNotNull(result);
        assertEquals("newAccessToken", result.token());
        assertEquals(newRefreshToken, result.refreshToken());
        verify(stringRedisTemplate).delete(REFRESH_TOKEN_PREFIX + REFRESH_TOKEN_ID);
        verify(valueOperations).set(eq(REFRESH_TOKEN_PREFIX + NEW_REFRESH_TOKEN_ID), eq(TEST_EMAIL), anyLong(),
                eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void refreshUsedOrRevokedTokenThrowsException() {
        // Arrange - The token is valid, but no longer stored
        when(refreshTokenDecoder.decode(REFRESH_TOKEN)).thenReturn(refreshToken(TEST_EMAIL));
        when(stringRedisTemplate.delete(REFRESH_TOKEN_PREFIX + REFRESH_TOKEN_ID)).thenReturn(false);

        // Act & Assert
        assertThrows(InvalidTokenException.class, () -> authService.refresh(TOKEN_TYPE_PREFIX + REFRESH_TOKEN));
        verify(oauth2TokenService, never()).generateAccessToken(any());
        verify(oauth2TokenService, never()).generateRefreshToken(any());
    }

    @Test
    void refreshInvalidTokenThrowsException() {
        // Arrange
        when(refreshTokenDecoder.decode(REFRESH_TOKEN)).thenThrow(new BadJwtException("Invalid signature"));

        // Act & Assert
        assertThrows(InvalidTokenException.class, () -> authService.refresh(TOKEN_TYPE_PREFIX + REFRESH_TOKEN));
        verify(stringRedisTemplate, never()).delete(anyString());
    }

    @Test
    void refreshWithoutBearerTokenThrowsException() {
        // Act & Assert
        assertThrows(InvalidTokenException.class, () -> authService.refresh(REFRESH_TOKEN));
        verifyNoInteractions(refreshTokenDecoder);
    }

    @Test
    void logoutRevokesAccessAndRefreshTokens() {
        // Arrange
        Jwt accessToken = accessToken(TEST_EMAIL);
        when(refreshTokenDecoder.decode(REFRESH_TOKEN)).thenReturn(refreshToken(TEST_EMAIL));

        // Act
        authService.logout(new LogoutRequestDTO(REFRESH_TOKEN), accessToken);

        // Assert
        verify(tokenBlacklistService).blacklist(accessToken);
        verify(stringRedisTemplate).delete(REFRESH_TOKEN_PREFIX + REFRESH_TOKEN_ID);
    }

    @Test
    void logoutWithInvalidRefreshTokenRevokesAccessToken() {
        // Arrange
        Jwt accessToken = accessToken(TEST_EMAIL);
        when(refreshTokenDecoder.decode(REFRESH_TOKEN)).thenThrow(new BadJwtException("Jwt expired"));

        // Act
        authService.logout(new LogoutRequestDTO(REFRESH_TOKEN), accessToken);

        // Assert
        verify(tokenBlacklistService).blacklist(accessToken);
        verify(stringRedisTemplate, never()).delete(anyString());
    }

    @Test
    void logoutDoesNotRevokeRefreshTokenOfAnotherUser() {
        // Arrange
        Jwt accessToken = accessToken(TEST_EMAIL);
        when(refreshTokenDecoder.decode(REFRESH_TOKEN)).thenReturn(refreshToken(OTHER_EMAIL));

        // Act
        authService.logout(new LogoutRequestDTO(REFRESH_TOKEN), accessToken);

        // Assert
        verify(tokenBlacklistService).blacklist(accessToken);
        verify(stringRedisTemplate, never()).delete(anyString());
    }

    @Test
    void logoutWhenRedisFailsThrowsException() {
        // Arrange
        Jwt accessToken = accessToken(TEST_EMAIL);
        doThrow(new RedisConnectionFailureException("Redis is down")).when(tokenBlacklistService).blacklist(accessToken);

        // Act & Assert
        assertThrows(LogoutFailedException.class,
                () -> authService.logout(new LogoutRequestDTO(REFRESH_TOKEN), accessToken));
    }

    @Test
    void validateTokenValidTokenReturnsValidationResponse() {
        // Arrange
        when(accessTokenDecoder.decode(ACCESS_TOKEN)).thenReturn(accessToken(TEST_EMAIL));

        // Act
        TokenValidationResponseDTO result = authService.validateToken(TOKEN_TYPE_PREFIX + ACCESS_TOKEN);

        // Assert
        assertTrue(result.valid());
        assertEquals("Token is valid", result.message());
    }

    @Test
    void validateTokenRejectedByValidatorsReturnsReasons() {
        // Arrange
        when(accessTokenDecoder.decode(ACCESS_TOKEN)).thenThrow(new JwtValidationException("Invalid token", List.of(
                new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "The token has been revoked", null),
                new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "The token_type claim is not valid", null))));

        // Act
        TokenValidationResponseDTO result = authService.validateToken(TOKEN_TYPE_PREFIX + ACCESS_TOKEN);

        // Assert
        assertFalse(result.valid());
        assertEquals("The token has been revoked; The token_type claim is not valid", result.message());
    }

    @Test
    void validateTokenWithInvalidSignatureReturnsInvalidResponse() {
        // Arrange
        when(accessTokenDecoder.decode(ACCESS_TOKEN)).thenThrow(new BadJwtException("Invalid signature"));

        // Act
        TokenValidationResponseDTO result = authService.validateToken(TOKEN_TYPE_PREFIX + ACCESS_TOKEN);

        // Assert
        assertFalse(result.valid());
        assertEquals("Token is malformed or its signature is invalid", result.message());
    }

    @Test
    void validateTokenInvalidFormatThrowsException() {
        // Act & Assert - The Authorization header has no Bearer token
        assertThrows(InvalidTokenException.class,
                () -> authService.validateToken("InvalidTokenFormat"));
        verifyNoInteractions(accessTokenDecoder);
    }

    private static Jwt accessToken(String subject) {
        return token(ACCESS_TOKEN, subject, "access-token-id").build();
    }

    private static Jwt refreshToken(String subject) {
        return token(REFRESH_TOKEN, subject, REFRESH_TOKEN_ID)
                .claim(OAuth2TokenService.TOKEN_TYPE_CLAIM, OAuth2TokenService.REFRESH_TOKEN_TYPE)
                .build();
    }

    private static Jwt.Builder token(String tokenValue, String subject, String tokenId) {
        Instant now = Instant.now();
        return Jwt.withTokenValue(tokenValue)
                .header("alg", "RS256")
                .subject(subject)
                .jti(tokenId)
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofHours(1)));
    }

    /**
     * Helper method to create a valid JWT token for testing purposes.
     * This creates a properly formatted JWT that can be parsed by SignedJWT.parse().
     */
    private String createValidJWT() throws Exception {
        // Create a simple JWT with basic claims
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(TEST_EMAIL)
                .issuer("g-commerce-api")
                .audience("g-commerce-api")
                .expirationTime(new Date(System.currentTimeMillis() + TOKEN_ACCESS_EXPIRATION_MS))
                .notBeforeTime(new Date())
                .issueTime(new Date())
                .jwtID(NEW_REFRESH_TOKEN_ID)
                .build();

        // Create a signed JWT (using HMAC for simplicity in tests)
        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.HS256)
                        .type(JOSEObjectType.JWT)
                        .build(),
                claimsSet);

        // Sign with a test secret
        byte[] secret = "test-secret-key-that-is-long-enough".getBytes();
        signedJWT.sign(new MACSigner(secret));

        return signedJWT.serialize();
    }
}
