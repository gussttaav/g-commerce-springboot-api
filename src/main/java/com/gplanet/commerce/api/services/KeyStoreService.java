package com.gplanet.commerce.api.services;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.nimbusds.jose.jwk.RSAKey;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service class that manages cryptographic key storage and rotation for JWT token operations.
 * Handles generation, storage, and retrieval of RSA key pairs with secure encryption.
 * 
 * @author Gustavo
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeyStoreService {

    private static final String KEY_PAIR_PREFIX = "oauth2:keypair:";
    private static final String ENCRYPTION_KEY_PREFIX = "oauth2:encryption:";
    private static final String ACTIVE_KEY_ID = "oauth2:active_key_id";
    private static final Duration KEY_EXPIRATION = Duration.ofDays(30);
    private static final String ENCRYPTION_ALGORITHM = "AES";
    private static final String CIPHER_TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final String RSA_ALGORITHM = "RSA";
    private static final int RSA_KEY_SIZE = 2048;
    private static final int AES_KEY_SIZE = 256;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Retrieves the active RSA key pair or creates a new one if none exists.
     * 
     * @return RSAKey the active RSA key pair
     */
    public RSAKey getOrCreateActiveKeyPair() {
        String activeKeyId = stringRedisTemplate.opsForValue().get(ACTIVE_KEY_ID);
        if (activeKeyId != null) {
            RSAKey existing = getKeyPair(activeKeyId);
            if (existing != null) {
                return existing;
            }
        }
        RSAKey newKey = generateAndStoreKeyPair();
        stringRedisTemplate.opsForValue().set(ACTIVE_KEY_ID, newKey.getKeyID(), KEY_EXPIRATION);
        return newKey;
    }

    /**
     * Rotates the active key pair by generating and storing a new RSA key.
     * 
     * @return RSAKey the newly generated RSA key pair
     */
    public RSAKey rotateKeyPair() {
        RSAKey newKey = generateAndStoreKeyPair();
        stringRedisTemplate.opsForValue().set(ACTIVE_KEY_ID, newKey.getKeyID(), KEY_EXPIRATION);
        return newKey;
    }

    /**
     * Generates and stores a new RSA key pair with secure encryption.
     * 
     * @return RSAKey the generated RSA key pair
     * @throws IllegalStateException if key pair generation fails
     */
    public RSAKey generateAndStoreKeyPair() {
        try {
            String keyId = UUID.randomUUID().toString();
            KeyPair keyPair = generateRSAKeyPair();

            RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                    .privateKey((RSAPrivateKey) keyPair.getPrivate())
                    .keyID(keyId)
                    .build();

            storeKeyPairSecurely(keyId, rsaKey);
            log.info("Generated and stored new RSA key pair with ID: {}", keyId);
            return rsaKey;
        } catch (Exception ex) {
            throw new IllegalStateException("Key pair generation failed", ex);
        }
    }

    /**
     * Retrieves a specific RSA key pair by its ID.
     * 
     * @param keyId the key pair identifier
     * @return RSAKey the retrieved RSA key pair, or null if not found
     */
    public RSAKey getKeyPair(String keyId) {
        try {
            String encryptedPrivateKey = stringRedisTemplate.opsForValue().get(KEY_PAIR_PREFIX + keyId + ":private");
            String publicKey = stringRedisTemplate.opsForValue().get(KEY_PAIR_PREFIX + keyId + ":public");
            if (encryptedPrivateKey == null || publicKey == null) {
                return null;
            }
            String decryptedPrivateKey = decryptPrivateKey(keyId, encryptedPrivateKey);
            byte[] pubBytes = Base64.getDecoder().decode(publicKey);
            byte[] privBytes = Base64.getDecoder().decode(decryptedPrivateKey);
            RSAPublicKey pub = (RSAPublicKey) java.security.KeyFactory.getInstance(RSA_ALGORITHM)
                    .generatePublic(new java.security.spec.X509EncodedKeySpec(pubBytes));
            RSAPrivateKey priv = (RSAPrivateKey) java.security.KeyFactory.getInstance(RSA_ALGORITHM)
                    .generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(privBytes));
            return new RSAKey.Builder(pub).privateKey(priv).keyID(keyId).build();
        } catch (Exception ex) {
            log.error("Failed to retrieve key pair for ID: {}", keyId, ex);
            return null;
        }
    }

    /**
     * Generates a new RSA key pair.
     * 
     * @return KeyPair the generated RSA key pair
     * @throws NoSuchAlgorithmException if RSA algorithm is not available
     */
    private KeyPair generateRSAKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        keyPairGenerator.initialize(RSA_KEY_SIZE);
        return keyPairGenerator.generateKeyPair();
    }

    /**
     * Stores an RSA key pair securely with encryption.
     * 
     * @param keyId the key pair identifier
     * @param rsaKey the RSA key to store
     * @throws Exception if key storage operations fail
     */
    private void storeKeyPairSecurely(String keyId, RSAKey rsaKey) throws Exception {
        SecretKey encryptionKey = generateEncryptionKey();
        String encryptionKeyEncoded = Base64.getEncoder().encodeToString(encryptionKey.getEncoded());
        stringRedisTemplate.opsForValue()
            .set(ENCRYPTION_KEY_PREFIX + keyId, encryptionKeyEncoded, KEY_EXPIRATION);

        String privateKeyEncoded = Base64.getEncoder().encodeToString(rsaKey.toRSAPrivateKey().getEncoded());
        String encryptedPrivateKey = encryptPrivateKey(encryptionKey, privateKeyEncoded);
        stringRedisTemplate.opsForValue()
            .set(KEY_PAIR_PREFIX + keyId + ":private", encryptedPrivateKey, KEY_EXPIRATION);

        String publicKeyEncoded = Base64.getEncoder().encodeToString(rsaKey.toRSAPublicKey().getEncoded());
        stringRedisTemplate.opsForValue().set(KEY_PAIR_PREFIX + keyId + ":public", publicKeyEncoded, KEY_EXPIRATION);
    }

    /**
     * Generates an AES encryption key for securing private keys.
     * 
     * @return SecretKey the generated AES encryption key
     * @throws NoSuchAlgorithmException if AES algorithm is not available
     */
    private SecretKey generateEncryptionKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ENCRYPTION_ALGORITHM);
        keyGenerator.init(AES_KEY_SIZE);
        return keyGenerator.generateKey();
    }

    /**
     * Encrypts a private key using AES encryption.
     * 
     * @param encryptionKey the AES encryption key
     * @param privateKey the private key to encrypt
     * @return String the Base64-encoded encrypted private key
     * @throws Exception if encryption operations fail
     */
    private String encryptPrivateKey(SecretKey encryptionKey, String privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey);
        byte[] encryptedBytes = cipher.doFinal(privateKey.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * Decrypts a private key using the corresponding encryption key.
     * 
     * @param keyId the key pair identifier
     * @param encryptedPrivateKey the encrypted private key
     * @return String the decrypted private key
     * @throws Exception if decryption operations fail
     */
    private String decryptPrivateKey(String keyId, String encryptedPrivateKey) throws Exception {
        String encryptionKeyEncoded = stringRedisTemplate.opsForValue().get(ENCRYPTION_KEY_PREFIX + keyId);
        if (encryptionKeyEncoded == null) {
            throw new IllegalStateException("Encryption key not found for key ID: " + keyId);
        }
        byte[] encryptionKeyBytes = Base64.getDecoder().decode(encryptionKeyEncoded);
        SecretKey encryptionKey = new SecretKeySpec(encryptionKeyBytes, ENCRYPTION_ALGORITHM);
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, encryptionKey);
        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedPrivateKey);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes);
    }
}


