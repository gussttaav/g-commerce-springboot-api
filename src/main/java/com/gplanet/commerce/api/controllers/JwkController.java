package com.gplanet.commerce.api.controllers;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gplanet.commerce.api.configs.app.props.JwtProperties;
import com.gplanet.commerce.api.dtos.jwks.JwkDTO;
import com.gplanet.commerce.api.dtos.jwks.JwksResponseDTO;
import com.gplanet.commerce.api.dtos.jwks.OpenIdConfigurationDTO;
import com.gplanet.commerce.api.services.KeyStoreService;
import com.nimbusds.jose.jwk.RSAKey;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for exposing JSON Web Key Set (JWKS) and OpenID Connect configuration.
 * 
 * <p>This controller handles cryptographic key management and OpenID Connect discovery:
 * <ul>
 *   <li>Exposes public keys for JWT verification in standard JWKS format</li>
 *   <li>Provides OpenID Provider configuration following RFC 8414</li>
 *   <li>Supports token validation by external services and OAuth 2.0 clients</li>
 *   <li>Implements standard discovery endpoints for automated client configuration</li>
 * </ul>
 * </p>
 * 
 * <p>All endpoints in this controller are public and do not require authentication.
 * These endpoints follow standard discovery patterns for OpenID Connect and are
 * commonly used by OAuth 2.0 clients for automatic configuration.</p>
 *
 * @author Gustavo
 * @version 1.0
 */
@Slf4j
@RestController
@Tag(name = "JWKS & Discovery", 
     description = "JSON Web Key Set and OpenID Connect discovery endpoints for JWT verification")
@RequiredArgsConstructor
public class JwkController {

    private final KeyStoreService keyStoreService;
    private final JwtProperties jwtProperties;

    /**
     * Exposes the JWKS (JSON Web Key Set) with the public RSA keys that tokens may be signed with.
     *
     * <p>This endpoint provides the public key information needed by clients and external
     * services to verify JWT signatures. The response follows RFC 7517 (JSON Web Key)
     * format and includes the active signing key first, followed by keys retired by a rotation
     * that still have unexpired tokens.</p>
     *
     * <p>Clients should cache this information but be prepared to refetch it when a token
     * carries an unknown key ID, as keys are rotated periodically for security.</p>
     *
     * @return JwksResponseDTO containing the public RSA keys in JWKS format
     * @throws IllegalStateException if key retrieval fails
     */
    @SecurityRequirements
    @Operation(summary = "Get JSON Web Key Set",
               description = "Returns the public RSA keys in JWKS format for JWT signature verification. "
                           + "This endpoint follows RFC 7517 standard and is used by external services and clients "
                           + "to validate JWT tokens issued by this authorization server. "
                           + "The active signing key is listed first, followed by recently rotated keys that "
                           + "still have valid tokens. Each key includes its key ID, algorithm, and RSA parameters.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "JWKS retrieved successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = JwksResponseDTO.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error - failed to retrieve or generate JWKS")
    })
    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public JwksResponseDTO jwks() {
        try {
            List<JwkDTO> keys = keyStoreService.getPublicKeys().stream()
                .map(this::toJwkDto)
                .toList();
            if (keys.isEmpty()) {
                log.warn("No RSA key available for JWKS");
            }

            log.debug("Serving JWKS with {} key(s)", keys.size());
            return new JwksResponseDTO(keys);

        } catch (Exception e) {
            log.error("Failed to generate JWKS response", e);
            throw new IllegalStateException("Failed to retrieve JWKS", e);
        }
    }

    /**
     * Provides OpenID Connect Provider configuration document.
     * 
     * <p>This endpoint returns authorization server metadata according to RFC 8414 
     * (OAuth 2.0 Authorization Server Metadata) and OpenID Connect Discovery 1.0 
     * specification. It provides essential configuration information that OAuth 2.0 
     * and OpenID Connect clients need to interact with this authorization server.</p>
     * 
     * <p>The configuration includes supported grant types, token endpoints, and 
     * algorithm information needed for proper client configuration and token validation.</p>
     *
     * @return OpenIdConfigurationDTO containing OpenID Provider metadata
     */
    @SecurityRequirements
    @Operation(summary = "Get OpenID Connect configuration", 
               description = "Returns OpenID Provider metadata according to RFC 8414 (OAuth 2.0 Authorization "
                           + "Server Metadata) and OpenID Connect Discovery 1.0 specification. This endpoint "
                           + "provides essential configuration information for OAuth 2.0 and OpenID Connect clients, "
                           + "including supported grant types, token endpoints, and cryptographic algorithms.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OpenID configuration retrieved successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = OpenIdConfigurationDTO.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error - failed to generate configuration")
    })
    @GetMapping(value = "/.well-known/openid-configuration", produces = MediaType.APPLICATION_JSON_VALUE)
    public OpenIdConfigurationDTO openidConfiguration() {
        String issuer = jwtProperties.getIssuer();
        String jwksUri = "/.well-known/jwks.json";
        
        log.debug("Serving OpenID configuration for issuer: {}", issuer);
        
        return new OpenIdConfigurationDTO(
            issuer,
            jwksUri,
            "/api/auth/login",
            new String[] {"password", "refresh_token"},
            new String[] {"RS256"}
        );
    }

    /**
     * Converts an RSA key to its public JWK representation.
     *
     * @param rsaKey the RSA key; only its public parameters are exposed
     * @return the JWK DTO
     */
    private JwkDTO toJwkDto(RSAKey rsaKey) {
        RSAKey publicKey = rsaKey.toPublicJWK();
        return new JwkDTO(
            publicKey.getKeyType().getValue(),    // kty
            resolveKeyUse(publicKey),             // use
            publicKey.getKeyID(),                 // kid
            resolveAlgorithm(publicKey),          // alg
            publicKey.getModulus().toString(),    // n
            publicKey.getPublicExponent().toString() // e
        );
    }

    /**
     * Resolves the JWK "use" (key use) value from the given RSA public key.
     * 
     * <p>If the key does not explicitly define a use, this method defaults to
     * {@code "sig"} (signature), which is the common value for signing keys.</p>
     *
     * @param publicKey the RSA public key from which to resolve the key use
     * @return the key use value, never {@code null}
     */
    private String resolveKeyUse(RSAKey publicKey) {
        if (publicKey.getKeyUse() == null) {
            return "sig";
        }
        return publicKey.getKeyUse().getValue();
    }

    /**
     * Resolves the JWK "alg" (algorithm) value from the given RSA public key.
     *
     * <p>If the key does not explicitly define an algorithm, this method defaults to
     * {@code "RS256"} (RSA Signature with SHA-256).</p>
     *
     * @param publicKey the RSA public key from which to resolve the algorithm
     * @return the algorithm name, never {@code null}
     */
    private String resolveAlgorithm(RSAKey publicKey) {
        if (publicKey.getAlgorithm() == null) {
            return "RS256";
        }
        return publicKey.getAlgorithm().getName();
    }
}
