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
 *   <li><b>accessExpirationMs</b>: The validity period of access tokens in milliseconds.</li>
 *   <li><b>refreshExpirationMs</b>: The validity period of refresh tokens in milliseconds.</li>
 *   <li><b>issuer</b>: The issuer claim to be set in, and required on, JWT tokens.</li>
 *   <li><b>audience</b>: The audience claim to be set in, and required on, JWT tokens.</li>
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
     * Access token expiration (milliseconds).
     */
    private long accessExpirationMs;

    /**
     * Refresh token expiration (milliseconds).
     */
    private long refreshExpirationMs;

    /**
     * The issuer to be included in the JWT token's claims.
     */
    private String issuer;

    /**
     * The audience to be included in the JWT token's claims.
     */
    private String audience = "g-commerce-api";
}
