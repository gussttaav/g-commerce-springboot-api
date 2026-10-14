package com.gplanet.commerce.api.controllers;

import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_AUTH_LOGIN;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_JWKS;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_OPENID_CONFIG;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.gplanet.commerce.api.configs.TestSecurityConfig;
import com.gplanet.commerce.api.configs.app.props.JwtProperties;
import com.gplanet.commerce.api.services.KeyStoreService;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;

@WebMvcTest(JwkController.class)
@Import(TestSecurityConfig.class)
class JwkControllerTest {

    private static final String TEST_KEY_ID = "test-key-id";
    private static final int RSA_KEY_SIZE = 2048;
    private static final String RSA_ALGORITHM = "RSA";
    private static final String RSA_KEY_GENERATION_ALG = "RS256";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KeyStoreService keyStoreService;

    @MockitoBean
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        // Setup common mock responses
        when(jwtProperties.getIssuer()).thenReturn("https://api.example.com");
    }

    private RSAKey createTestRSAKey() {
        return createTestRSAKey(TEST_KEY_ID);
    }

    private RSAKey createTestRSAKey(String keyId) {
        try {
            // Create a proper RSAKey with all required fields
            return new RSAKey.Builder(createTestRSAPublicKey())
                .keyUse(KeyUse.SIGNATURE) // This is the missing field causing NPE
                .algorithm(Algorithm.parse(RSA_KEY_GENERATION_ALG))
                .keyID(keyId)
                .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test RSA key", e);
        }
    }

    private RSAPublicKey createTestRSAPublicKey() {
        try {
            // Create a minimal RSA public key for testing
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(RSA_ALGORITHM);
            keyGen.initialize(RSA_KEY_SIZE);
            return (RSAPublicKey) keyGen.generateKeyPair().getPublic();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test RSA public key", e);
        }
    }

    @Nested
    @DisplayName("GET " + ENDPOINT_JWKS)
    class JwksTests {

        @Test
        @WithAnonymousUser
        @DisplayName("Should return valid JWKS response")
        void jwksReturnsValidJwksResponse() throws Exception {
            // Arrange
            RSAKey rsaKey = createTestRSAKey();
            when(keyStoreService.getPublicKeys()).thenReturn(List.of(rsaKey));

            // Act & Assert
            mockMvc.perform(get(ENDPOINT_JWKS))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.keys[0].kty").value(RSA_ALGORITHM))
                .andExpect(jsonPath("$.keys[0].kid").value(TEST_KEY_ID))
                .andExpect(jsonPath("$.keys[0].use").value("sig"))
                .andExpect(jsonPath("$.keys[0].alg").value(RSA_KEY_GENERATION_ALG))
                .andExpect(jsonPath("$.keys[0].n").exists())
                .andExpect(jsonPath("$.keys[0].e").exists());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Should publish every key returned by the key store, in order")
        void jwksReturnsActiveAndRetiredKeys() throws Exception {
            // Arrange
            RSAKey activeKey = createTestRSAKey("active-key-id");
            RSAKey retiredKey = createTestRSAKey("retired-key-id");
            when(keyStoreService.getPublicKeys()).thenReturn(List.of(activeKey, retiredKey));

            // Act & Assert
            mockMvc.perform(get(ENDPOINT_JWKS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys.length()").value(2))
                .andExpect(jsonPath("$.keys[0].kid").value("active-key-id"))
                .andExpect(jsonPath("$.keys[1].kid").value("retired-key-id"));
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Should return empty keys array when no keys available")
        void jwksNoKeysAvailableReturnsEmptyKeys() throws Exception {
            // Arrange
            when(keyStoreService.getPublicKeys()).thenReturn(List.of());

            // Act & Assert
            mockMvc.perform(get(ENDPOINT_JWKS))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.keys").isArray())
                .andExpect(jsonPath("$.keys").isEmpty());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Should handle service exception gracefully")
        void jwksServiceExceptionReturnsInternalServerError() throws Exception {
            // Arrange
            when(keyStoreService.getPublicKeys())
                .thenThrow(new RuntimeException("Key service unavailable"));

            // Act & Assert
            mockMvc.perform(get(ENDPOINT_JWKS))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"));
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Should be accessible without authentication")
        void jwksAccessibleWithoutAuthentication() throws Exception {
            // Arrange
            RSAKey rsaKey = createTestRSAKey();
            when(keyStoreService.getPublicKeys()).thenReturn(List.of(rsaKey));

            // Act & Assert
            mockMvc.perform(get(ENDPOINT_JWKS))
                .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /.well-known/openid-configuration")
    class OpenIdConfigurationTests {

        @Test
        @WithAnonymousUser
        @DisplayName("Should return valid OpenID configuration")
        void openidConfigurationReturnsValidConfiguration() throws Exception {
            // Act & Assert
            mockMvc.perform(get(ENDPOINT_OPENID_CONFIG))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.issuer").value("https://api.example.com"))
                .andExpect(jsonPath("$.jwks_uri").value(ENDPOINT_JWKS))
                .andExpect(jsonPath("$.token_endpoint").value(ENDPOINT_AUTH_LOGIN))
                .andExpect(jsonPath("$.grant_types_supported[0]").value("password"))
                .andExpect(jsonPath("$.grant_types_supported[1]").value("refresh_token"))
                .andExpect(jsonPath("$.id_token_signing_alg_values_supported[0]").value(RSA_KEY_GENERATION_ALG));
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Should be accessible without authentication")
        void openidConfigurationAccessibleWithoutAuthentication() throws Exception {
            // Act & Assert
            mockMvc.perform(get(ENDPOINT_OPENID_CONFIG))
                .andExpect(status().isOk());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Should include all required OpenID Connect discovery fields")
        void openidConfigurationIncludesAllRequiredFields() throws Exception {
            // Act & Assert
            mockMvc.perform(get(ENDPOINT_OPENID_CONFIG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").exists())
                .andExpect(jsonPath("$.jwks_uri").exists())
                .andExpect(jsonPath("$.token_endpoint").exists())
                .andExpect(jsonPath("$.grant_types_supported").exists())
                .andExpect(jsonPath("$.id_token_signing_alg_values_supported").exists());
        }
    }

    @Nested
    @DisplayName("Additional JWK Tests")
    class AdditionalJwkTests {

        @Test
        @WithAnonymousUser
        @DisplayName("Should handle RSA key without explicit key use")
        void jwksRSAKeyWithoutKeyUseReturnsDefaultKeyUse() throws Exception {
            // Arrange - Create RSA key without explicit key use (like your original test)
            RSAKey rsaKey = new RSAKey.Builder(createTestRSAPublicKey())
                .keyID(TEST_KEY_ID)
                .algorithm(Algorithm.parse(RSA_KEY_GENERATION_ALG))
                .build();
            
            when(keyStoreService.getPublicKeys()).thenReturn(List.of(rsaKey));

            // Act & Assert - The controller should handle this gracefully
            mockMvc.perform(get(ENDPOINT_JWKS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].kty").value(RSA_ALGORITHM))
                .andExpect(jsonPath("$.keys[0].kid").value(TEST_KEY_ID));
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Should return proper content type headers")
        void jwksReturnsCorrectContentType() throws Exception {
            // Arrange
            RSAKey rsaKey = createTestRSAKey();
            when(keyStoreService.getPublicKeys()).thenReturn(List.of(rsaKey));

            // Act & Assert
            mockMvc.perform(get(ENDPOINT_JWKS))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }
    }
}
