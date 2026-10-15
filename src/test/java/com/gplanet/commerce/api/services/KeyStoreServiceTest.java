package com.gplanet.commerce.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.gplanet.commerce.api.configs.app.props.JwtProperties;
import com.gplanet.commerce.api.configs.app.props.KeyEncryptionProperties;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;

@ExtendWith(MockitoExtension.class)
class KeyStoreServiceTest {

    private static final String TEST_KEY_ID = "test-key-id";
    private static final String RETIRED_KEY_ID = "retired-key-id";
    private static final String EXPIRED_KEY_ID = "expired-key-id";
    private static final int RSA_KEY_SIZE = 2048;
    private static final Duration ACCESS_TOKEN_LIFETIME = Duration.ofHours(1);
    private static final Duration REFRESH_TOKEN_LIFETIME = Duration.ofHours(24);
    private static final Duration RETIRED_KEY_RETENTION = Duration.ofHours(25);

    private static final String ACTIVE_KEY_ID_KEY = "oauth2:active_key_id";
    private static final String KEY_IDS_KEY = "oauth2:key_ids";
    private static final String KEYPAR_PRIVATE_PATTERN = "oauth2:keypair:%s:private";
    private static final String KEYPAR_PUBLIC_PATTERN = "oauth2:keypair:%s:public";
    private static final String LEGACY_ENCRYPTION_PATTERN = "oauth2:encryption:%s";
    private static final String LEGACY_ENCRYPTION_PREFIX = "oauth2:encryption:";

    // Deriving the encryption key is deliberately slow, so the tests share these ciphers
    private static final PrivateKeyCipher PRIVATE_KEY_CIPHER = privateKeyCipher("test-secret-of-the-key-store-service");
    private static final PrivateKeyCipher OTHER_SECRET_CIPHER = privateKeyCipher("another-secret-of-the-key-store");

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

    @Captor
    private ArgumentCaptor<String> storedPrivateKeyCaptor;

    private KeyStoreService keyStoreService;

    private RSAKey testRsaKey;
    private String encryptedPrivateKey;
    private String publicKeyEncoded;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        lenient().when(jwtProperties.getAccessExpirationMs()).thenReturn(ACCESS_TOKEN_LIFETIME.toMillis());
        lenient().when(jwtProperties.getRefreshExpirationMs()).thenReturn(REFRESH_TOKEN_LIFETIME.toMillis());
        keyStoreService = new KeyStoreService(stringRedisTemplate, jwtProperties, PRIVATE_KEY_CIPHER);

        // Create a proper RSA key for testing
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(RSA_KEY_SIZE);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        testRsaKey = new RSAKey.Builder((java.security.interfaces.RSAPublicKey) keyPair.getPublic())
                .privateKey(keyPair.getPrivate())
                .keyID(TEST_KEY_ID)
                .build();

        // Encrypt the private key as the service does
        encryptedPrivateKey = PRIVATE_KEY_CIPHER.encrypt(TEST_KEY_ID, testRsaKey.toRSAPrivateKey().getEncoded());

