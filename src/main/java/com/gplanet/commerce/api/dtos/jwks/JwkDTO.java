package com.gplanet.commerce.api.dtos.jwks;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO record representing a single JSON Web Key (JWK).
 * Contains the public key information in JWK format.
 *
 * @param kty Key type (e.g., "RSA")
 * @param use Public key use (e.g., "sig" for signature)
 * @param kid Key ID - unique identifier for this key
 * @param alg Algorithm intended for use with this key (e.g., "RS256")
 * @param n RSA public key modulus (Base64url encoded)
 * @param e RSA public key exponent (Base64url encoded)
 * 
 * @author Gustavo
 * @version 1.0
 */
@Schema(name = "Jwk", description = "A public RSA key that verifies the signature of the tokens")
public record JwkDTO(
    @Schema(description = "Key type", example = "RSA", requiredMode = REQUIRED)
    String kty,
    
    @Schema(description = "Public key use", example = "sig", requiredMode = REQUIRED)
    String use,
    
    @Schema(description = "Key ID - unique identifier for this key", 
            example = "12345678-1234-1234-1234-123456789012", requiredMode = REQUIRED)
    String kid,
    
    @Schema(description = "Algorithm intended for use with this key", 
            example = "RS256", requiredMode = REQUIRED)
    String alg,
    
    @Schema(description = "RSA public key modulus (Base64url encoded)", 
            example = "0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx...", 
            requiredMode = REQUIRED)
    String n,
    
    @Schema(description = "RSA public key exponent (Base64url encoded)", 
            example = "AQAB", requiredMode = REQUIRED)
    String e
) {
}
