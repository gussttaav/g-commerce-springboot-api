package com.gplanet.commerce.api.integration;

import static com.gplanet.commerce.api.testutils.ApiErrorResultMatchers.errorMessage;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_AUTH_LOGIN;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_AUTH_LOGOUT;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_AUTH_REFRESH;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_AUTH_VALIDATE;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_USERS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import com.gplanet.commerce.api.dtos.auth.LoginDTO;
import com.gplanet.commerce.api.dtos.auth.LogoutRequestDTO;
import com.gplanet.commerce.api.dtos.user.UserDTO;
import com.gplanet.commerce.api.entities.User;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest extends AbstractIntegrationTest {
    private static final String TEST_USER_NAME = "Test User";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String INVALID_CREDENTIALS = "Invalid email or password";

    @Test
    void loginValidCredentialsReturnsTokens() throws Exception {
        // Arrange - Create user first
        UserDTO userDTO = new UserDTO(TEST_USER_NAME, TEST_EMAIL, TEST_PASSWORD);
        
        mockMvc.perform(post(ENDPOINT_USERS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isOk());

        LoginDTO loginDTO = new LoginDTO(TEST_EMAIL, TEST_PASSWORD);

        // Act & Assert
        mockMvc.perform(post(ENDPOINT_AUTH_LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andExpect(jsonPath("$.refreshExpiresIn").isNumber());
    }

    @Test
    void loginInvalidCredentialsReturnsError() throws Exception {
        // Arrange
        LoginDTO loginDTO = new LoginDTO("nonexistent@example.com", "wrongpassword");

        // Act & Assert
        mockMvc.perform(post(ENDPOINT_AUTH_LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithUnknownEmailOrWrongPasswordReturnsTheSameError() throws Exception {
        // Arrange
        createUser(TEST_USER_NAME, TEST_EMAIL, TEST_PASSWORD, User.Role.USER);

        // Act & Assert - The response doesn't reveal whether the email is registered
        mockMvc.perform(post(ENDPOINT_AUTH_LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginDTO(TEST_EMAIL, "wrongPassword123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(errorMessage(INVALID_CREDENTIALS));

        mockMvc.perform(post(ENDPOINT_AUTH_LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginDTO("unknown@example.com", TEST_PASSWORD))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(errorMessage(INVALID_CREDENTIALS));
    }

    @Test
    void refreshValidTokenReturnsNewTokens() throws Exception {
        // Arrange - Login first to get token
        UserDTO userDTO = new UserDTO(TEST_USER_NAME, TEST_EMAIL, TEST_PASSWORD);

        mockMvc.perform(post(ENDPOINT_USERS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isOk());
        
        LoginDTO loginDTO = new LoginDTO(TEST_EMAIL, TEST_PASSWORD);
        
        String loginResponse = mockMvc.perform(post(ENDPOINT_AUTH_LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        String refreshToken = objectMapper.readTree(loginResponse).get("refreshToken").asText();

        // Act & Assert - Refresh tokens
        mockMvc.perform(post(ENDPOINT_AUTH_REFRESH)
                .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    void validateValidTokenReturnsValidationSuccess() throws Exception {
        // Arrange - Login first to get token
        UserDTO userDTO = new UserDTO(TEST_USER_NAME, TEST_EMAIL, TEST_PASSWORD);
        
        mockMvc.perform(post(ENDPOINT_USERS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isOk());

        LoginDTO loginDTO = new LoginDTO(TEST_EMAIL, TEST_PASSWORD);
        
        String loginResponse = mockMvc.perform(post(ENDPOINT_AUTH_LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        String accessToken = objectMapper.readTree(loginResponse).get("token").asText();

        // Act & Assert - Validate token
        mockMvc.perform(get(ENDPOINT_AUTH_VALIDATE)
                .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void logoutValidoBlacklistsTokens() throws Exception {
        // Arrange - Login first to get tokens
        UserDTO userDTO = new UserDTO(TEST_USER_NAME, TEST_EMAIL, TEST_PASSWORD);
        
        mockMvc.perform(post(ENDPOINT_USERS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isOk());

        LoginDTO loginDTO = new LoginDTO(TEST_EMAIL, TEST_PASSWORD);
        
        String loginResponse = mockMvc.perform(post(ENDPOINT_AUTH_LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        String accessToken = objectMapper.readTree(loginResponse).get("token").asText();
        String refreshToken = objectMapper.readTree(loginResponse).get("refreshToken").asText();
        LogoutRequestDTO logoutRequest = new LogoutRequestDTO(refreshToken);

        // Act & Assert - Logout
        mockMvc.perform(post(ENDPOINT_AUTH_LOGOUT)
                .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isOk());
    }
}
