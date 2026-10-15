package com.gplanet.commerce.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gplanet.commerce.api.configs.app.props.JwtProperties;
import com.gplanet.commerce.api.entities.User;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

@ExtendWith(MockitoExtension.class)
class OAuth2TokenServiceTest {

    private static final Long TEST_USER_ID = 42L;
    private static final String TEST_EMAIL = "test@example.com";
    private static final Long ACCESS_EXPIRATION_MS = 3600000L;
    private static final Long REFRESH_EXPIRATION_MS = 86400000L;
    private static final int RSA_KEY_SIZE = 2048;

    @Mock
    private KeyStoreService keyStoreService;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private OAuth2TokenService oauth2TokenService;

    private User user;
    private RSAKey rsaKey;

    @BeforeEach
    void setUp() throws Exception {
        user = new User();
        user.setId(TEST_USER_ID);
        user.setEmail(TEST_EMAIL);
        user.setRole(User.Role.USER);

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
        lenient().when(jwtProperties.getAudience()).thenReturn("test-audience");
    }

    @Test
    void generateAccessTokenUsesConfiguredIssuerAudienceAndKeyId() throws Exception {
        // Arrange
        when(keyStoreService.getOrCreateActiveKeyPair()).thenReturn(rsaKey);

        // Act
        SignedJWT token = SignedJWT.parse(oauth2TokenService.generateAccessToken(user));

        // Assert
        JWTClaimsSet claims = token.getJWTClaimsSet();
        assertThat(claims.getIssuer()).isEqualTo("test-issuer");
        assertThat(claims.getAudience()).containsExactly("test-audience");
        assertThat(token.getHeader().getKeyID()).isEqualTo("test-key-id");
    }

    @Test
    void generateAccessTokenIdentifiesTheUserById() throws Exception {
        // Arrange
        when(keyStoreService.getOrCreateActiveKeyPair()).thenReturn(rsaKey);

        // Act
        JWTClaimsSet claims = SignedJWT.parse(oauth2TokenService.generateAccessToken(user)).getJWTClaimsSet();

        // Assert - The email can change, so it is only informative
        assertThat(claims.getSubject()).isEqualTo(TEST_USER_ID.toString());
        assertThat(claims.getStringClaim(OAuth2TokenService.EMAIL_CLAIM)).isEqualTo(TEST_EMAIL);
        assertThat(claims.getStringListClaim("authorities")).containsExactly("ROLE_USER");
    }

    @Test
    void generateRefreshTokenIdentifiesTheUserById() throws Exception {
        // Arrange
        when(keyStoreService.getOrCreateActiveKeyPair()).thenReturn(rsaKey);

        // Act
        JWTClaimsSet claims = SignedJWT.parse(oauth2TokenService.generateRefreshToken(user)).getJWTClaimsSet();

        // Assert
        assertThat(claims.getSubject()).isEqualTo(TEST_USER_ID.toString());
        assertThat(claims.getStringClaim(OAuth2TokenService.TOKEN_TYPE_CLAIM))
                .isEqualTo(OAuth2TokenService.REFRESH_TOKEN_TYPE);
    }

    @Test
    void generateAccessTokenValidUserReturnsToken() throws Exception {
        // Arrange
        when(keyStoreService.getOrCreateActiveKeyPair()).thenReturn(rsaKey);

        // Act
        String token = oauth2TokenService.generateAccessToken(user);

        // Assert
        assertNotNull(token);
    }

    @Test
    void generateRefreshTokenValidUserReturnsToken() throws Exception {
        // Arrange
        when(keyStoreService.getOrCreateActiveKeyPair()).thenReturn(rsaKey);

        // Act
        String token = oauth2TokenService.generateRefreshToken(user);

        // Assert
        assertNotNull(token);
    }

    @Test
    void generateAccessTokenKeyStoreExceptionThrowsIllegalStateException() throws Exception {
        // Arrange
        when(keyStoreService.getOrCreateActiveKeyPair()).thenThrow(new RuntimeException("Key store error"));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> 
            oauth2TokenService.generateAccessToken(user));
    }

    @Test
    void generateRefreshTokenKeyStoreExceptionThrowsIllegalStateException() throws Exception {
        // Arrange
        when(keyStoreService.getOrCreateActiveKeyPair()).thenThrow(new RuntimeException("Key store error"));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> 
            oauth2TokenService.generateRefreshToken(user));
    }
}
