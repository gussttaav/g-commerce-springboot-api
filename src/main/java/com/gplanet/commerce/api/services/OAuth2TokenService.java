package com.gplanet.commerce.api.services;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.gplanet.commerce.api.configs.app.props.JwtProperties;
import com.gplanet.commerce.api.entities.User;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service class that handles JWT token generation for OAuth2 authentication.
 * Generates both access and refresh tokens using RSA cryptography.
 * <p>
 * Tokens identify the user by ID in the {@code sub} claim. Unlike the email, the ID never changes or passes to
 * another user, so a token keeps belonging to the same user after they change their email.
 * </p>
 * 
 * @author Gustavo
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2TokenService {

    /**
     * Claim that marks refresh tokens. Access tokens don't carry it.
     */
    public static final String TOKEN_TYPE_CLAIM = "token_type";

    /**
     * Value of {@link #TOKEN_TYPE_CLAIM} in refresh tokens.
     */
    public static final String REFRESH_TOKEN_TYPE = "refresh";

    /**
     * Claim of access tokens with the user's email when the token was issued.
     */
    public static final String EMAIL_CLAIM = "email";

    private final KeyStoreService keyStoreService;
    private final JwtProperties jwtProperties;

    /**
     * Generates a JWT access token for authenticated users.
     * Includes the user's email and authorities and standard JWT claims.
     * 
     * @param user the authenticated user
     * @return String the serialized JWT access token
     * @throws IllegalStateException if token generation fails
     */
    public String generateAccessToken(User user) {
        try {
            RSAKey activeKey = keyStoreService.getOrCreateActiveKeyPair();
            Instant now = Instant.now();
            Instant exp = now.plusMillis(jwtProperties.getAccessExpirationMs());

            List<String> authorities = List.of(user.getRole().authority());

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(user.getId().toString())
                    .issuer(jwtProperties.getIssuer())
                    .audience(jwtProperties.getAudience())
                    .expirationTime(Date.from(exp))
                    .notBeforeTime(Date.from(now))
                    .issueTime(Date.from(now))
                    .jwtID(UUID.randomUUID().toString())
                    .claim(EMAIL_CLAIM, user.getEmail())
                    .claim("scope", String.join(" ", authorities))
                    .claim("authorities", authorities)
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new com.nimbusds.jose.JWSHeader.Builder(JWSAlgorithm.RS256)
                            .keyID(activeKey.getKeyID())
                            .type(JOSEObjectType.JWT)
                            .build(),
                    claimsSet
            );

            JWSSigner signer = new RSASSASigner(activeKey);
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (Exception e) {
            log.error("Failed to generate access token", e);
            throw new IllegalStateException("Token generation failed", e);
        }
    }


    /**
     * Generates a JWT refresh token for token renewal.
     * Includes basic claims without user authorities for security.
     * 
     * @param user the authenticated user
     * @return String the serialized JWT refresh token
     * @throws IllegalStateException if token generation fails
     */
    public String generateRefreshToken(User user) {
        try {
            RSAKey activeKey = keyStoreService.getOrCreateActiveKeyPair();
            Instant now = Instant.now();
            Instant exp = now.plusMillis(jwtProperties.getRefreshExpirationMs());

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(user.getId().toString())
                    .issuer(jwtProperties.getIssuer())
                    .audience(jwtProperties.getAudience())
                    .expirationTime(Date.from(exp))
                    .notBeforeTime(Date.from(now))
                    .issueTime(Date.from(now))
                    .jwtID(UUID.randomUUID().toString())
                    .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new com.nimbusds.jose.JWSHeader.Builder(JWSAlgorithm.RS256)
                            .keyID(activeKey.getKeyID())
                            .type(JOSEObjectType.JWT)
                            .build(),
                    claimsSet
            );

            JWSSigner signer = new RSASSASigner(activeKey);
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (Exception e) {
            log.error("Failed to generate refresh token", e);
            throw new IllegalStateException("Refresh token generation failed", e);
        }
    }
}


