package com.gplanet.commerce.api.configs.security.ratelimiting;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import com.gplanet.commerce.api.configs.app.props.RateLimitProperties;
import com.gplanet.commerce.api.configs.app.props.RateLimitProperties.Limit;

import io.github.bucket4j.Bucket;

class RateLimitBucketResolverTest {

    private static final String CLIENT_ADDRESS = "10.0.0.1";
    private static final String OTHER_CLIENT_ADDRESS = "10.0.0.2";
    private static final String USER_EMAIL = "user@example.com";
    private static final int ANONYMOUS_CAPACITY = 10;
    private static final int USER_CAPACITY = 30;
    private static final int ADMIN_CAPACITY = 100;
    private static final int LOGIN_CAPACITY = 5;
    // The login limit refills one request per minute, so an empty login bucket takes 5 minutes to refill
    private static final Duration LONGEST_REFILL_TIME = Duration.ofMinutes(LOGIN_CAPACITY);

    private final AtomicLong nanoTime = new AtomicLong();
    private RateLimitBucketResolver resolver;

    @BeforeEach
    void setUp() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setWindowMinutes(1);
        properties.setUnauthenticated(new Limit(ANONYMOUS_CAPACITY, 5));
        properties.setUser(new Limit(USER_CAPACITY, 20));
        properties.setAdmin(new Limit(ADMIN_CAPACITY, 50));
        properties.setLogin(new Limit(LOGIN_CAPACITY, 1));
        resolver = new RateLimitBucketResolver(properties, nanoTime::get);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void anonymousRequestsAreLimitedPerIpAddress() {
        authenticate(new AnonymousAuthenticationToken("key", "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        Bucket bucket = resolver.resolveBucket(request(CLIENT_ADDRESS));

        assertThat(bucket.getAvailableTokens()).isEqualTo(ANONYMOUS_CAPACITY);
        assertThat(resolver.resolveBucket(request(CLIENT_ADDRESS))).isSameAs(bucket);
        assertThat(resolver.resolveBucket(request(OTHER_CLIENT_ADDRESS))).isNotSameAs(bucket);
    }

    @Test
    void requestsWithoutAuthenticationAreLimitedPerIpAddress() {
        Bucket bucket = resolver.resolveBucket(request(CLIENT_ADDRESS));

        assertThat(bucket.getAvailableTokens()).isEqualTo(ANONYMOUS_CAPACITY);
        assertThat(resolver.resolveBucket(request(OTHER_CLIENT_ADDRESS))).isNotSameAs(bucket);
    }

    @Test
    void usersAreLimitedPerUserWithTheUserLimit() {
        Bucket anonymousBucket = resolver.resolveBucket(request(CLIENT_ADDRESS));
        authenticate(user("ROLE_USER"));

        Bucket bucket = resolver.resolveBucket(request(CLIENT_ADDRESS));

        assertThat(bucket.getAvailableTokens()).isEqualTo(USER_CAPACITY);
        assertThat(bucket).isNotSameAs(anonymousBucket);
        assertThat(resolver.resolveBucket(request(OTHER_CLIENT_ADDRESS))).isSameAs(bucket);
    }

    @Test
    void adminsAreLimitedWithTheAdminLimit() {
        authenticate(user("ROLE_ADMIN"));

        Bucket bucket = resolver.resolveBucket(request(CLIENT_ADDRESS));

        assertThat(bucket.getAvailableTokens()).isEqualTo(ADMIN_CAPACITY);
    }

    @Test
    void roleChangeAppliesTheLimitOfTheNewRole() {
        authenticate(user("ROLE_USER"));
        Bucket userBucket = resolver.resolveBucket(request(CLIENT_ADDRESS));
        userBucket.tryConsume(USER_CAPACITY);

        authenticate(user("ROLE_ADMIN"));
        Bucket adminBucket = resolver.resolveBucket(request(CLIENT_ADDRESS));

        assertThat(adminBucket.getAvailableTokens()).isEqualTo(ADMIN_CAPACITY);
    }

    @Test
    void loginAttemptsHaveTheirOwnLimitPerIpAddress() {
        Bucket bucket = resolver.resolveLoginBucket(request(CLIENT_ADDRESS));

        assertThat(bucket.getAvailableTokens()).isEqualTo(LOGIN_CAPACITY);
        assertThat(resolver.resolveLoginBucket(request(CLIENT_ADDRESS))).isSameAs(bucket);
        assertThat(resolver.resolveLoginBucket(request(OTHER_CLIENT_ADDRESS))).isNotSameAs(bucket);
        assertThat(resolver.resolveBucket(request(CLIENT_ADDRESS))).isNotSameAs(bucket);
    }

    @Test
    void unusedBucketIsKeptUntilItCouldHaveRefilled() {
        Bucket bucket = resolver.resolveLoginBucket(request(CLIENT_ADDRESS));
        bucket.tryConsume(LOGIN_CAPACITY);

        nanoTime.addAndGet(LONGEST_REFILL_TIME.minusSeconds(1).toNanos());

        assertThat(resolver.resolveLoginBucket(request(CLIENT_ADDRESS))).isSameAs(bucket);
    }

    @Test
    void unusedBucketIsEvictedOnceItCouldHaveRefilled() {
        Bucket bucket = resolver.resolveLoginBucket(request(CLIENT_ADDRESS));

        nanoTime.addAndGet(LONGEST_REFILL_TIME.toNanos());

        assertThat(resolver.resolveLoginBucket(request(CLIENT_ADDRESS))).isNotSameAs(bucket);
    }

    private static Authentication user(String role) {
        return UsernamePasswordAuthenticationToken.authenticated(USER_EMAIL, null,
                AuthorityUtils.createAuthorityList(role));
    }

    private static void authenticate(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private static MockHttpServletRequest request(String clientAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(clientAddress);
        return request;
    }
}
