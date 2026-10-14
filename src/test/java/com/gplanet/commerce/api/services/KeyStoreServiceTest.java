package com.gplanet.commerce.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.gplanet.commerce.api.configs.app.props.JwtProperties;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;

@ExtendWith(MockitoExtension.class)
class KeyStoreServiceTest {

    private static final String TEST_KEY_ID = "test-key-id";
    private static final String RETIRED_KEY_ID = "retired-key-id";
    private static final String EXPIRED_KEY_ID = "expired-key-id";
    private static final int RSA_KEY_SIZE = 2048;
    private static final int AES_KEY_SIZE = 256;
    private static final Duration ACCESS_TOKEN_LIFETIME = Duration.ofHours(1);
    private static final Duration REFRESH_TOKEN_LIFETIME = Duration.ofHours(24);
    private static final Duration RETIRED_KEY_RETENTION = Duration.ofHours(25);

    private static final String ACTIVE_KEY_ID_KEY = "oauth2:active_key_id";
    private static final String KEY_IDS_KEY = "oauth2:key_ids";
    private static final String KEYPAR_PRIVATE_PATTERN = "oauth2:keypair:%s:private";
    private static final String KEYPAR_PUBLIC_PATTERN = "oauth2:keypair:%s:public";
    private static final String ENCRYPTION_PATTERN = "oauth2:encryption:%s";

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private JwtProperties jwtProperties;

    @Captor
    private ArgumentCaptor<Collection<String>> deletedKeysCaptor;

    @InjectMocks
    private KeyStoreService keyStoreService;

    private RSAKey testRsaKey;
    private String encryptedPrivateKey;
    private String publicKeyEncoded;
    private String encryptionKeyEncoded;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        lenient().when(jwtProperties.getAccessExpirationMs()).thenReturn(ACCESS_TOKEN_LIFETIME.toMillis());
        lenient().when(jwtProperties.getRefreshExpirationMs()).thenReturn(REFRESH_TOKEN_LIFETIME.toMillis());

        // Create a proper RSA key for testing
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(RSA_KEY_SIZE);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        testRsaKey = new RSAKey.Builder((java.security.interfaces.RSAPublicKey) keyPair.getPublic())
                .privateKey(keyPair.getPrivate())
                .keyID(TEST_KEY_ID)
                .build();

        // Create a proper AES encryption key
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(AES_KEY_SIZE);
        SecretKey aesKey = keyGenerator.generateKey();
        encryptionKeyEncoded = Base64.getEncoder().encodeToString(aesKey.getEncoded());

        // Encrypt the private key using AES (simulating what the service does)
        String privateKeyEncoded = Base64.getEncoder().encodeToString(
            testRsaKey.toRSAPrivateKey().getEncoded());
        encryptedPrivateKey = encryptWithAES(privateKeyEncoded, aesKey);

