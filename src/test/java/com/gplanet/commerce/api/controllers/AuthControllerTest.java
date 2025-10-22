package com.gplanet.commerce.api.controllers;

import static com.gplanet.commerce.api.testutils.ApiErrorResultMatchers.errorMessage;
import static com.gplanet.commerce.api.testutils.ApiErrorResultMatchers.validationError;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_AUTH_LOGIN;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_AUTH_LOGOUT;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_AUTH_REFRESH;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_AUTH_VALIDATE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gplanet.commerce.api.configs.TestSecurityConfig;
import com.gplanet.commerce.api.dtos.auth.LoginDTO;
import com.gplanet.commerce.api.dtos.auth.LoginResponseDTO;
import com.gplanet.commerce.api.dtos.auth.LogoutRequestDTO;
import com.gplanet.commerce.api.dtos.auth.TokenValidationResponseDTO;
import com.gplanet.commerce.api.dtos.user.UserResponseDTO;
import com.gplanet.commerce.api.entities.User;
import com.gplanet.commerce.api.exceptions.InvalidPasswordException;
import com.gplanet.commerce.api.exceptions.InvalidTokenException;
import com.gplanet.commerce.api.services.AuthService;

@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
class AuthControllerTest {

    private static final String TEST_USER_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";
    private static final String TOKEN_TYPE = "Bearer";
    private static final String AUTH_HEADER = "Authorization";
    private static final String AUTH_REFRESH_TOKEN = "Bearer " + REFRESH_TOKEN;
    private static final String AUTH_ACCESS_TOKEN = "Bearer " + ACCESS_TOKEN;
    private static final Long TOKEN_ACCESS_EXPIRATION_MS = 3600000L;
    private static final Long REFRESH_TOKEN_EXPIRATION_MS = 86400000L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @BeforeEach
    void setUp() {
        // Configure ObjectMapper for consistent formatting
        objectMapper.configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Nested
    @DisplayName("POST " + ENDPOINT_AUTH_LOGIN)
    class LoginTests {

        @Test
        @WithAnonymousUser
        @DisplayName("Should login successfully with valid credentials")
        void loginValidCredentialsReturnsLoginResponse() throws Exception {
            // Arrange
            LoginDTO loginDTO = new LoginDTO(TEST_USER_EMAIL, TEST_PASSWORD);
            LoginResponseDTO loginResponse = new LoginResponseDTO(
                new UserResponseDTO(1L, "Test User", TEST_USER_EMAIL, User.Role.USER, null),
                ACCESS_TOKEN,
                TOKEN_TYPE,
                TOKEN_ACCESS_EXPIRATION_MS,
                REFRESH_TOKEN,
                REFRESH_TOKEN_EXPIRATION_MS
            );

            when(authService.login(any(LoginDTO.class))).thenReturn(loginResponse);

            // Act & Assert
            mockMvc.perform(post(ENDPOINT_AUTH_LOGIN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.user.email").value(TEST_USER_EMAIL))
                .andExpect(jsonPath("$.token").value(ACCESS_TOKEN))
                .andExpect(jsonPath("$.refreshToken").value(REFRESH_TOKEN))
                .andExpect(jsonPath("$.tokenType").value(TOKEN_TYPE))
                .andExpect(jsonPath("$.expiresIn").value(TOKEN_ACCESS_EXPIRATION_MS))
                .andExpect(jsonPath("$.refreshExpiresIn").value(REFRESH_TOKEN_EXPIRATION_MS));

            verify(authService).login(any(LoginDTO.class));
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Should return 404 when user not found")
        void loginUserNotFoundReturnsUnauthorized() throws Exception {
            // Arrange
            LoginDTO loginDTO = new LoginDTO(TEST_USER_EMAIL, TEST_PASSWORD);
            when(authService.login(any(LoginDTO.class)))
                .thenThrow(new UsernameNotFoundException("User not found"));

            // Act & Assert
            mockMvc.perform(post(ENDPOINT_AUTH_LOGIN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isUnauthorized())
                .andExpect(errorMessage("User not found"));

            verify(authService).login(any(LoginDTO.class));
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Should return 401 when password is invalid")
        void loginInvalidPasswordReturnsUnauthorized() throws Exception {
            // Arrange
            LoginDTO loginDTO = new LoginDTO(TEST_USER_EMAIL, "wrong-password");
            when(authService.login(any(LoginDTO.class)))
                .thenThrow(new InvalidPasswordException("Invalid password"));

            // Act & Assert
            mockMvc.perform(post(ENDPOINT_AUTH_LOGIN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isUnauthorized())
                .andExpect(errorMessage("Invalid password"));

            verify(authService).login(any(LoginDTO.class));
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Should return 400 when email is invalid")
        void loginInvalidEmailBadRequest() throws Exception {
            // Arrange
            LoginDTO loginDTO = new LoginDTO("invalid-email", TEST_PASSWORD);

            // Act & Assert
            mockMvc.perform(post(ENDPOINT_AUTH_LOGIN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(validationError());

            verifyNoInteractions(authService);
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Should return 400 when required fields are missing")
        void loginMissingFieldsBadRequest() throws Exception {
            // Arrange
            LoginDTO loginDTO = new LoginDTO("", "");

            // Act & Assert
            mockMvc.perform(post(ENDPOINT_AUTH_LOGIN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(validationError());

            verifyNoInteractions(authService);
        }
    }

    @Nested
    @DisplayName("POST " + ENDPOINT_AUTH_REFRESH)
    class RefreshTests {

        @Test
        @WithAnonymousUser
        @DisplayName("Should refresh tokens with valid refresh token")
        void refreshValidTokenReturnsNewTokens() throws Exception {
            // Arrange
            LoginResponseDTO refreshResponse = new LoginResponseDTO(
                new UserResponseDTO(1L, "Test User", TEST_USER_EMAIL, User.Role.USER, null),
                "new-access-token",
                TOKEN_TYPE,
                TOKEN_ACCESS_EXPIRATION_MS,
                "new-refresh-token",
                REFRESH_TOKEN_EXPIRATION_MS
            );

            when(authService.refresh(any(String.class))).thenReturn(refreshResponse);

            // Act & Assert
            mockMvc.perform(post(ENDPOINT_AUTH_REFRESH)
                    .header(AUTH_HEADER, AUTH_REFRESH_TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));

            verify(authService).refresh(eq(AUTH_REFRESH_TOKEN));
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Should return 401 when refresh token is invalid")
        void refreshInvalidTokenReturnsUnauthorized() throws Exception {
            // Arrange
            when(authService.refresh(any(String.class)))
                .thenThrow(new InvalidTokenException("Invalid refresh token"));

            // Act & Assert
            mockMvc.perform(post(ENDPOINT_AUTH_REFRESH)
                    .header(AUTH_HEADER, AUTH_REFRESH_TOKEN))
                .andExpect(status().isUnauthorized())
                .andExpect(errorMessage("Invalid refresh token"));

            verify(authService).refresh(eq(AUTH_REFRESH_TOKEN));
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Should return 401 when Authorization header is missing")
        void refreshMissingAuthorizationHeaderReturnsUnauthorized() throws Exception {
            // Act & Assert
            mockMvc.perform(post(ENDPOINT_AUTH_REFRESH))
                .andExpect(status().isUnauthorized());

            verifyNoInteractions(authService);
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Should return 401 when Authorization header format is invalid")
        void refreshInvalidAuthorizationFormatReturnsUnauthorized() throws Exception {
            // Arrange
            when(authService.refresh(eq(REFRESH_TOKEN)))
                .thenThrow(new InvalidTokenException("Missing or invalid Bearer token"));

            // Act & Assert
            mockMvc.perform(post(ENDPOINT_AUTH_REFRESH)
                    .header(AUTH_HEADER, REFRESH_TOKEN))
                .andExpect(status().isUnauthorized())
                .andExpect(errorMessage("Missing or invalid Bearer token"));

            verify(authService).refresh(eq(REFRESH_TOKEN));
        }
    }

    @Nested
    @DisplayName("GET " + ENDPOINT_AUTH_VALIDATE)
    class ValidateTests {

        @Test
        @WithAnonymousUser
        @DisplayName("Should validate token successfully")
        void validateValidTokenReturnsValidationResponse() throws Exception {
            // Arrange
            TokenValidationResponseDTO validationResponse = new TokenValidationResponseDTO(true, "Token is valid");
            when(authService.validateToken(any(String.class))).thenReturn(validationResponse);

            // Act & Assert
            mockMvc.perform(get(ENDPOINT_AUTH_VALIDATE)
                    .header(AUTH_HEADER, AUTH_ACCESS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.message").value("Token is valid"));

            verify(authService).validateToken(eq(AUTH_ACCESS_TOKEN));
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Should return 401 when token is invalid")
        void validateInvalidTokenReturnsUnauthorized() throws Exception {
            // Arrange
            when(authService.validateToken(any(String.class)))
                .thenThrow(new InvalidTokenException("Invalid token"));

            // Act & Assert
            mockMvc.perform(get(ENDPOINT_AUTH_VALIDATE)
                    .header(AUTH_HEADER, AUTH_ACCESS_TOKEN))
                .andExpect(status().isUnauthorized())
                .andExpect(errorMessage("Invalid token"));

            verify(authService).validateToken(eq(AUTH_ACCESS_TOKEN));
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Should return 401 when Authorization header is missing")
        void validateMissingAuthorizationHeaderReturnsUnauthorized() throws Exception {
            // Act & Assert
            mockMvc.perform(get(ENDPOINT_AUTH_VALIDATE))
                .andExpect(status().isUnauthorized());

            verifyNoInteractions(authService);
        }
    }

    @Nested
    @DisplayName("POST " + ENDPOINT_AUTH_LOGOUT)
    class LogoutTests {

        @Test
        @WithMockUser(username = TEST_USER_EMAIL)
        @DisplayName("Should logout successfully with valid request")
        void logoutValidRequestReturnsOk() throws Exception {
            // Arrange
            LogoutRequestDTO logoutRequest = new LogoutRequestDTO(REFRESH_TOKEN);

            // Act & Assert
            mockMvc.perform(post(ENDPOINT_AUTH_LOGOUT)
                    .header(AUTH_HEADER, AUTH_ACCESS_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isOk());

            verify(authService).logout(any(LogoutRequestDTO.class), eq(AUTH_ACCESS_TOKEN));
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Should return 401 when not authenticated")
        void logoutNotAuthenticatedReturnsUnauthorized() throws Exception {
            // Arrange
            LogoutRequestDTO logoutRequest = new LogoutRequestDTO(REFRESH_TOKEN);

            // Act & Assert
            mockMvc.perform(post(ENDPOINT_AUTH_LOGOUT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isUnauthorized());

            verifyNoInteractions(authService);
        }

        @Test
        @WithMockUser(username = TEST_USER_EMAIL)
        @DisplayName("Should return 400 when refresh token is missing")
        void logoutMissingRefreshTokenReturnsBadRequest() throws Exception {
            // Arrange
            LogoutRequestDTO logoutRequest = new LogoutRequestDTO("");

            // Act & Assert
            mockMvc.perform(post(ENDPOINT_AUTH_LOGOUT)
                    .header(AUTH_HEADER, AUTH_ACCESS_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isBadRequest());

            verifyNoInteractions(authService);
        }
    }
}
