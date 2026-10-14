package com.gplanet.commerce.api.integration;

import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_AUTH_LOGOUT;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_AUTH_REFRESH;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_USUARIOS_ME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.gplanet.commerce.api.dtos.auth.LoginDTO;
import com.gplanet.commerce.api.dtos.auth.LoginResponseDTO;
import com.gplanet.commerce.api.dtos.auth.LogoutRequestDTO;
import com.gplanet.commerce.api.entities.User;

/**
 * Integration tests for the token lifecycle: logout revokes the session's tokens (SEC-1) and refresh tokens are
 * not accepted as access tokens (SEC-2).
 */
class TokenLifecycleIntegrationTest extends AbstractIntegrationTest {

    private static final String USER_EMAIL = "lifecycle@example.com";
    private static final String USER_PASSWORD = "Password123!";

    private LoginResponseDTO session;

    @BeforeEach
    void login() {
        crearUsuario("Lifecycle User", USER_EMAIL, USER_PASSWORD, User.Role.USER);
        session = authService.login(new LoginDTO(USER_EMAIL, USER_PASSWORD));
    }

    @Test
    void accessTokenIsRejectedAfterLogout() throws Exception {
        logout(session);

        getProfile(session.token()).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenIsRejectedAfterLogout() throws Exception {
        logout(session);

        refresh(session.refreshToken()).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutDoesNotRevokeOtherSessionsOfTheUser() throws Exception {
        LoginResponseDTO otherSession = authService.login(new LoginDTO(USER_EMAIL, USER_PASSWORD));

        logout(session);

        getProfile(otherSession.token()).andExpect(status().isOk());
        refresh(otherSession.refreshToken()).andExpect(status().isOk());
    }

    @Test
    void refreshTokenIsRejectedAsAccessToken() throws Exception {
        getProfile(session.refreshToken()).andExpect(status().isUnauthorized());
    }

    @Test
    void accessTokenIsRejectedForRefresh() throws Exception {
        refresh(session.token()).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshIssuesWorkingTokens() throws Exception {
        LoginResponseDTO refreshed = readSession(refresh(session.refreshToken()).andExpect(status().isOk()));

        getProfile(refreshed.token()).andExpect(status().isOk());
        refresh(refreshed.refreshToken()).andExpect(status().isOk());
    }

    private void logout(LoginResponseDTO loggedOutSession) throws Exception {
        mockMvc.perform(post(ENDPOINT_AUTH_LOGOUT)
                .header(HttpHeaders.AUTHORIZATION, bearer(loggedOutSession.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LogoutRequestDTO(loggedOutSession.refreshToken()))))
                .andExpect(status().isOk());
    }

    private ResultActions getProfile(String token) throws Exception {
        return mockMvc.perform(get(ENDPOINT_USUARIOS_ME).header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private ResultActions refresh(String token) throws Exception {
        return mockMvc.perform(post(ENDPOINT_AUTH_REFRESH).header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private LoginResponseDTO readSession(ResultActions result) throws Exception {
        return objectMapper.readValue(result.andReturn().getResponse().getContentAsString(), LoginResponseDTO.class);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
