package com.gplanet.commerce.api.integration;

import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_USUARIOS_ME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
 * Integration tests for the checks the resource server applies to incoming access tokens:
 * signature key, issuer and audience (SEC-7), token type (SEC-2) and token ID (SEC-1).
 */
class JwtValidationIntegrationTest extends AbstractIntegrationTest {

    private static final String USER_EMAIL = "claims@example.com";
    private static final String USER_PASSWORD = "Password123!";
    private static final int RSA_KEY_SIZE = 2048;

    @Autowired
    private KeyStoreService keyStoreService;

    @Autowired
    private JwtProperties jwtProperties;

    @BeforeEach
    void createUser() {
        crearUsuario("Claims User", USER_EMAIL, USER_PASSWORD, User.Role.USER);
    }

    @Test
    void tokenWithExpectedIssuerAndAudienceIsAccepted() throws Exception {
        String token = sign(validClaims().build(), keyStoreService.getOrCreateActiveKeyPair());

        mockMvc.perform(get(ENDPOINT_USUARIOS_ME).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
    }

    @Test
    void tokenFromAnotherIssuerIsRejected() throws Exception {
        JWTClaimsSet claims = validClaims().issuer("https://another-issuer.example.com").build();
        String token = sign(claims, keyStoreService.getOrCreateActiveKeyPair());

        mockMvc.perform(get(ENDPOINT_USUARIOS_ME).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenForAnotherAudienceIsRejected() throws Exception {
        JWTClaimsSet claims = validClaims().audience("another-api").build();
        String token = sign(claims, keyStoreService.getOrCreateActiveKeyPair());

        mockMvc.perform(get(ENDPOINT_USUARIOS_ME).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenWithoutAudienceIsRejected() throws Exception {
        JWTClaimsSet claims = validClaims().audience((String) null).build();
        String token = sign(claims, keyStoreService.getOrCreateActiveKeyPair());

        mockMvc.perform(get(ENDPOINT_USUARIOS_ME).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSignedWithUnknownKeyIsRejected() throws Exception {
        RSAKey unknownKey = new RSAKeyGenerator(RSA_KEY_SIZE).keyID(UUID.randomUUID().toString()).generate();
        String token = sign(validClaims().build(), unknownKey);

        mockMvc.perform(get(ENDPOINT_USUARIOS_ME).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenIsRejected() throws Exception {
        JWTClaimsSet claims = validClaims().claim("token_type", "refresh").build();
        String token = sign(claims, keyStoreService.getOrCreateActiveKeyPair());

        mockMvc.perform(get(ENDPOINT_USUARIOS_ME).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenWithoutIdIsRejected() throws Exception {
        // Tokens are revoked by ID, so a token without one could never be revoked
        JWTClaimsSet claims = validClaims().jwtID(null).build();
        String token = sign(claims, keyStoreService.getOrCreateActiveKeyPair());

        mockMvc.perform(get(ENDPOINT_USUARIOS_ME).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized());
    }

    private JWTClaimsSet.Builder validClaims() {
        Instant now = Instant.now();
        return new JWTClaimsSet.Builder()
                .subject(USER_EMAIL)
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
        return "Bearer " + jwt.serialize();
    }
}
