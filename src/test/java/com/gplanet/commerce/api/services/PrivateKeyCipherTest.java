package com.gplanet.commerce.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.gplanet.commerce.api.configs.app.props.KeyEncryptionProperties;

/**
 * Unit tests for {@link PrivateKeyCipher}: private keys are encrypted with AES-GCM under a key derived from the
 * configured secret, which is required (KEY-3).
 */
class PrivateKeyCipherTest {

    private static final String KEY_ID = "key-id";
    private static final String SECRET = "test-secret-of-the-private-key-cipher";
    private static final byte[] PRIVATE_KEY = "encoded private key".getBytes(StandardCharsets.UTF_8);

    // Deriving the encryption key is deliberately slow, so the tests share these ciphers
    private static final PrivateKeyCipher CIPHER = privateKeyCipher(SECRET);
    private static final PrivateKeyCipher OTHER_SECRET_CIPHER = privateKeyCipher("another-secret-of-the-private-key");

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(KeyEncryptionProperties.class, PrivateKeyCipher.class);

    private static PrivateKeyCipher privateKeyCipher(String secret) {
        KeyEncryptionProperties properties = new KeyEncryptionProperties();
        properties.setSecret(secret);
        return new PrivateKeyCipher(properties);
    }

    @Test
    void decryptReturnsTheEncryptedPrivateKey() throws Exception {
        String encrypted = CIPHER.encrypt(KEY_ID, PRIVATE_KEY);

        assertThat(CIPHER.decrypt(KEY_ID, encrypted)).isEqualTo(PRIVATE_KEY);
    }

    @Test
    void cipherWithTheSameSecretDecryptsThePrivateKey() throws Exception {
        String encrypted = CIPHER.encrypt(KEY_ID, PRIVATE_KEY);

        assertThat(privateKeyCipher(SECRET).decrypt(KEY_ID, encrypted)).isEqualTo(PRIVATE_KEY);
    }

    @Test
    void encryptUsesARandomIv() throws Exception {
        assertThat(CIPHER.encrypt(KEY_ID, PRIVATE_KEY)).isNotEqualTo(CIPHER.encrypt(KEY_ID, PRIVATE_KEY));
    }

    @Test
    void decryptWithAnotherSecretFails() throws Exception {
        String encrypted = CIPHER.encrypt(KEY_ID, PRIVATE_KEY);

        assertThatThrownBy(() -> OTHER_SECRET_CIPHER.decrypt(KEY_ID, encrypted))
                .isInstanceOf(GeneralSecurityException.class);
    }

    @Test
    void decryptForAnotherKeyIdFails() throws Exception {
        String encrypted = CIPHER.encrypt(KEY_ID, PRIVATE_KEY);

        assertThatThrownBy(() -> CIPHER.decrypt("another-key-id", encrypted))
                .isInstanceOf(GeneralSecurityException.class);
    }

    @Test
    void decryptModifiedPrivateKeyFails() throws Exception {
        byte[] encrypted = Base64.getDecoder().decode(CIPHER.encrypt(KEY_ID, PRIVATE_KEY));
        encrypted[encrypted.length / 2] ^= 1;
        String modified = Base64.getEncoder().encodeToString(encrypted);

        assertThatThrownBy(() -> CIPHER.decrypt(KEY_ID, modified))
                .isInstanceOf(GeneralSecurityException.class);
    }

    @Test
    void decryptTruncatedPrivateKeyFails() {
        String truncated = Base64.getEncoder().encodeToString(new byte[8]);

        assertThatThrownBy(() -> CIPHER.decrypt(KEY_ID, truncated))
                .isInstanceOf(GeneralSecurityException.class);
    }

    @Test
    void startupFailsWithoutSecret() {
        contextRunner.run(context -> assertThat(context).getFailure()
                .rootCause().hasMessageContaining("secret").hasMessageContaining("must be set"));
    }

    @Test
    void startupFailsWithShortSecret() {
        contextRunner.withPropertyValues("oauth2.key-encryption.secret=too-short")
                .run(context -> assertThat(context).getFailure()
                        .rootCause().hasMessageContaining("secret").hasMessageContaining("at least 32 characters"));
    }

    @Test
    void startupSucceedsWithSecret() {
        contextRunner.withPropertyValues("oauth2.key-encryption.secret=" + SECRET)
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(PrivateKeyCipher.class));
    }
}
