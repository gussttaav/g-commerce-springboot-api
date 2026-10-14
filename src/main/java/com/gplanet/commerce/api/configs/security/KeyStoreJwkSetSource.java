package com.gplanet.commerce.api.configs.security;

import java.util.ArrayList;

import org.springframework.dao.DataAccessException;

import com.gplanet.commerce.api.services.KeyStoreService;
import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSetCacheRefreshEvaluator;
import com.nimbusds.jose.jwk.source.JWKSetSource;
import com.nimbusds.jose.proc.SecurityContext;

import lombok.RequiredArgsConstructor;

/**
 * Nimbus {@link JWKSetSource} that loads the public signing keys from {@link KeyStoreService}.
 * <p>
 * Caching is left to the {@link com.nimbusds.jose.jwk.source.JWKSourceBuilder} that wraps this source.
 * </p>
 *
 * @author Gustavo
 * @version 1.0
 */
@RequiredArgsConstructor
final class KeyStoreJwkSetSource implements JWKSetSource<SecurityContext> {

    private final KeyStoreService keyStoreService;

    @Override
    public JWKSet getJWKSet(JWKSetCacheRefreshEvaluator refreshEvaluator, long currentTime, SecurityContext context)
            throws KeySourceException {
        try {
            return new JWKSet(new ArrayList<JWK>(keyStoreService.getPublicKeys()));
        } catch (DataAccessException ex) {
            throw new KeySourceException("Failed to load the JWT signing keys", ex);
        }
    }

    @Override
    public void close() {
        // Nothing to release: the Redis connection is managed by Spring
    }
}
