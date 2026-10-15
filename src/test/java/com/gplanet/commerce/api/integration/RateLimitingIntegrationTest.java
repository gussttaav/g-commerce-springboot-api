package com.gplanet.commerce.api.integration;

import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_AUTH_LOGIN;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_PRODUCTS;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_USERS_ME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.gplanet.commerce.api.dtos.auth.LoginDTO;
import com.gplanet.commerce.api.entities.User;

/**
 * Integration tests for rate limiting (SEC-5): each anonymous client has its own bucket, keyed by IP address and
 * with the unauthenticated limits, login attempts have a stricter limit of their own, and users get the limits of
 * their current role.
 * <p>
 * Buckets outlive a test, so each test uses its own IP addresses and users. Buckets refill one request per hour, so
 * no request is refilled while a test runs.
 * </p>
 */
@TestPropertySource(properties = {
    "rate-limit.window-minutes=60",
    "rate-limit.unauthenticated.capacity=" + RateLimitingIntegrationTest.ANONYMOUS_CAPACITY,
    "rate-limit.unauthenticated.refill=1",
    "rate-limit.user.capacity=" + RateLimitingIntegrationTest.USER_CAPACITY,
    "rate-limit.user.refill=1",
    "rate-limit.admin.capacity=" + RateLimitingIntegrationTest.ADMIN_CAPACITY,
    "rate-limit.admin.refill=1",
    "rate-limit.login.capacity=" + RateLimitingIntegrationTest.LOGIN_CAPACITY,
    "rate-limit.login.refill=1"
})
class RateLimitingIntegrationTest extends AbstractIntegrationTest {

    static final int ANONYMOUS_CAPACITY = 3;
    static final int USER_CAPACITY = 5;
    static final int ADMIN_CAPACITY = 8;
    static final int LOGIN_CAPACITY = 2;

    private static final String REMAINING_HEADER = "X-Rate-Limit-Remaining";
    private static final String PASSWORD = "Password123!";

    @Test
    void anonymousRequestsHaveTheUnauthenticatedLimit() throws Exception {
        String client = "10.0.0.1";

        listProducts(client).andExpect(status().isOk())
                .andExpect(header().string(REMAINING_HEADER, String.valueOf(ANONYMOUS_CAPACITY - 1)));
        for (int i = 1; i < ANONYMOUS_CAPACITY; i++) {
            listProducts(client).andExpect(status().isOk());
        }

        listProducts(client).andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER));
    }

    @Test
    void anonymousClientsDoNotShareTheirLimit() throws Exception {
        String client = "10.0.0.2";
        for (int i = 0; i < ANONYMOUS_CAPACITY; i++) {
            listProducts(client).andExpect(status().isOk());
        }
        listProducts(client).andExpect(status().isTooManyRequests());

        listProducts("10.0.0.3").andExpect(status().isOk());
    }

    @Test
    void loginAttemptsHaveTheirOwnStricterLimit() throws Exception {
        String client = "10.0.0.4";
        for (int i = 0; i < LOGIN_CAPACITY; i++) {
            login(client, "unknown@example.com").andExpect(status().isUnauthorized());
        }

        login(client, "unknown@example.com").andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER));
        // The client's other requests are limited separately
        listProducts(client).andExpect(status().isOk());
    }

    @Test
    void usersHaveTheLimitOfTheirCurrentRole() throws Exception {
        String email = "promoted@example.com";
        User user = createUser("Promoted User", email, PASSWORD, User.Role.USER);
        String userToken = obtainJwtToken(email, PASSWORD);
        for (int i = 0; i < USER_CAPACITY; i++) {
            getProfile(userToken).andExpect(status().isOk());
        }
        getProfile(userToken).andExpect(status().isTooManyRequests());

        user.setRole(User.Role.ADMIN);
        userRepository.save(user);
        String adminToken = obtainJwtToken(email, PASSWORD);

        getProfile(adminToken).andExpect(status().isOk())
                .andExpect(header().string(REMAINING_HEADER, String.valueOf(ADMIN_CAPACITY - 1)));
    }

    private ResultActions listProducts(String clientAddress) throws Exception {
        return mockMvc.perform(get(ENDPOINT_PRODUCTS).with(from(clientAddress)));
    }

    private ResultActions login(String clientAddress, String email) throws Exception {
        return mockMvc.perform(post(ENDPOINT_AUTH_LOGIN)
                .with(from(clientAddress))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginDTO(email, PASSWORD))));
    }

    private ResultActions getProfile(String authorization) throws Exception {
        return mockMvc.perform(get(ENDPOINT_USERS_ME).header(HttpHeaders.AUTHORIZATION, authorization));
    }

    private static RequestPostProcessor from(String clientAddress) {
        return request -> {
            request.setRemoteAddr(clientAddress);
            return request;
        };
    }
}
