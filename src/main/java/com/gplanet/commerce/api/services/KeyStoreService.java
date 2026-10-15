package com.gplanet.commerce.api.services;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.gplanet.commerce.api.configs.app.props.JwtProperties;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service class that manages the RSA key pairs used to sign and verify JWT tokens.
 * <p>
 * Keys are stored in Redis so that every application instance signs with the same active key. The active
 * key never expires on its own; it only changes when it is rotated. Rotation retires the previous key: it
 * is no longer used for signing, but it remains available for verification (and is published in the JWKS)
 * until every token it signed has expired.
 * </p>
 * <p>
 * Private keys are stored encrypted by {@link PrivateKeyCipher}, whose key is not stored in Redis.
 * </p>
 *
 * @author Gustavo
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeyStoreService {

    private static final String KEY_PAIR_PREFIX = "oauth2:keypair:";
    /** Earlier versions stored the AES key of each private key here, next to the encrypted private key. */
    private static final String LEGACY_ENCRYPTION_KEY_PREFIX = "oauth2:encryption:";
    private static final String ACTIVE_KEY_ID = "oauth2:active_key_id";
    private static final String KEY_IDS = "oauth2:key_ids";
    private static final Duration RETIRED_KEY_GRACE_PERIOD = Duration.ofHours(1);
    private static final String RSA_ALGORITHM = "RSA";
    private static final int RSA_KEY_SIZE = 2048;

    private final StringRedisTemplate stringRedisTemplate;
    private final JwtProperties jwtProperties;
    private final PrivateKeyCipher privateKeyCipher;

    /**
     * Ensures an active key pair exists when the application starts and that it never expires.
     * <p>
     * Earlier versions of this service stored keys with a 30-day TTL and did not register them in the key
     * set, so such a key is adopted here instead of being left to expire.
     * </p>
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeActiveKey() {
        String activeKeyId = getOrCreateActiveKeyPair().getKeyID();
        if (Boolean.TRUE.equals(stringRedisTemplate.opsForSet().isMember(KEY_IDS, activeKeyId))) {
            return;
        }
        stringRedisTemplate.persist(ACTIVE_KEY_ID);
        keyPairEntries(activeKeyId).forEach(stringRedisTemplate::persist);
        stringRedisTemplate.opsForSet().add(KEY_IDS, activeKeyId);
        log.info("Adopted existing RSA key pair {} as a non-expiring active key", activeKeyId);
    }

    /**
     * Retrieves the active RSA key pair or creates a new one if none exists.
     * <p>
     * An active key that cannot be read is replaced by a new one, e.g. a key stored by an earlier version, which
     * kept the AES key of the private key in Redis, or a key encrypted with a different secret.
     * </p>
     *
     * @return RSAKey the active RSA key pair, including its private key
     */
    public RSAKey getOrCreateActiveKeyPair() {
        String activeKeyId = stringRedisTemplate.opsForValue().get(ACTIVE_KEY_ID);
        if (activeKeyId == null) {
            return createFirstKeyPair();
        }
        RSAKey activeKey = getKeyPair(activeKeyId);
        if (activeKey == null) {
            log.warn("Key material for active key {} is missing or unreadable, rotating to a new key pair",
                    activeKeyId);
            return rotateKeyPair();
        }
        return activeKey;
    }

    /**
     * Rotates the active key pair: generates a new active key and retires the previous one.
     * <p>
     * The retired key stays available for verification for the longest token lifetime (plus a grace
     * period), so tokens signed before the rotation remain valid until they expire.
     * </p>
     *
     * @return RSAKey the newly generated RSA key pair
     */
    public RSAKey rotateKeyPair() {
        String previousKeyId = stringRedisTemplate.opsForValue().get(ACTIVE_KEY_ID);
        RSAKey newKey = generateAndStoreKeyPair();
        stringRedisTemplate.opsForValue().set(ACTIVE_KEY_ID, newKey.getKeyID());
        if (previousKeyId != null) {
            retireKeyPair(previousKeyId);
        }
        log.info("Rotated RSA signing key from {} to {}", previousKeyId, newKey.getKeyID());
        return newKey;
    }

    /**
     * Retrieves a specific RSA key pair by its ID.
     *
     * @param keyId the key pair identifier
     * @return RSAKey the retrieved RSA key pair, or null if it is missing or cannot be decrypted or decoded
     * @throws org.springframework.dao.DataAccessException if Redis cannot be read
     */
    public RSAKey getKeyPair(String keyId) {
        try {
            String encryptedPrivateKey = stringRedisTemplate.opsForValue().get(privateKeyEntry(keyId));
            String publicKey = stringRedisTemplate.opsForValue().get(publicKeyEntry(keyId));
            if (encryptedPrivateKey == null || publicKey == null) {
                return null;
            }
            byte[] privBytes = privateKeyCipher.decrypt(keyId, encryptedPrivateKey);
            RSAPrivateKey priv = (RSAPrivateKey) KeyFactory.getInstance(RSA_ALGORITHM)
                    .generatePrivate(new PKCS8EncodedKeySpec(privBytes));
            return rsaKeyBuilder(keyId, decodePublicKey(publicKey)).privateKey(priv).build();
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            // Unreadable key material. Redis errors propagate: they must not be mistaken for a missing key.
            log.error("Failed to decrypt or decode key pair for ID: {}", keyId, ex);
            return null;
        }
    }

    /**
     * Retrieves the public keys that tokens may still be signed with: the active key and every retired key
     * whose retention period has not ended yet.
     * <p>
     * The active key is included even if it is not registered in the key set, which is the case for a key
     * created by an earlier version until {@link #initializeActiveKey()} adopts it.
     * </p>
     *
     * @return the public keys, with the active key first
     */
    public List<RSAKey> getPublicKeys() {
        String activeKeyId = stringRedisTemplate.opsForValue().get(ACTIVE_KEY_ID);
        Set<String> storedKeyIds = stringRedisTemplate.opsForSet().members(KEY_IDS);
        List<String> keyIds = new ArrayList<>();
        if (activeKeyId != null) {
            keyIds.add(activeKeyId);
        }
        if (storedKeyIds != null) {
            storedKeyIds.stream().filter(keyId -> !keyId.equals(activeKeyId)).forEach(keyIds::add);
        }
        if (keyIds.isEmpty()) {
            return List.of();
        }

        List<String> encodedKeys = stringRedisTemplate.opsForValue()
                .multiGet(keyIds.stream().map(KeyStoreService::publicKeyEntry).toList());
        if (encodedKeys == null) {
            return List.of();
        }
        List<RSAKey> publicKeys = new ArrayList<>();
        for (int i = 0; i < keyIds.size(); i++) {
            String keyId = keyIds.get(i);
            String encodedKey = encodedKeys.get(i);
            if (encodedKey == null) {
                // Retired key whose retention period is over
                stringRedisTemplate.opsForSet().remove(KEY_IDS, keyId);
                continue;
            }
            try {
                publicKeys.add(rsaKeyBuilder(keyId, decodePublicKey(encodedKey)).build());
            } catch (GeneralSecurityException | IllegalArgumentException ex) {
                log.error("Skipping unreadable public key for ID: {}", keyId, ex);
            }
        }
        return publicKeys;
    }

    /**
     * Creates the very first active key pair. Several instances may start at the same time, so only the
     * first key to be registered becomes active; the others are discarded.
     *
     * @return RSAKey the active RSA key pair
     * @throws IllegalStateException if another instance won the race but its key pair cannot be read
     */
    private RSAKey createFirstKeyPair() {
        RSAKey newKey = generateAndStoreKeyPair();
        Boolean activated = stringRedisTemplate.opsForValue().setIfAbsent(ACTIVE_KEY_ID, newKey.getKeyID());
        if (Boolean.TRUE.equals(activated)) {
            log.info("Created first RSA signing key {}", newKey.getKeyID());
            return newKey;
        }

        stringRedisTemplate.opsForSet().remove(KEY_IDS, newKey.getKeyID());
        stringRedisTemplate.delete(keyPairEntries(newKey.getKeyID()));
        String activeKeyId = stringRedisTemplate.opsForValue().get(ACTIVE_KEY_ID);
        if (activeKeyId != null) {
            RSAKey activeKey = getKeyPair(activeKeyId);
            if (activeKey != null) {
                return activeKey;
            }
        }
        throw new IllegalStateException("Another instance created the active key pair, but it cannot be read");
    }

    /**
     * Generates a new RSA key pair, stores it without expiration and registers it in the key set.
     *
     * @return RSAKey the generated RSA key pair
     * @throws IllegalStateException if key pair generation fails
     */
    private RSAKey generateAndStoreKeyPair() {
        try {
            String keyId = UUID.randomUUID().toString();
            KeyPair keyPair = generateRSAKeyPair();
            RSAKey rsaKey = rsaKeyBuilder(keyId, (RSAPublicKey) keyPair.getPublic())
                    .privateKey((RSAPrivateKey) keyPair.getPrivate())
                    .build();

            storeKeyPairSecurely(keyId, rsaKey);
            stringRedisTemplate.opsForSet().add(KEY_IDS, keyId);
            log.info("Generated and stored new RSA key pair with ID: {}", keyId);
            return rsaKey;
        } catch (Exception ex) {
            throw new IllegalStateException("Key pair generation failed", ex);
        }
    }

    /**
     * Schedules a key pair for deletion once no token signed with it can still be valid.
     * <p>
     * The private key is kept for the same period: an instance that read it just before the rotation may
     * still sign one last token with it, and that token remains verifiable. The AES key that an earlier version
     * may have stored for it is deleted right away.
     * </p>
     *
     * @param keyId the key pair identifier
     */
    private void retireKeyPair(String keyId) {
        long longestTokenLifetimeMs = Math.max(
                jwtProperties.getAccessExpirationMs(), jwtProperties.getRefreshExpirationMs());
        Duration retention = Duration.ofMillis(longestTokenLifetimeMs).plus(RETIRED_KEY_GRACE_PERIOD);
        keyPairEntries(keyId).forEach(entry -> stringRedisTemplate.expire(entry, retention));
        stringRedisTemplate.delete(LEGACY_ENCRYPTION_KEY_PREFIX + keyId);
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
     * Stores an RSA key pair, with its private key encrypted.
     *
     * @param keyId the key pair identifier
     * @param rsaKey the RSA key to store
     * @throws Exception if key storage operations fail
     */
    private void storeKeyPairSecurely(String keyId, RSAKey rsaKey) throws Exception {
        String encryptedPrivateKey = privateKeyCipher.encrypt(keyId, rsaKey.toRSAPrivateKey().getEncoded());
        stringRedisTemplate.opsForValue().set(privateKeyEntry(keyId), encryptedPrivateKey);

        String publicKeyEncoded = Base64.getEncoder().encodeToString(rsaKey.toRSAPublicKey().getEncoded());
        stringRedisTemplate.opsForValue().set(publicKeyEntry(keyId), publicKeyEncoded);
    }

    /**
     * Decodes a Base64-encoded X.509 RSA public key.
     *
     * @param encodedKey the Base64-encoded public key
     * @return RSAPublicKey the decoded public key
     * @throws GeneralSecurityException if the key is not a valid X.509 RSA public key
     */
    private static RSAPublicKey decodePublicKey(String encodedKey) throws GeneralSecurityException {
        byte[] keyBytes = Base64.getDecoder().decode(encodedKey);
        return (RSAPublicKey) KeyFactory.getInstance(RSA_ALGORITHM)
                .generatePublic(new X509EncodedKeySpec(keyBytes));
    }

    /**
     * Starts building the JWK representation of a signing key.
     *
     * @param keyId the key pair identifier
     * @param publicKey the RSA public key
     * @return RSAKey.Builder a builder for an RS256 signature key
     */
    private static RSAKey.Builder rsaKeyBuilder(String keyId, RSAPublicKey publicKey) {
        return new RSAKey.Builder(publicKey)
                .keyID(keyId)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256);
    }

    private static List<String> keyPairEntries(String keyId) {
        return List.of(publicKeyEntry(keyId), privateKeyEntry(keyId));
    }

    private static String publicKeyEntry(String keyId) {
        return KEY_PAIR_PREFIX + keyId + ":public";
    }

    private static String privateKeyEntry(String keyId) {
        return KEY_PAIR_PREFIX + keyId + ":private";
    }
}
