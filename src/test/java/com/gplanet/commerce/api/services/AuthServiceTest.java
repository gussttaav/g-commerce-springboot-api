package com.gplanet.commerce.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.gplanet.commerce.api.configs.app.props.JwtProperties;
import com.gplanet.commerce.api.dtos.auth.LoginDTO;
import com.gplanet.commerce.api.dtos.auth.LoginResponseDTO;
import com.gplanet.commerce.api.dtos.auth.LogoutRequestDTO;
import com.gplanet.commerce.api.dtos.auth.TokenValidationResponseDTO;
import com.gplanet.commerce.api.dtos.user.UserMapper;
import com.gplanet.commerce.api.dtos.user.UserResponseDTO;
import com.gplanet.commerce.api.entities.User;
import com.gplanet.commerce.api.exceptions.InvalidPasswordException;
import com.gplanet.commerce.api.exceptions.InvalidTokenException;
import com.gplanet.commerce.api.exceptions.ResourceNotFoundException;
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
    private static final String TEST_PASSWORD = "password123";
    private static final String ACCESS_TOKEN = "access.token.123";
    private static final String TOKEN_TYPE = "Bearer";
    private static final String TOKEN_TYPE_PREFIX = "Bearer ";
    private static final String BLACKLIST_PREFIX = "token:blacklist:";
    private static final String REFRESH_TOKEN_PREFIX = "token:refresh:";
    private static final Long TOKEN_ACCESS_EXPIRATION_MS = 3600000L;
    private static final Long REFRESH_TOKEN_EXPIRATION_MS = 86400000L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private OAuth2TokenService oauth2TokenService;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthService authService;

    private User user;
    private UserDetails userDetails;
    private LoginDTO loginDTO;
    private UserResponseDTO userResponseDTO;

    @BeforeEach
    void setUp() {
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
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));
        when(customUserDetailsService.loadUserByUsername(TEST_EMAIL)).thenReturn(userDetails);
        when(passwordEncoder.matches(TEST_PASSWORD, user.getPassword())).thenReturn(true);
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
        verify(userRepository).findByEmail(TEST_EMAIL);
        verify(passwordEncoder).matches(TEST_PASSWORD, user.getPassword());
        verify(valueOperations).set(
            eq(REFRESH_TOKEN_PREFIX + validRefreshToken),
            eq(TEST_EMAIL),
            anyLong(),
            eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    void loginInvalidPasswordThrowsException() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));
        when(customUserDetailsService.loadUserByUsername(TEST_EMAIL)).thenReturn(userDetails);
        when(passwordEncoder.matches(TEST_PASSWORD, user.getPassword())).thenReturn(false);

        // Act & Assert
        assertThrows(InvalidPasswordException.class, () -> authService.login(loginDTO));
        verify(userRepository).findByEmail(TEST_EMAIL);
        verify(passwordEncoder).matches(TEST_PASSWORD, user.getPassword());
    }

    @Test
    void loginUserNotFoundThrowsException() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> authService.login(loginDTO));
        verify(userRepository).findByEmail(TEST_EMAIL);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void refreshValidTokenReturnsNewTokens() throws Exception {
        // Arrange - Create a valid JWT token for testing
        String validRefreshToken = createValidJWT();
        String newRefreshToken = createValidJWT();
        
        when(valueOperations.get(REFRESH_TOKEN_PREFIX + validRefreshToken)).thenReturn(TEST_EMAIL);
        when(stringRedisTemplate.hasKey(BLACKLIST_PREFIX + validRefreshToken)).thenReturn(false);
        when(customUserDetailsService.loadUserByUsername(TEST_EMAIL)).thenReturn(userDetails);
        when(oauth2TokenService.generateAccessToken(userDetails)).thenReturn("newAccessToken");
        when(oauth2TokenService.generateRefreshToken(userDetails)).thenReturn(newRefreshToken);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));
        when(userMapper.toUserResponseDTO(user)).thenReturn(userResponseDTO);
        
        // Mock the Redis set operation for storing the new refresh token
        doNothing().when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        // Act
        LoginResponseDTO result = authService.refresh(TOKEN_TYPE_PREFIX + validRefreshToken);

        // Assert
        assertNotNull(result);
        assertEquals("newAccessToken", result.token());
        assertEquals(newRefreshToken, result.refreshToken());
        verify(valueOperations).get(REFRESH_TOKEN_PREFIX + validRefreshToken);
        verify(stringRedisTemplate).hasKey(BLACKLIST_PREFIX + validRefreshToken);
        verify(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void refreshInvalidTokenThrowsException() {
        // Arrange
        when(valueOperations.get(anyString())).thenReturn(null);

        // Act & Assert
        assertThrows(InvalidTokenException.class,
                () -> authService.refresh("Bearer invalidToken"));
    }

    @Test
    void logoutValidTokensBlacklistsTokens() throws Exception {
        // Arrange
        String validRefreshToken = createValidJWT();
        LogoutRequestDTO logoutRequest = new LogoutRequestDTO(validRefreshToken);
        
        // Create valid JWT tokens for testing
        String validAccessToken = createValidJWT();
        
        // Mock Redis operations
        when(stringRedisTemplate.delete(REFRESH_TOKEN_PREFIX + validRefreshToken)).thenReturn(true);
        doNothing().when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        // Act
        authService.logout(logoutRequest, TOKEN_TYPE_PREFIX + validAccessToken);

        // Assert
        verify(stringRedisTemplate).delete(REFRESH_TOKEN_PREFIX + validRefreshToken);
        // Verify that tokens are blacklisted (2 calls: one for access token, one for refresh token)
        verify(valueOperations, times(2)).set(anyString(), eq("blacklisted"), anyLong(), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void validateTokenValidTokenReturnsValidationResponse() throws Exception {
        // Arrange
        String validToken = createValidJWT();
        when(stringRedisTemplate.hasKey(BLACKLIST_PREFIX + validToken)).thenReturn(false);

        // Act
        TokenValidationResponseDTO result = authService.validateToken(TOKEN_TYPE_PREFIX + validToken);

        // Assert
        assertNotNull(result);
        assertEquals(true, result.valid());
        assertEquals("Token is valid", result.message());
        verify(stringRedisTemplate).hasKey(BLACKLIST_PREFIX + validToken);
    }

    @Test
    void validateTokenBlacklistedTokenReturnsInvalidResponse() throws Exception {
        // Arrange
        String validToken = createValidJWT();
        when(stringRedisTemplate.hasKey(BLACKLIST_PREFIX + validToken)).thenReturn(true);

        // Act
        TokenValidationResponseDTO result = authService.validateToken(TOKEN_TYPE_PREFIX + validToken);

        // Assert
        assertNotNull(result);
        assertEquals(false, result.valid());
        assertEquals("Token is blacklisted", result.message());
        verify(stringRedisTemplate).hasKey(BLACKLIST_PREFIX + validToken);
    }

    @Test
    void validateTokenInvalidFormatThrowsException() {
        // Act & Assert - The service throws UsernameNotFoundException for invalid token format
        assertThrows(InvalidTokenException.class,
                () -> authService.validateToken("InvalidTokenFormat"));
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
                .jwtID("test-jwt-id")
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
