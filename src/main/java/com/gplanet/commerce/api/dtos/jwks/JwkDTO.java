package com.gplanet.commerce.api.dtos.jwks;

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
@Schema(description = "JSON Web Key containing RSA public key information")
public record JwkDTO(
    @Schema(description = "Key type", example = "RSA", required = true)
    String kty,
    
    @Schema(description = "Public key use", example = "sig", required = true)
    String use,
    
    @Schema(description = "Key ID - unique identifier for this key", 
            example = "12345678-1234-1234-1234-123456789012", required = true)
    String kid,
    
    @Schema(description = "Algorithm intended for use with this key", 
            example = "RS256", required = true)
    String alg,
    
    @Schema(description = "RSA public key modulus (Base64url encoded)", 
            example = "0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx...", 
            required = true)
    String n,
    
    @Schema(description = "RSA public key exponent (Base64url encoded)", 
            example = "AQAB", required = true)
    String e
) {
}
