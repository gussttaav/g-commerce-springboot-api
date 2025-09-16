package com.gplanet.commerce.api.services;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import com.gplanet.commerce.api.configs.app.props.JwtProperties;
import com.nimbusds.jose.jwk.RSAKey;

@ExtendWith(MockitoExtension.class)
class OAuth2TokenServiceTest {

    private static final String TEST_USERNAME = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final Long ACCESS_EXPIRATION_MS = 3600000L;
    private static final Long REFRESH_EXPIRATION_MS = 86400000L;
    private static final int RSA_KEY_SIZE = 2048;

    @Mock
    private KeyStoreService keyStoreService;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private OAuth2TokenService oauth2TokenService;

    private UserDetails userDetails;
    private RSAKey rsaKey;

    @BeforeEach
    void setUp() throws Exception {
        userDetails = User.withUsername(TEST_USERNAME)
                .password(TEST_PASSWORD)
                .authorities("ROLE_USER")
                .build();

        // Create a proper RSA key with sufficient size
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(RSA_KEY_SIZE);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        
        rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey(keyPair.getPrivate())
                .keyID("test-key-id")
                .build();

        // Use lenient to avoid unnecessary stubbing exceptions
        lenient().when(jwtProperties.getAccessExpirationMs()).thenReturn(ACCESS_EXPIRATION_MS);
        lenient().when(jwtProperties.getRefreshExpirationMs()).thenReturn(REFRESH_EXPIRATION_MS);
        lenient().when(jwtProperties.getIssuer()).thenReturn("test-issuer");
    }

    @Test
    void generateAccessTokenValidUserDetailsReturnsToken() throws Exception {
        // Arrange
        when(keyStoreService.getOrCreateActiveKeyPair()).thenReturn(rsaKey);

        // Act
        String token = oauth2TokenService.generateAccessToken(userDetails);

        // Assert
        assertNotNull(token);
    }

    @Test
    void generateRefreshTokenValidUserDetailsReturnsToken() throws Exception {
        // Arrange
        when(keyStoreService.getOrCreateActiveKeyPair()).thenReturn(rsaKey);

        // Act
        String token = oauth2TokenService.generateRefreshToken(userDetails);

        // Assert
        assertNotNull(token);
    }

    @Test
    void generateAccessTokenKeyStoreExceptionThrowsIllegalStateException() throws Exception {
        // Arrange
        when(keyStoreService.getOrCreateActiveKeyPair()).thenThrow(new RuntimeException("Key store error"));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> 
            oauth2TokenService.generateAccessToken(userDetails));
    }

    @Test
    void generateRefreshTokenKeyStoreExceptionThrowsIllegalStateException() throws Exception {
        // Arrange
        when(keyStoreService.getOrCreateActiveKeyPair()).thenThrow(new RuntimeException("Key store error"));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> 
            oauth2TokenService.generateRefreshToken(userDetails));
    }

    @Test
    void generateAccessTokenUserWithNoAuthoritiesReturnsToken() throws Exception {
        // Arrange
        UserDetails userNoAuthorities = User.withUsername(TEST_USERNAME)
                .password(TEST_PASSWORD)
                .authorities(Collections.emptyList())
                .build();
        
        when(keyStoreService.getOrCreateActiveKeyPair()).thenReturn(rsaKey);

        // Act
        String token = oauth2TokenService.generateAccessToken(userNoAuthorities);

        // Assert
        assertNotNull(token);
    }
}
