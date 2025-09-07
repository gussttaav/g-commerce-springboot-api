package com.gplanet.commerce.api.configs.app.props;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Configuration properties for JWT (JSON Web Token) settings.
 * <p>
 * This class binds properties prefixed with <code>jwt</code> from the application's configuration files.
 * It provides the secret key, token expiration time, and issuer for JWT operations.
 * </p>
 *
 * <ul>
 *   <li><b>secret</b>: The secret key used to sign and validate JWT tokens.</li>
 *   <li><b>expirationMs</b>: The validity period of the JWT token in milliseconds.</li>
 *   <li><b>issuer</b>: The issuer claim to be set in the JWT token.</li>
 * </ul>
 * 
 * @author Gustavo
 * @version 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    /**
     * The secret key used for signing and verifying JWT tokens.
     */
    private String secret;

    /**
     * The expiration time (in milliseconds) for the JWT token.
     */
    private long expirationMs;

    /**
     * The issuer to be included in the JWT token's claims.
     */
    private String issuer;
}
