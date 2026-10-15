package com.gplanet.commerce.api.configs.app.props;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Configuration properties for the encryption of the RSA private keys stored in Redis, bound from the properties
 * prefixed with <code>oauth2.key-encryption</code>.
 *
 * <ul>
 *   <li><b>secret</b>: the secret from which the AES key that encrypts the private keys is derived. It is never
 *   stored in Redis, so a copy of the Redis data does not reveal the private keys. Every instance must use the same
 *   secret; changing it makes the stored private keys unreadable, which rotates the signing key.</li>
 * </ul>
 *
 * @author Gustavo
 * @version 1.0
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "oauth2.key-encryption")
public class KeyEncryptionProperties {

    /**
     * Minimum length of the secret, in characters. A random 256-bit key encoded in Base64 (e.g. generated with
     * {@code openssl rand -base64 32}) has 44.
     */
    public static final int MIN_SECRET_LENGTH = 32;

    /**
     * Secret from which the key that encrypts the RSA private keys is derived.
     */
    @NotBlank(message = "must be set (OAUTH2_KEY_ENCRYPTION_SECRET), e.g. to the output of: openssl rand -base64 32")
    @Size(min = MIN_SECRET_LENGTH, message = "must be at least {min} characters long")
    private String secret;
}
