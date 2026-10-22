package com.gplanet.commerce.api.integration;

import static com.gplanet.commerce.api.testutils.ApiErrorResultMatchers.errorMessage;
import static com.gplanet.commerce.api.testutils.ApiErrorResultMatchers.forbiddenError;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_USERS;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_USERS_ME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import com.gplanet.commerce.api.dtos.user.UserRoleUpdateDTO;
import com.gplanet.commerce.api.dtos.user.UserUpdateDTO;
import com.gplanet.commerce.api.entities.User;

/**
 * The email, password and role of the shared demo accounts can't be changed, so no visitor can lock the others out.
 */
@TestPropertySource(properties = "demo.protected-accounts=" + DemoAccountProtectionIntegrationTest.DEMO_USER_EMAIL
    + "," + AbstractIntegrationTest.ADMIN_EMAIL)
class DemoAccountProtectionIntegrationTest extends AbstractIntegrationTest {

    static final String DEMO_USER_EMAIL = "john@example.com";
    private static final String DEMO_USER_PASSWORD = "demo-user-password";
    private static final String CREDENTIALS_MESSAGE =
        "The email and password of the demo accounts can't be changed; register your own account to try it";
    private static final String ROLE_MESSAGE = "The role of the demo accounts can't be changed";

    private User demoUser;
    private String demoUserToken;

    @BeforeEach
    void createDemoUser() {
        demoUser = createUser("John Doe", DEMO_USER_EMAIL, DEMO_USER_PASSWORD, User.Role.USER);
        demoUserToken = obtainJwtToken(DEMO_USER_EMAIL, DEMO_USER_PASSWORD);
    }

    @Test
    void demoAccountCannotChangeItsEmail() throws Exception {
        mockMvc.perform(patch(ENDPOINT_USERS_ME)
                .header(HttpHeaders.AUTHORIZATION, demoUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserUpdateDTO(null, "someone@example.com"))))
                .andExpect(status().isForbidden())
                .andExpect(forbiddenError())
                .andExpect(errorMessage(CREDENTIALS_MESSAGE));

        assertEquals(DEMO_USER_EMAIL, userRepository.findById(demoUser.getId()).orElseThrow().getEmail());
    }

    @Test
    void demoAccountCannotChangeItsPassword() throws Exception {
        UserUpdateDTO passwordChange = new UserUpdateDTO(DEMO_USER_PASSWORD, "another-password", "another-password");

        mockMvc.perform(patch(ENDPOINT_USERS_ME)
                .header(HttpHeaders.AUTHORIZATION, demoUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordChange)))
                .andExpect(status().isForbidden())
                .andExpect(errorMessage(CREDENTIALS_MESSAGE));

        User user = userRepository.findById(demoUser.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches(DEMO_USER_PASSWORD, user.getPassword()));
    }

    @Test
    void demoAccountCanChangeItsName() throws Exception {
        mockMvc.perform(patch(ENDPOINT_USERS_ME)
                .header(HttpHeaders.AUTHORIZATION, demoUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserUpdateDTO("John A. Doe", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John A. Doe"));
    }

    @Test
    void adminCannotChangeTheRoleOfADemoAccount() throws Exception {
        createAdminUser();
        String adminToken = obtainJwtToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(patch(ENDPOINT_USERS + "/" + demoUser.getId() + "/role")
                .header(HttpHeaders.AUTHORIZATION, adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserRoleUpdateDTO(User.Role.ADMIN))))
                .andExpect(status().isForbidden())
                .andExpect(forbiddenError())
                .andExpect(errorMessage(ROLE_MESSAGE));

        assertEquals(User.Role.USER, userRepository.findById(demoUser.getId()).orElseThrow().getRole());
    }

    @Test
    void adminCanChangeTheRoleOfOtherAccounts() throws Exception {
        createAdminUser();
        String adminToken = obtainJwtToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        User visitor = createUser("Visitor", "visitor@example.com", "visitor-password", User.Role.USER);

        mockMvc.perform(patch(ENDPOINT_USERS + "/" + visitor.getId() + "/role")
                .header(HttpHeaders.AUTHORIZATION, adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserRoleUpdateDTO(User.Role.ADMIN))))
                .andExpect(status().isOk());

        assertEquals(User.Role.ADMIN, userRepository.findById(visitor.getId()).orElseThrow().getRole());
    }
}
