package com.gplanet.commerce.api.services;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.nimbusds.jose.jwk.RSAKey;

@ExtendWith(MockitoExtension.class)
class KeyStoreServiceTest {

    private static final String TEST_KEY_ID = "test-key-id";
    private static final int RSA_KEY_SIZE = 2048;
    private static final long KEY_EXPIRATION_DAYS = 30;
    private static final int AES_KEY_SIZE = 256;

    private static final String ACTIVE_KEY_ID_KEY = "oauth2:active_key_id";
    private static final String KEYPAR_PRIVATE_PATTERN = "oauth2:keypair:%s:private";
    private static final String KEYPAR_PUBLIC_PATTERN = "oauth2:keypair:%s:public";
    private static final String ENCRYPTION_PATTERN = "oauth2:encryption:%s";

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private KeyStoreService keyStoreService;

    private RSAKey testRsaKey;
    private String encryptedPrivateKey;
    private String publicKeyEncoded;
    private String encryptionKeyEncoded;

    @BeforeEach
    void setUp() throws Exception {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        
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

    @Test
    void getOrCreateActiveKeyPairExistingKeyReturnsKey() {
        // Arrange
        when(valueOperations.get(ACTIVE_KEY_ID_KEY)).thenReturn(TEST_KEY_ID);
        when(valueOperations.get(String.format(KEYPAR_PRIVATE_PATTERN, TEST_KEY_ID)))
            .thenReturn(encryptedPrivateKey);
        when(valueOperations.get(String.format(KEYPAR_PUBLIC_PATTERN, TEST_KEY_ID)))
            .thenReturn(publicKeyEncoded);
        when(valueOperations.get(String.format(ENCRYPTION_PATTERN, TEST_KEY_ID)))
            .thenReturn(encryptionKeyEncoded);

        // Act
        RSAKey result = keyStoreService.getOrCreateActiveKeyPair();

        // Assert
        assertNotNull(result);
    }

    @Test
    void getOrCreateActiveKeyPairNoActiveKeyCreatesNewKey() {
        // Arrange
        when(valueOperations.get(ACTIVE_KEY_ID_KEY)).thenReturn(null);

        // Act
        RSAKey result = keyStoreService.getOrCreateActiveKeyPair();

        // Assert - Should create a new key when no active key exists
        assertNotNull(result);
    }

    @Test
    void rotateKeyPairSuccess() {
        // Act
        RSAKey result = keyStoreService.rotateKeyPair();

        // Assert
        assertNotNull(result);
    }

    @Test
    void getKeyPairExistingKeyReturnsKey() {
        // Arrange
        when(valueOperations.get(String.format(KEYPAR_PRIVATE_PATTERN, TEST_KEY_ID)))
            .thenReturn(encryptedPrivateKey);
        when(valueOperations.get(String.format(KEYPAR_PUBLIC_PATTERN, TEST_KEY_ID)))
            .thenReturn(publicKeyEncoded);
        when(valueOperations.get(String.format(ENCRYPTION_PATTERN, TEST_KEY_ID)))
            .thenReturn(encryptionKeyEncoded);

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
    void generateAndStoreKeyPairSuccess() {
        // Act
        RSAKey result = keyStoreService.generateAndStoreKeyPair();

        // Assert
        assertNotNull(result);
    }

    @Test
    void generateAndStoreKeyPairExceptionThrowsIllegalStateException() {
        // Arrange - Force exception by mocking problematic behavior
        doThrow(new RuntimeException("Test exception"))
            .when(valueOperations).set(anyString(), anyString(), eq(Duration.ofDays(KEY_EXPIRATION_DAYS)));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> 
            keyStoreService.generateAndStoreKeyPair());
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
