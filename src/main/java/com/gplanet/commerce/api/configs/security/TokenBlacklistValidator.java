package com.gplanet.commerce.api.configs.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;

import com.gplanet.commerce.api.services.TokenBlacklistService;

import lombok.RequiredArgsConstructor;

/**
 * JWT validator that rejects revoked tokens.
 * <p>
 * Tokens are revoked by their {@code jti} claim, so a token without one is rejected too: it could never be revoked.
 * </p>
 *
 * @author Gustavo
 * @version 1.0
 */
@RequiredArgsConstructor
public class TokenBlacklistValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error MISSING_TOKEN_ID =
            new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "The jti claim is missing", null);
    private static final OAuth2Error REVOKED_TOKEN =
            new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "The token has been revoked", null);

    private final TokenBlacklistService tokenBlacklistService;

    /**
     * Validates that the token has an ID and is not blacklisted.
     *
     * @param token the token to validate
     * @return the validation result
     */
    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        String tokenId = token.getId();
        if (!StringUtils.hasText(tokenId)) {
            return OAuth2TokenValidatorResult.failure(MISSING_TOKEN_ID);
        }
        if (tokenBlacklistService.isBlacklisted(tokenId)) {
            return OAuth2TokenValidatorResult.failure(REVOKED_TOKEN);
        }
        return OAuth2TokenValidatorResult.success();
    }
}
