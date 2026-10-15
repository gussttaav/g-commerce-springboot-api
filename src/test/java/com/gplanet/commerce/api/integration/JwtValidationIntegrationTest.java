package com.gplanet.commerce.api.integration;

import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_AUTH_VALIDATE;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_USERS_ME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import com.gplanet.commerce.api.configs.app.props.JwtProperties;
import com.gplanet.commerce.api.entities.User;
import com.gplanet.commerce.api.services.KeyStoreService;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * Integration tests for the checks applied to incoming access tokens: signature key, timestamps, issuer and
 * audience (SEC-7), token type (SEC-2), token ID (SEC-1) and subject (FUN-5). Each case also checks that
 * {@code /api/auth/validate} gives the same verdict as the resource server (SEC-4).
 */
class JwtValidationIntegrationTest extends AbstractIntegrationTest {

    private static final String USER_EMAIL = "claims@example.com";
    private static final String USER_PASSWORD = "Password123!";
    private static final int RSA_KEY_SIZE = 2048;

    @Autowired
    private KeyStoreService keyStoreService;

    @Autowired
    private JwtProperties jwtProperties;

    private Long userId;

    @BeforeEach
    void createUser() {
        userId = createUser("Claims User", USER_EMAIL, USER_PASSWORD, User.Role.USER).getId();
    }

    @Test
    void tokenWithExpectedIssuerAndAudienceIsAccepted() throws Exception {
        assertAccepted(sign(validClaims().build(), activeKey()));
    }

    @Test
    void tokenFromAnotherIssuerIsRejected() throws Exception {
        JWTClaimsSet claims = validClaims().issuer("https://another-issuer.example.com").build();

        assertRejected(sign(claims, activeKey()));
    }

    @Test
    void tokenForAnotherAudienceIsRejected() throws Exception {
        JWTClaimsSet claims = validClaims().audience("another-api").build();

        assertRejected(sign(claims, activeKey()));
    }

    @Test
    void tokenWithoutAudienceIsRejected() throws Exception {
        JWTClaimsSet claims = validClaims().audience((String) null).build();

        assertRejected(sign(claims, activeKey()));
    }

    @Test
    void tokenSignedWithUnknownKeyIsRejected() throws Exception {
        RSAKey unknownKey = new RSAKeyGenerator(RSA_KEY_SIZE).keyID(UUID.randomUUID().toString()).generate();

        assertRejected(sign(validClaims().build(), unknownKey));
    }

    @Test
    void tokenWithTamperedClaimsIsRejected() throws Exception {
        SignedJWT original = SignedJWT.parse(sign(validClaims().build(), activeKey()));
        SignedJWT tampered = SignedJWT.parse(
                sign(validClaims().claim("authorities", List.of("ROLE_ADMIN")).build(), activeKey()));

        // The admin claims with the signature of the original token
        SignedJWT forged = new SignedJWT(tampered.getHeader().toBase64URL(), tampered.getPayload().toBase64URL(),
                original.getSignature());

        assertRejected(forged.serialize());
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        Instant issuedAt = Instant.now().minus(Duration.ofHours(2));
        JWTClaimsSet claims = validClaims()
                .issueTime(Date.from(issuedAt))
                .notBeforeTime(Date.from(issuedAt))
                .expirationTime(Date.from(issuedAt.plus(Duration.ofHours(1))))
                .build();

        assertRejected(sign(claims, activeKey()));
    }

    @Test
    void refreshTokenIsRejected() throws Exception {
        JWTClaimsSet claims = validClaims().claim("token_type", "refresh").build();

        assertRejected(sign(claims, activeKey()));
    }

    @Test
    void tokenWithEmailAsSubjectIsRejected() throws Exception {
        // Tokens issued before tokens identified users by ID; the email may belong to another user by now
        JWTClaimsSet claims = validClaims().subject(USER_EMAIL).build();

        assertRejected(sign(claims, activeKey()));
    }

    @Test
    void tokenWithoutIdIsRejected() throws Exception {
        // Tokens are revoked by ID, so a token without one could never be revoked
        JWTClaimsSet claims = validClaims().jwtID(null).build();

        assertRejected(sign(claims, activeKey()));
    }

    private void assertAccepted(String token) throws Exception {
        mockMvc.perform(get(ENDPOINT_USERS_ME).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get(ENDPOINT_AUTH_VALIDATE).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    private void assertRejected(String token) throws Exception {
        mockMvc.perform(get(ENDPOINT_USERS_ME).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(ENDPOINT_AUTH_VALIDATE).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    private RSAKey activeKey() {
        return keyStoreService.getOrCreateActiveKeyPair();
    }

    private JWTClaimsSet.Builder validClaims() {
        Instant now = Instant.now();
        return new JWTClaimsSet.Builder()
                .subject(userId.toString())
                .issuer(jwtProperties.getIssuer())
                .audience(jwtProperties.getAudience())
                .issueTime(Date.from(now))
                .notBeforeTime(Date.from(now))
                .expirationTime(Date.from(now.plus(Duration.ofMinutes(5))))
                .jwtID(UUID.randomUUID().toString())
                .claim("authorities", List.of("ROLE_USER"));
    }

    private String sign(JWTClaimsSet claims, RSAKey key) throws JOSEException {
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(key.getKeyID())
                .type(JOSEObjectType.JWT)
                .build();
        SignedJWT jwt = new SignedJWT(header, claims);
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }
}
