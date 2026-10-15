package com.gplanet.commerce.api.integration;

import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_PURCHASES;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_USERS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import com.gplanet.commerce.api.dtos.user.UserDTO;
import com.gplanet.commerce.api.entities.User;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest extends AbstractIntegrationTest {
    
    @Test
    void publicEndpointWithoutAuthenticationIsAllowed() throws Exception {
        UserDTO userDTO = new UserDTO("New User", "new@example.com", "password123");
        
        mockMvc.perform(post(ENDPOINT_USERS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isOk());
    }
    
    @Test
    void protectedEndpointWithoutAuthenticationIsDenied() throws Exception {
        mockMvc.perform(get(ENDPOINT_PURCHASES))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void adminEndpointWithRegularUserIsForbidden() throws Exception {
        // Create regular user using the helper method which properly handles password encoding
        createUser("Regular User", "user@example.com", "Password123!", User.Role.USER);
        
        // Use the JWT token obtained through proper authentication flow
        String jwtToken = obtainJwtToken("user@example.com", "Password123!");
        
        mockMvc.perform(patch(String.format("%s/1/role", ENDPOINT_USERS))
                .header(HttpHeaders.AUTHORIZATION, jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());
    }
}