        // Encode the public key
        publicKeyEncoded = Base64.getEncoder().encodeToString(
            testRsaKey.toRSAPublicKey().getEncoded());
    }

    private static PrivateKeyCipher privateKeyCipher(String secret) {
        KeyEncryptionProperties properties = new KeyEncryptionProperties();
        properties.setSecret(secret);
        return new PrivateKeyCipher(properties);
    }

    private void stubStoredKeyPair(String keyId) {
        stubStoredKeyPair(keyId, encryptedPrivateKey);
    }

    private void stubStoredKeyPair(String keyId, String storedPrivateKey) {
        when(valueOperations.get(String.format(KEYPAR_PRIVATE_PATTERN, keyId))).thenReturn(storedPrivateKey);
        when(valueOperations.get(String.format(KEYPAR_PUBLIC_PATTERN, keyId))).thenReturn(publicKeyEncoded);
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
            .hasSize(2)
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
    void getOrCreateActiveKeyPairRedisFailurePropagatesWithoutRotating() {
        // Arrange - Redis fails while reading the active key pair
        when(valueOperations.get(ACTIVE_KEY_ID_KEY)).thenReturn(TEST_KEY_ID);
        when(valueOperations.get(String.format(KEYPAR_PRIVATE_PATTERN, TEST_KEY_ID)))
            .thenThrow(new RedisConnectionFailureException("Redis unavailable"));

        // Act & Assert - A Redis error is not missing key material, so the key must not be replaced
        assertThrows(RedisConnectionFailureException.class, () ->
            keyStoreService.getOrCreateActiveKeyPair());
        verify(valueOperations, never()).set(eq(ACTIVE_KEY_ID_KEY), anyString());
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
    }

    @Test
    void rotateKeyPairStoresPrivateKeyEncryptedWithoutItsEncryptionKey() throws Exception {
        // Arrange
        when(valueOperations.get(ACTIVE_KEY_ID_KEY)).thenReturn(null);

        // Act
        RSAKey result = keyStoreService.rotateKeyPair();

        // Assert - Only the configured secret decrypts the stored private key, and no AES key is stored
        verify(valueOperations).set(
            eq(String.format(KEYPAR_PRIVATE_PATTERN, result.getKeyID())), storedPrivateKeyCaptor.capture());
        assertThat(PRIVATE_KEY_CIPHER.decrypt(result.getKeyID(), storedPrivateKeyCaptor.getValue()))
            .isEqualTo(result.toRSAPrivateKey().getEncoded());
        verify(valueOperations, never()).set(startsWith(LEGACY_ENCRYPTION_PREFIX), anyString());
    }

    @Test
    void getOrCreateActiveKeyPairReplacesKeyEncryptedByPreviousVersion() throws Exception {
        // Arrange - Earlier versions encrypted private keys with AES/ECB and stored the AES key in Redis
        SecretKey legacyEncryptionKey = KeyGenerator.getInstance("AES").generateKey();
        Cipher legacyCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        legacyCipher.init(Cipher.ENCRYPT_MODE, legacyEncryptionKey);
        String encodedPrivateKey = Base64.getEncoder().encodeToString(testRsaKey.toRSAPrivateKey().getEncoded());
        String legacyPrivateKey = Base64.getEncoder().encodeToString(
            legacyCipher.doFinal(encodedPrivateKey.getBytes()));
        when(valueOperations.get(ACTIVE_KEY_ID_KEY)).thenReturn(TEST_KEY_ID);
        stubStoredKeyPair(TEST_KEY_ID, legacyPrivateKey);
        lenient().when(valueOperations.get(String.format(LEGACY_ENCRYPTION_PATTERN, TEST_KEY_ID)))
            .thenReturn(Base64.getEncoder().encodeToString(legacyEncryptionKey.getEncoded()));

        // Act
        RSAKey result = keyStoreService.getOrCreateActiveKeyPair();

        // Assert - The exposed key is retired and its AES key deleted
        assertThat(result.getKeyID()).isNotEqualTo(TEST_KEY_ID);
        verify(valueOperations).set(ACTIVE_KEY_ID_KEY, result.getKeyID());
        verify(stringRedisTemplate).expire(String.format(KEYPAR_PUBLIC_PATTERN, TEST_KEY_ID), RETIRED_KEY_RETENTION);
        verify(stringRedisTemplate).delete(String.format(LEGACY_ENCRYPTION_PATTERN, TEST_KEY_ID));
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
    void getPublicKeysIncludesActiveKeyMissingFromKeySet() {
        // Arrange - Active key created by a previous version, not adopted into the key set yet
        when(setOperations.members(KEY_IDS_KEY)).thenReturn(Set.of());
        when(valueOperations.get(ACTIVE_KEY_ID_KEY)).thenReturn(TEST_KEY_ID);
        when(valueOperations.multiGet(List.of(String.format(KEYPAR_PUBLIC_PATTERN, TEST_KEY_ID))))
            .thenReturn(List.of(publicKeyEncoded));

        // Act
        List<RSAKey> result = keyStoreService.getPublicKeys();

        // Assert
        assertThat(result).extracting(JWK::getKeyID).containsExactly(TEST_KEY_ID);
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
        stubStoredKeyPair(TEST_KEY_ID, "invalid-base64-with-hyphens-like-this");

        // Act
        RSAKey result = keyStoreService.getKeyPair(TEST_KEY_ID);

        // Assert - Should return null when Base64 is invalid
        assertNull(result);
    }

    @Test
    void getKeyPairEncryptedWithAnotherSecretReturnsNull() throws Exception {
        // Arrange
        stubStoredKeyPair(TEST_KEY_ID,
            OTHER_SECRET_CIPHER.encrypt(TEST_KEY_ID, testRsaKey.toRSAPrivateKey().getEncoded()));

        // Act
        RSAKey result = keyStoreService.getKeyPair(TEST_KEY_ID);

        // Assert
        assertNull(result);
    }

    @Test
    void getKeyPairWithPrivateKeyOfAnotherKeyIdReturnsNull() {
        // Arrange - The private key encrypted for TEST_KEY_ID is stored under another key ID
        stubStoredKeyPair(RETIRED_KEY_ID);

        // Act
        RSAKey result = keyStoreService.getKeyPair(RETIRED_KEY_ID);

        // Assert
        assertNull(result);
    }
}
