package com.gplanet.commerce.api.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.gplanet.commerce.api.configs.TestRedisConfiguration;
import com.gplanet.commerce.api.entities.User;
import com.gplanet.commerce.api.exceptions.ApiException;
import com.gplanet.commerce.api.utilities.DatabaseOperationHandler;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfiguration.class) 
class DatabaseOperationHandlerTest extends AbstractIntegrationTest {

    @Test
    void executeOperationSuccess() {
        // Arrange
        User user = new User();
        user.setName("Test");
        user.setEmail("test@example.com");
        user.setPassword("password");
        user.setRole(User.Role.USER);
        
        // Act
        User result = DatabaseOperationHandler.executeOperation(() -> 
            userRepository.save(user));
            
        // Assert
        assertNotNull(result.getId());
        assertEquals("test@example.com", result.getEmail());
    }
    
    @Test
    void executeOperationDuplicateKeyThrowsException() {
        // Arrange
        User user1 = new User();
        user1.setEmail("duplicate@example.com");
        user1.setName("Test 1");
        user1.setPassword("password1");
        user1.setRole(User.Role.USER);
        userRepository.save(user1);
        
        User user2 = new User();
        user2.setEmail("duplicate@example.com");
        user2.setName("Test 2");
        user2.setPassword("password2");
        user2.setRole(User.Role.USER);
        
        // Act & Assert
        assertThrows(ApiException.class, () -> 
            DatabaseOperationHandler.executeOperation(() -> 
                userRepository.save(user2)));
    }
}
