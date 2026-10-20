package com.gplanet.commerce.api.integration;

import static com.gplanet.commerce.api.testutils.ApiErrorResultMatchers.errorMessage;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_USERS;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_USERS_ME;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import com.gplanet.commerce.api.dtos.user.UserDTO;
import com.gplanet.commerce.api.dtos.user.UserRoleUpdateDTO;
import com.gplanet.commerce.api.dtos.user.UserUpdateDTO;
import com.gplanet.commerce.api.entities.User;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserIntegrationTest extends AbstractIntegrationTest {
    private static final String TEST_USER_NAME = "Test User";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_ADMIN_EMAIL = "admin@example.com";
    private static final String TEST_ADMIN_PASSWORD = "admin123";
    private static final String USER_ROLE = "USER";
    private static final String ADMIN_ROLE = "ADMIN";

    private String createChangeRoleRequest(Long userId) {
        return String.format("%s/%d/role", ENDPOINT_USERS, userId);
    }

    public static ResultMatcher userName(String name) throws Exception {
        return jsonPath("$.name").value(name);
    }

    public static ResultMatcher userEmail(String email) throws Exception {
        return jsonPath("$.email").value(email);
    }

    public static ResultMatcher userRol(String role) throws Exception {
        return jsonPath("$.role").value(role);
    }

    @Test
    void registerUserValidDataReturnsCreatedUser() throws Exception {
        // Arrange
        UserDTO userDTO = new UserDTO(TEST_USER_NAME, TEST_EMAIL, TEST_PASSWORD);
        
        // Act & Assert
        mockMvc.perform(post(ENDPOINT_USERS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isOk())
                .andExpect(userEmail(TEST_EMAIL))
                .andExpect(userRol(USER_ROLE))
                .andExpect(userName(TEST_USER_NAME));
        
        // Verify database
        User savedUser = userRepository.findByEmail(TEST_EMAIL)
            .orElseThrow(() -> new AssertionError("User not found in the database"));
        assertEquals(User.Role.USER, savedUser.getRole());
        assertTrue(passwordEncoder.matches(TEST_PASSWORD, savedUser.getPassword()));
    }
    
    @Test
    void registerWithoutRoleCreatesUserWithUserRole() throws Exception {
        // Arrange - The role is optional, as in the README example
        String request = """
            {"name": "%s", "email": "%s", "password": "%s"}
            """.formatted(TEST_USER_NAME, TEST_EMAIL, TEST_PASSWORD);

        // Act & Assert
        mockMvc.perform(post(ENDPOINT_USERS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isOk())
                .andExpect(userRol(USER_ROLE));

        User savedUser = userRepository.findByEmail(TEST_EMAIL).orElseThrow();
        assertEquals(User.Role.USER, savedUser.getRole());
    }

    @Test
    void registerAdminValidDataReturnsCreatedAdmin() throws Exception {
        // Arrange
        final String newEmail = "new.admin@example.com";
        UserDTO adminDTO = new UserDTO(
                "New Admin", 
                newEmail, 
                "admin123", User.Role.ADMIN);
        createAdminUser();

        String authHeader = obtainJwtToken(TEST_ADMIN_EMAIL, TEST_ADMIN_PASSWORD);
        
        // Act & Assert
        mockMvc.perform(post(ENDPOINT_USERS)
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adminDTO)))
                .andExpect(status().isOk())
                .andExpect(userEmail(newEmail))
                .andExpect(userRol(ADMIN_ROLE));
                
        // Verify database
        User savedAdmin = userRepository.findByEmail(newEmail)
            .orElseThrow(() -> new AssertionError("Admin not found in the database"));
        assertEquals(User.Role.ADMIN, savedAdmin.getRole());
    }
    
    @Test
    void registerUserDuplicateEmailReturnsError() throws Exception {
        // Arrange - Create first user
        UserDTO firstUser = new UserDTO("First User", TEST_EMAIL, TEST_PASSWORD);
        
        mockMvc.perform(post(ENDPOINT_USERS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstUser)))
                .andExpect(status().isOk());

        // Arrange - Create duplicate user
        UserDTO duplicatedUser = new UserDTO("Duplicate User", TEST_EMAIL, TEST_PASSWORD);
        
        // Act & Assert - The database error used to leak the table and column names
        mockMvc.perform(post(ENDPOINT_USERS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicatedUser)))
                .andExpect(status().isConflict())
                .andExpect(errorMessage("A user with this email already exists"));
    }
    
    @Test
    void updateProfileAuthenticatedUserUpdatesProfile() throws Exception {
        // Arrange - Create user first
        final String updatedEmail = "updated@example.com";
        UserDTO userDTO = new UserDTO(TEST_USER_NAME, TEST_EMAIL, TEST_PASSWORD);
        
        mockMvc.perform(post(ENDPOINT_USERS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isOk());

        // Arrange - Prepare update data
        UserUpdateDTO updatedUserDTO = new UserUpdateDTO(
            "Updated Name", updatedEmail);
        
        String authHeader = obtainJwtToken(TEST_EMAIL, TEST_PASSWORD);
        
        // Act & Assert
        mockMvc.perform(patch(ENDPOINT_USERS_ME)
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedUserDTO)))
                .andExpect(status().isOk())
                .andExpect(userName("Updated Name"))
                .andExpect(userEmail(updatedEmail));
                
        // Verify database update
        assertFalse(userRepository.findByEmail(TEST_EMAIL).isPresent(), 
            "Old email should not exist");
        assertTrue(userRepository.findByEmail(updatedEmail).isPresent(),
            "New email should exist");
    }

    @Test
    void updateProfileToTakenEmailReturnsConflict() throws Exception {
        // Arrange - Two users; the second one tries to take the email of the first
        final String takenEmail = "taken@example.com";
        createUser("Other User", takenEmail, TEST_PASSWORD, User.Role.USER);
        createUser(TEST_USER_NAME, TEST_EMAIL, TEST_PASSWORD, User.Role.USER);
        String authHeader = obtainJwtToken(TEST_EMAIL, TEST_PASSWORD);

        // Act & Assert
        mockMvc.perform(patch(ENDPOINT_USERS_ME)
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserUpdateDTO(null, takenEmail))))
                .andExpect(status().isConflict())
                .andExpect(errorMessage("A user with this email already exists"));

        assertTrue(userRepository.findByEmail(TEST_EMAIL).isPresent(), "The email should not change");
    }

    @Test
    void updateProfileKeepingOwnEmailUpdatesProfile() throws Exception {
        // Arrange
        createUser(TEST_USER_NAME, TEST_EMAIL, TEST_PASSWORD, User.Role.USER);
        String authHeader = obtainJwtToken(TEST_EMAIL, TEST_PASSWORD);

        // Act & Assert - Sending the current email again is not a conflict with itself
        mockMvc.perform(patch(ENDPOINT_USERS_ME)
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserUpdateDTO("Renamed User", TEST_EMAIL))))
                .andExpect(status().isOk())
                .andExpect(userName("Renamed User"))
                .andExpect(userEmail(TEST_EMAIL));
    }

    @Test
    void changePasswordValidDataChangesSuccessfully() throws Exception {
        // Arrange - Create user first
        final String newPassword = "newPassword123";
        UserDTO userDTO = new UserDTO(TEST_USER_NAME, TEST_EMAIL, TEST_PASSWORD);
        
        mockMvc.perform(post(ENDPOINT_USERS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isOk());

        // Arrange - Prepare password change data
        UserUpdateDTO changeDTO = new UserUpdateDTO(TEST_PASSWORD, newPassword, newPassword);
        
        String authHeader = obtainJwtToken(TEST_EMAIL, TEST_PASSWORD);
        
        // Act & Assert
        mockMvc.perform(patch(ENDPOINT_USERS_ME)
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(changeDTO)))
                .andExpect(status().isOk());
                
        // Verify password change
        User user = userRepository.findByEmail(TEST_EMAIL)
            .orElseThrow(() -> new AssertionError("User not found"));
        assertTrue(passwordEncoder.matches(newPassword, user.getPassword()), 
            "Password should be updated in database");
    }
    
    @Test
    void changePasswordIncorrectCurrentPasswordReturnsError() throws Exception {
        // Arrange - Create user first
        UserDTO userDTO = new UserDTO(TEST_USER_NAME, TEST_EMAIL, TEST_PASSWORD);
        
        mockMvc.perform(post(ENDPOINT_USERS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isOk());

        // Arrange - Prepare invalid password change data
        UserUpdateDTO changeDTO = new UserUpdateDTO("wrongPassword", "newPassword", "newPassword");
        
        String authHeader = obtainJwtToken(TEST_EMAIL, TEST_PASSWORD);
        
        // Act & Assert
        mockMvc.perform(patch(ENDPOINT_USERS_ME)
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(changeDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(errorMessage("The current password is incorrect"));
    }

    @Test
    void getProfileAuthenticatedUserReturnsProfile() throws Exception {
        // Arrange - Create user first
        UserDTO userDTO = new UserDTO(TEST_USER_NAME, TEST_EMAIL, TEST_PASSWORD);
        
        mockMvc.perform(post(ENDPOINT_USERS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isOk());

        String authHeader = obtainJwtToken(TEST_EMAIL, TEST_PASSWORD);
        
        // Act & Assert
        mockMvc.perform(get(ENDPOINT_USERS_ME)
                .header(HttpHeaders.AUTHORIZATION, authHeader))
                .andExpect(status().isOk())
                .andExpect(userName(TEST_USER_NAME))
                .andExpect(userEmail(TEST_EMAIL))
                .andExpect(userRol(USER_ROLE));
    }

    @Test
    void getProfileWithoutAuthenticationReturnsUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(get(ENDPOINT_USERS_ME))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listUsersAsAdminReturnsAllUsers() throws Exception {
        // Arrange - Create regular user
        UserDTO userDTO = new UserDTO(TEST_USER_NAME, TEST_EMAIL, TEST_PASSWORD);
        
        mockMvc.perform(post(ENDPOINT_USERS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isOk());

        // Create admin user
        createAdminUser();
        String authHeader = obtainJwtToken(TEST_ADMIN_EMAIL, TEST_ADMIN_PASSWORD);
        
        // Act & Assert
        mockMvc.perform(get(ENDPOINT_USERS)
                .header(HttpHeaders.AUTHORIZATION, authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[*].email", hasItems(TEST_EMAIL, TEST_ADMIN_EMAIL)))
                .andExpect(jsonPath("$.content[*].role", hasItems(USER_ROLE, ADMIN_ROLE)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.isLastPage").value(true));
    }

    @Test
    void listUsersAsRegularUserReturnsForbidden() throws Exception {
        // Arrange - Create regular user
        UserDTO userDTO = new UserDTO(TEST_USER_NAME, TEST_EMAIL, TEST_PASSWORD);
        
        mockMvc.perform(post(ENDPOINT_USERS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isOk());

        String authHeader = obtainJwtToken(TEST_EMAIL, TEST_PASSWORD);
        
        // Act & Assert
        mockMvc.perform(get(ENDPOINT_USERS)
                .header(HttpHeaders.AUTHORIZATION, authHeader))
                .andExpect(status().isForbidden());
    }

    @Test
    void changeRoleAsAdminChangesRole() throws Exception {
        // Arrange - Create regular user
        UserDTO userDTO = new UserDTO(TEST_USER_NAME, TEST_EMAIL, TEST_PASSWORD);
        
        MvcResult result = mockMvc.perform(post(ENDPOINT_USERS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isOk())
                .andReturn();

        // Get user ID from response
        String responseJson = result.getResponse().getContentAsString();
        Number userIdNumber = JsonPath.read(responseJson, "$.id");
        Long userId = userIdNumber.longValue();

        // Create admin user
        createAdminUser();
        String authHeader = obtainJwtToken(TEST_ADMIN_EMAIL, TEST_ADMIN_PASSWORD);
        UserRoleUpdateDTO newRoleDTO = new UserRoleUpdateDTO(User.Role.ADMIN);
        
        // Act & Assert - Change role to ADMIN
        mockMvc.perform(patch(createChangeRoleRequest(userId))
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newRoleDTO)))
                .andExpect(status().isOk());
                
        // Verify role change in database
        User user = userRepository.findByEmail(TEST_EMAIL)
                .orElseThrow(() -> new AssertionError("User not found"));
        assertEquals(User.Role.ADMIN, user.getRole());
    }

    @Test
    void changeRoleAsRegularUserReturnsForbidden() throws Exception {
        // Arrange - Create first regular user
        UserDTO userDTO = new UserDTO(TEST_USER_NAME, TEST_EMAIL, TEST_PASSWORD);
        
        MvcResult result = mockMvc.perform(post(ENDPOINT_USERS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isOk())
                .andReturn();

        // Get user ID from response
        String responseJson = result.getResponse().getContentAsString();
        Number userIdNumber = JsonPath.read(responseJson, "$.id");
        Long userId = userIdNumber.longValue();

        // Create second regular user
        UserDTO user2DTO = new UserDTO("Test User 2", "test2@example.com", TEST_PASSWORD);
        
        mockMvc.perform(post(ENDPOINT_USERS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user2DTO)))
                .andExpect(status().isOk());

        String authHeader = obtainJwtToken("test2@example.com", TEST_PASSWORD);
        UserRoleUpdateDTO newRoleDTO = new UserRoleUpdateDTO(User.Role.ADMIN);
        
        // Act & Assert - Try to change role
        mockMvc.perform(patch(createChangeRoleRequest(userId))
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newRoleDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void changeRoleNonExistentUserReturnsNotFound() throws Exception {
        // Arrange - Create admin user
        createAdminUser();
        String authHeader = obtainJwtToken(TEST_ADMIN_EMAIL, TEST_ADMIN_PASSWORD);
        UserRoleUpdateDTO newRoleDTO = new UserRoleUpdateDTO(User.Role.ADMIN);
        final Long userId = 999999L;
        
        // Act & Assert
        mockMvc.perform(patch(createChangeRoleRequest(userId))
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newRoleDTO)))
                .andExpect(status().isNotFound());
    }
}
