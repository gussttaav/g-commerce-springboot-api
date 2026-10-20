package com.gplanet.commerce.api.dtos.jwks;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO record representing OpenID Connect Provider configuration.
 * Contains metadata about the OAuth 2.0/OpenID Connect authorization server.
 *
 * @param issuer The authorization server's issuer identifier
 * @param jwks_uri URL of the authorization server's JWK Set document
 * @param token_endpoint URL of the token endpoint
 * @param grant_types_supported Grant types supported by the authorization server
 * @param id_token_signing_alg_values_supported JWS signing algorithms supported for ID Tokens
 * 
 * @author Gustavo
 * @version 1.0
 */
@Schema(name = "OpenIdConfiguration", description = "OpenID provider metadata (RFC 8414)")
public record OpenIdConfigurationDTO(
    @Schema(description = "Authorization server's issuer identifier", 
            example = "g-commerce-api", requiredMode = REQUIRED)
    String issuer,
    
    @Schema(description = "URL of the authorization server's JWK Set document", 
            example = "/.well-known/jwks.json", requiredMode = REQUIRED)
    String jwks_uri,
    
    @Schema(description = "URL of the token endpoint", 
            example = "/api/auth/login", requiredMode = REQUIRED)
    String token_endpoint,
    
    @Schema(description = "Grant types supported by the authorization server", 
            example = "[\"password\", \"refresh_token\"]", requiredMode = REQUIRED)
    String[] grant_types_supported,
    
    @Schema(description = "JWS signing algorithms supported for ID Tokens", 
            example = "[\"RS256\"]", requiredMode = REQUIRED)
    String[] id_token_signing_alg_values_supported
) {
}
