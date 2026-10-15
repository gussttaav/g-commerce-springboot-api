package com.gplanet.commerce.api.services;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import com.gplanet.commerce.api.configs.app.props.KeyEncryptionProperties;

/**
 * Encrypts the RSA private keys that {@link KeyStoreService} stores in Redis.
 * <p>
 * Private keys are encrypted with AES-256-GCM, which also authenticates them: a modified entry fails to decrypt
 * instead of producing a different key. The AES key is derived once, when the application starts, from the secret
 * configured in {@link KeyEncryptionProperties}, and it is never stored. Each encryption uses a random IV, and the
 * key ID is bound to the ciphertext as associated data, so an encrypted private key cannot be moved to another
 * key ID.
 * </p>
 * <p>
 * The encrypted form is the Base64 encoding of the 12-byte IV followed by the ciphertext and its 16-byte tag.
 * </p>
 *
 * @author Gustavo
 * @version 1.0
 */
@Component
public class PrivateKeyCipher {

    private static final String KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256";
    /** Not secret: it separates this key from any other key derived from the same secret. */
    private static final byte[] KEY_DERIVATION_SALT =
            "g-commerce:oauth2:private-key-encryption".getBytes(StandardCharsets.UTF_8);
    private static final int KEY_DERIVATION_ITERATIONS = 600_000;
    private static final int KEY_SIZE_BITS = 256;
    private static final String KEY_ALGORITHM = "AES";
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKey encryptionKey;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Derives the encryption key from the configured secret.
     *
     * @param properties the key encryption properties, with a validated secret
     * @throws IllegalStateException if the key cannot be derived
     */
    public PrivateKeyCipher(KeyEncryptionProperties properties) {
        this.encryptionKey = deriveKey(properties.getSecret());
    }

    /**
     * Encrypts the private key of a key pair.
     *
     * @param keyId the key pair identifier, bound to the ciphertext
     * @param privateKey the encoded private key
     * @return String the Base64-encoded IV, ciphertext and tag
     * @throws GeneralSecurityException if the encryption fails
     */
    public String encrypt(String keyId, byte[] privateKey) throws GeneralSecurityException {
        byte[] iv = new byte[IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);
        byte[] ciphertext = initCipher(Cipher.ENCRYPT_MODE, keyId, iv).doFinal(privateKey);
        byte[] encrypted = ByteBuffer.allocate(iv.length + ciphertext.length).put(iv).put(ciphertext).array();
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * Decrypts the private key of a key pair.
     *
     * @param keyId the key pair identifier the private key was encrypted for
     * @param encryptedPrivateKey the Base64-encoded IV, ciphertext and tag
     * @return byte[] the encoded private key
     * @throws GeneralSecurityException if the private key was encrypted with another key or for another key ID,
     *         or it has been modified
     * @throws IllegalArgumentException if the encrypted private key is not valid Base64
     */
    public byte[] decrypt(String keyId, String encryptedPrivateKey) throws GeneralSecurityException {
        byte[] encrypted = Base64.getDecoder().decode(encryptedPrivateKey);
        if (encrypted.length < IV_LENGTH_BYTES + TAG_LENGTH_BITS / Byte.SIZE) {
            throw new GeneralSecurityException("Encrypted private key is too short");
        }
        byte[] iv = Arrays.copyOf(encrypted, IV_LENGTH_BYTES);
        return initCipher(Cipher.DECRYPT_MODE, keyId, iv)
                .doFinal(encrypted, IV_LENGTH_BYTES, encrypted.length - IV_LENGTH_BYTES);
    }

    private Cipher initCipher(int mode, String keyId, byte[] iv) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        cipher.init(mode, encryptionKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
        cipher.updateAAD(keyId.getBytes(StandardCharsets.UTF_8));
        return cipher;
    }

    private static SecretKey deriveKey(String secret) {
        PBEKeySpec keySpec = new PBEKeySpec(
                secret.toCharArray(), KEY_DERIVATION_SALT, KEY_DERIVATION_ITERATIONS, KEY_SIZE_BITS);
        try {
            byte[] key = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM).generateSecret(keySpec).getEncoded();
            return new SecretKeySpec(key, KEY_ALGORITHM);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Failed to derive the private key encryption key", ex);
        } finally {
            keySpec.clearPassword();
        }
    }
}