        // Encode the public key
        publicKeyEncoded = Base64.getEncoder().encodeToString(
            testRsaKey.toRSAPublicKey().getEncoded());
    }

    private String encryptWithAES(String data, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    private void stubStoredKeyPair(String keyId) {
        when(valueOperations.get(String.format(KEYPAR_PRIVATE_PATTERN, keyId))).thenReturn(encryptedPrivateKey);
        when(valueOperations.get(String.format(KEYPAR_PUBLIC_PATTERN, keyId))).thenReturn(publicKeyEncoded);
        when(valueOperations.get(String.format(ENCRYPTION_PATTERN, keyId))).thenReturn(encryptionKeyEncoded);
    }

    @Test
    void getOrCreateActiveKeyPairExistingKeyReturnsKey() {
        // Arrange
        when(valueOperations.get(ACTIVE_KEY_ID_KEY)).thenReturn(TEST_KEY_ID);
        stubStoredKeyPair(TEST_KEY_ID);

        // Act
        RSAKey result = keyStoreService.getOrCreateActiveKeyPair();

        // Assert
        assertThat(result.getKeyID()).isEqualTo(TEST_KEY_ID);
        assertThat(result.isPrivate()).isTrue();
    }

    @Test
    void getOrCreateActiveKeyPairNoActiveKeyCreatesNonExpiringKey() {
        // Arrange
        when(valueOperations.get(ACTIVE_KEY_ID_KEY)).thenReturn(null);
        when(valueOperations.setIfAbsent(eq(ACTIVE_KEY_ID_KEY), anyString())).thenReturn(true);

        // Act
        RSAKey result = keyStoreService.getOrCreateActiveKeyPair();

        // Assert - The key is registered and stored without a TTL
        assertNotNull(result);
        verify(setOperations).add(KEY_IDS_KEY, result.getKeyID());
        verify(valueOperations).set(eq(String.format(KEYPAR_PUBLIC_PATTERN, result.getKeyID())), anyString());
        verify(stringRedisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    void getOrCreateActiveKeyPairUsesKeyCreatedConcurrentlyByAnotherInstance() {
        // Arrange - Another instance sets the active key between our read and our write
        when(valueOperations.get(ACTIVE_KEY_ID_KEY)).thenReturn(null, TEST_KEY_ID);
        when(valueOperations.setIfAbsent(eq(ACTIVE_KEY_ID_KEY), anyString())).thenReturn(false);
        stubStoredKeyPair(TEST_KEY_ID);

        // Act
        RSAKey result = keyStoreService.getOrCreateActiveKeyPair();

        // Assert - The other instance's key is used and ours is discarded
        assertThat(result.getKeyID()).isEqualTo(TEST_KEY_ID);
        verify(stringRedisTemplate).delete(deletedKeysCaptor.capture());
        assertThat(deletedKeysCaptor.getValue())
            .hasSize(3)
            .noneMatch(key -> key.contains(TEST_KEY_ID));
    }

    @Test
    void getOrCreateActiveKeyPairMissingKeyMaterialRotatesToNewKey() {
        // Arrange - The active key ID points to a key pair that no longer exists
        when(valueOperations.get(ACTIVE_KEY_ID_KEY)).thenReturn(TEST_KEY_ID);

        // Act
        RSAKey result = keyStoreService.getOrCreateActiveKeyPair();

        // Assert
        assertThat(result.getKeyID()).isNotEqualTo(TEST_KEY_ID);
        verify(valueOperations).set(ACTIVE_KEY_ID_KEY, result.getKeyID());
    }

    @Test
    void rotateKeyPairActivatesNewKeyAndRetiresPreviousKey() {
        // Arrange
        when(valueOperations.get(ACTIVE_KEY_ID_KEY)).thenReturn(TEST_KEY_ID);

        // Act
        RSAKey result = keyStoreService.rotateKeyPair();

        // Assert - New key is active and never expires
        assertThat(result.getKeyID()).isNotEqualTo(TEST_KEY_ID);
        verify(valueOperations).set(ACTIVE_KEY_ID_KEY, result.getKeyID());
        verify(setOperations).add(KEY_IDS_KEY, result.getKeyID());

        // Assert - Previous key outlives the longest token lifetime (24h) plus a grace period
        verify(stringRedisTemplate).expire(String.format(KEYPAR_PUBLIC_PATTERN, TEST_KEY_ID), RETIRED_KEY_RETENTION);
        verify(stringRedisTemplate).expire(String.format(KEYPAR_PRIVATE_PATTERN, TEST_KEY_ID), RETIRED_KEY_RETENTION);
        verify(stringRedisTemplate).expire(String.format(ENCRYPTION_PATTERN, TEST_KEY_ID), RETIRED_KEY_RETENTION);
    }

    @Test
    void rotateKeyPairWithoutPreviousKeyRetiresNothing() {
        // Arrange
        when(valueOperations.get(ACTIVE_KEY_ID_KEY)).thenReturn(null);

        // Act
        RSAKey result = keyStoreService.rotateKeyPair();

        // Assert
        assertNotNull(result);
        verify(stringRedisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    void rotateKeyPairStorageFailureThrowsIllegalStateException() {
        // Arrange - Force exception by mocking problematic behavior
        doThrow(new RuntimeException("Test exception"))
            .when(valueOperations).set(anyString(), anyString());

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
            keyStoreService.rotateKeyPair());
    }

    @Test
    void getPublicKeysReturnsActiveKeyFirstAndPrunesExpiredKeys() {
        // Arrange
        when(setOperations.members(KEY_IDS_KEY))
            .thenReturn(new LinkedHashSet<>(List.of(EXPIRED_KEY_ID, RETIRED_KEY_ID, TEST_KEY_ID)));
        when(valueOperations.get(ACTIVE_KEY_ID_KEY)).thenReturn(TEST_KEY_ID);
        Map<String, String> storedPublicKeys = Map.of(
            String.format(KEYPAR_PUBLIC_PATTERN, TEST_KEY_ID), publicKeyEncoded,
            String.format(KEYPAR_PUBLIC_PATTERN, RETIRED_KEY_ID), publicKeyEncoded);
        when(valueOperations.multiGet(anyCollection())).thenAnswer(invocation ->
            invocation.<Collection<String>>getArgument(0).stream().map(storedPublicKeys::get).toList());

        // Act
        List<RSAKey> result = keyStoreService.getPublicKeys();

        // Assert
        assertThat(result).extracting(JWK::getKeyID).containsExactly(TEST_KEY_ID, RETIRED_KEY_ID);
        assertThat(result).noneMatch(JWK::isPrivate);
        verify(setOperations).remove(KEY_IDS_KEY, EXPIRED_KEY_ID);
    }

    @Test
    void getPublicKeysNoRegisteredKeysReturnsEmptyList() {
        // Arrange
        when(setOperations.members(KEY_IDS_KEY)).thenReturn(Set.of());

        // Act & Assert
        assertThat(keyStoreService.getPublicKeys()).isEmpty();
    }

    @Test
    void initializeActiveKeyAdoptsKeyStoredWithExpiration() {
        // Arrange - Key created by a previous version: not registered and stored with a TTL
        when(valueOperations.get(ACTIVE_KEY_ID_KEY)).thenReturn(TEST_KEY_ID);
        stubStoredKeyPair(TEST_KEY_ID);
        when(setOperations.isMember(KEY_IDS_KEY, TEST_KEY_ID)).thenReturn(false);

        // Act
        keyStoreService.initializeActiveKey();

        // Assert
        verify(stringRedisTemplate).persist(ACTIVE_KEY_ID_KEY);
        verify(stringRedisTemplate).persist(String.format(KEYPAR_PUBLIC_PATTERN, TEST_KEY_ID));
        verify(stringRedisTemplate).persist(String.format(KEYPAR_PRIVATE_PATTERN, TEST_KEY_ID));
        verify(stringRedisTemplate).persist(String.format(ENCRYPTION_PATTERN, TEST_KEY_ID));
        verify(setOperations).add(KEY_IDS_KEY, TEST_KEY_ID);
    }

    @Test
    void initializeActiveKeyLeavesRegisteredKeyUntouched() {
        // Arrange
        when(valueOperations.get(ACTIVE_KEY_ID_KEY)).thenReturn(TEST_KEY_ID);
        stubStoredKeyPair(TEST_KEY_ID);
        when(setOperations.isMember(KEY_IDS_KEY, TEST_KEY_ID)).thenReturn(true);

        // Act
        keyStoreService.initializeActiveKey();

        // Assert
        verify(stringRedisTemplate, never()).persist(anyString());
    }

    @Test
    void getKeyPairExistingKeyReturnsKey() {
        // Arrange
        stubStoredKeyPair(TEST_KEY_ID);

        // Act
        RSAKey result = keyStoreService.getKeyPair(TEST_KEY_ID);

        // Assert - Should return the existing key
        assertNotNull(result);
    }

    @Test
    void getKeyPairNonExistentKeyReturnsNull() {
        // Arrange
        when(valueOperations.get(String.format(KEYPAR_PRIVATE_PATTERN, TEST_KEY_ID))).thenReturn(null);

        // Act
        RSAKey result = keyStoreService.getKeyPair(TEST_KEY_ID);

        // Assert - Should return null when key doesn't exist
        assertNull(result);
    }

    @Test
    void getKeyPairInvalidBase64ReturnsNull() {
        // Arrange - Provide invalid Base64 string
        when(valueOperations.get(String.format(KEYPAR_PRIVATE_PATTERN, TEST_KEY_ID)))
            .thenReturn("invalid-base64-with-hyphens-like-this");
        when(valueOperations.get(String.format(KEYPAR_PUBLIC_PATTERN, TEST_KEY_ID)))
            .thenReturn(publicKeyEncoded);
        when(valueOperations.get(String.format(ENCRYPTION_PATTERN, TEST_KEY_ID)))
            .thenReturn(encryptionKeyEncoded);

        // Act
        RSAKey result = keyStoreService.getKeyPair(TEST_KEY_ID);

        // Assert - Should return null when Base64 is invalid
        assertNull(result);
    }

    @Test
    void getKeyPairMissingEncryptionKeyReturnsNull() {
        // Arrange
        when(valueOperations.get(String.format(KEYPAR_PRIVATE_PATTERN, TEST_KEY_ID)))
            .thenReturn(encryptedPrivateKey);
        when(valueOperations.get(String.format(KEYPAR_PUBLIC_PATTERN, TEST_KEY_ID)))
            .thenReturn(publicKeyEncoded);
        when(valueOperations.get(String.format(ENCRYPTION_PATTERN, TEST_KEY_ID))).thenReturn(null);

        // Act
        RSAKey result = keyStoreService.getKeyPair(TEST_KEY_ID);

        // Assert - Should return null when encryption key is missing
        assertNull(result);
    }

    @Test
    void getKeyPairInvalidEncryptionKeyReturnsNull() {
        // Arrange - Provide invalid Base64 for encryption key
        when(valueOperations.get(String.format(KEYPAR_PRIVATE_PATTERN, TEST_KEY_ID)))
            .thenReturn(encryptedPrivateKey);
        when(valueOperations.get(String.format(KEYPAR_PUBLIC_PATTERN, TEST_KEY_ID)))
            .thenReturn(publicKeyEncoded);
        when(valueOperations.get(String.format(ENCRYPTION_PATTERN, TEST_KEY_ID)))
            .thenReturn("invalid-base64-encryption-key");

        // Act
        RSAKey result = keyStoreService.getKeyPair(TEST_KEY_ID);

        // Assert - Should return null when encryption key is invalid
        assertNull(result);
    }
}
