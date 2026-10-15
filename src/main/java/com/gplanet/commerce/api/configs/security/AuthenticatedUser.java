package com.gplanet.commerce.api.configs.security;

import org.springframework.security.core.Authentication;

/**
 * Identifies the user that authenticated a request.
 * <p>
 * Access tokens identify the user by ID in their subject, which is the name of the authentication of the request,
 * so the name of an authenticated request is the ID of its user.
 * </p>
 *
 * @author Gustavo
 * @version 1.0
 */
public final class AuthenticatedUser {

    private AuthenticatedUser() {
    }

    /**
     * Returns the ID of the user that authenticated a request.
     *
     * @param authentication the authentication of the request
     * @return the ID of the authenticated user
     */
    public static Long idOf(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }
}
