package com.gplanet.commerce.api.integration;

import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_JWKS;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_USUARIOS_ME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.Base64;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import com.gplanet.commerce.api.configs.app.props.JwtProperties;
import com.gplanet.commerce.api.entities.User;
import com.gplanet.commerce.api.services.KeyStoreService;
import com.nimbusds.jose.jwk.RSAKey;

/**
 * Integration tests for signing key rotation: tokens must keep validating across a rotation
 * without restarting the application (KEY-1). Also covers how private keys are stored (KEY-3).
 */
class KeyRotationIntegrationTest extends AbstractIntegrationTest {

    private static final String USER_EMAIL = "rotation@example.com";
    private static final String USER_PASSWORD = "Password123!";
    private static final String ACTIVE_KEY_ID_ENTRY = "oauth2:active_key_id";
    private static final String KEY_IDS_ENTRY = "oauth2:key_ids";
    private static final String PUBLIC_KEY_ENTRY = "oauth2:keypair:%s:public";
    private static final String PRIVATE_KEY_ENTRY = "oauth2:keypair:%s:private";
    private static final String ENCRYPTION_KEY_ENTRY = "oauth2:encryption:%s";
    private static final long NO_EXPIRATION = -1L;

    @Autowired
    private KeyStoreService keyStoreService;

    @Autowired
    private JwtProperties jwtProperties;

    @BeforeEach
    void createUser() {
        crearUsuario("Rotation User", USER_EMAIL, USER_PASSWORD, User.Role.USER);
    }

    @Test
    void tokenIssuedBeforeRotationIsStillAccepted() throws Exception {
        String token = obtenerJwtToken(USER_EMAIL, USER_PASSWORD);

        keyStoreService.rotateKeyPair();

        mockMvc.perform(get(ENDPOINT_USUARIOS_ME).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
    }

    @Test
    void tokenIssuedAfterRotationIsAcceptedWithoutRestart() throws Exception {
        // Let the decoder load the current key before it is rotated
        mockMvc.perform(get(ENDPOINT_USUARIOS_ME)
                .header(HttpHeaders.AUTHORIZATION, obtenerJwtToken(USER_EMAIL, USER_PASSWORD)))
                .andExpect(status().isOk());

        keyStoreService.rotateKeyPair();
        String token = obtenerJwtToken(USER_EMAIL, USER_PASSWORD);

        mockMvc.perform(get(ENDPOINT_USUARIOS_ME).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
    }

    @Test
    void jwksPublishesActiveKeyFirstAndKeepsRetiredKey() throws Exception {
        String retiredKeyId = keyStoreService.getOrCreateActiveKeyPair().getKeyID();
        String activeKeyId = keyStoreService.rotateKeyPair().getKeyID();

        mockMvc.perform(get(ENDPOINT_JWKS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].kid").value(activeKeyId))
                .andExpect(jsonPath("$.keys[*].kid", hasItem(retiredKeyId)));
    }

    @Test
    void activeKeyNeverExpiresAndRetiredKeyOutlivesLongestTokenLifetime() {
        String retiredKeyId = keyStoreService.getOrCreateActiveKeyPair().getKeyID();
        String activeKeyId = keyStoreService.rotateKeyPair().getKeyID();

        Duration longestTokenLifetime = Duration.ofMillis(
                Math.max(jwtProperties.getAccessExpirationMs(), jwtProperties.getRefreshExpirationMs()));

        assertThat(stringRedisTemplate.getExpire(ACTIVE_KEY_ID_ENTRY)).isEqualTo(NO_EXPIRATION);
        assertThat(stringRedisTemplate.getExpire(PUBLIC_KEY_ENTRY.formatted(activeKeyId))).isEqualTo(NO_EXPIRATION);
        assertThat(stringRedisTemplate.getExpire(PUBLIC_KEY_ENTRY.formatted(retiredKeyId)))
                .isBetween(longestTokenLifetime.toSeconds(), longestTokenLifetime.plusDays(1).toSeconds());
    }

    @Test
    void keyStoredWithExpirationByPreviousVersionIsAdoptedOnStartup() throws Exception {
        // Recreate what earlier versions left in Redis: an unregistered active key with a 30-day TTL
        String keyId = keyStoreService.rotateKeyPair().getKeyID();
        List<String> keyEntries = List.of(ACTIVE_KEY_ID_ENTRY, PUBLIC_KEY_ENTRY.formatted(keyId),
                PRIVATE_KEY_ENTRY.formatted(keyId));
        keyEntries.forEach(entry -> stringRedisTemplate.expire(entry, Duration.ofDays(30)));
        stringRedisTemplate.opsForSet().remove(KEY_IDS_ENTRY, keyId);
        String token = obtenerJwtToken(USER_EMAIL, USER_PASSWORD);

        // Requests served before the startup adoption must still see the active key
        mockMvc.perform(get(ENDPOINT_JWKS))
                .andExpect(jsonPath("$.keys[0].kid").value(keyId));

        keyStoreService.initializeActiveKey();

        assertThat(keyEntries).allSatisfy(entry ->
                assertThat(stringRedisTemplate.getExpire(entry)).isEqualTo(NO_EXPIRATION));
        assertThat(keyStoreService.getOrCreateActiveKeyPair().getKeyID()).isEqualTo(keyId);
        mockMvc.perform(get(ENDPOINT_USUARIOS_ME).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
        mockMvc.perform(get(ENDPOINT_JWKS))
                .andExpect(jsonPath("$.keys[0].kid").value(keyId));
    }

    @Test
    void privateKeyIsStoredWithoutItsEncryptionKey() {
        String keyId = keyStoreService.rotateKeyPair().getKeyID();

        assertThat(stringRedisTemplate.opsForValue().get(PRIVATE_KEY_ENTRY.formatted(keyId))).isNotBlank();
        assertThat(stringRedisTemplate.hasKey(ENCRYPTION_KEY_ENTRY.formatted(keyId))).isFalse();
    }

    @Test
    void keyEncryptedByPreviousVersionIsReplacedOnStartup() throws Exception {
        // Recreate what earlier versions stored: the private key encrypted with AES/ECB, next to its AES key
        RSAKey legacyKey = keyStoreService.rotateKeyPair();
        String legacyKeyId = legacyKey.getKeyID();
        String token = obtenerJwtToken(USER_EMAIL, USER_PASSWORD);
        SecretKey legacyEncryptionKey = KeyGenerator.getInstance("AES").generateKey();
        Cipher legacyCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        legacyCipher.init(Cipher.ENCRYPT_MODE, legacyEncryptionKey);
        String encodedPrivateKey = Base64.getEncoder().encodeToString(legacyKey.toRSAPrivateKey().getEncoded());
        stringRedisTemplate.opsForValue().set(PRIVATE_KEY_ENTRY.formatted(legacyKeyId),
                Base64.getEncoder().encodeToString(legacyCipher.doFinal(encodedPrivateKey.getBytes())));
        stringRedisTemplate.opsForValue().set(ENCRYPTION_KEY_ENTRY.formatted(legacyKeyId),
                Base64.getEncoder().encodeToString(legacyEncryptionKey.getEncoded()));

        keyStoreService.initializeActiveKey();

        // The exposed key no longer signs tokens and its AES key is gone from Redis
        assertThat(keyStoreService.getOrCreateActiveKeyPair().getKeyID()).isNotEqualTo(legacyKeyId);
        assertThat(stringRedisTemplate.hasKey(ENCRYPTION_KEY_ENTRY.formatted(legacyKeyId))).isFalse();
        // Tokens it signed remain valid until they expire
        mockMvc.perform(get(ENDPOINT_USUARIOS_ME).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
    }
}
