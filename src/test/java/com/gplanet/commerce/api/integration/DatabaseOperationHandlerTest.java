package com.gplanet.commerce.api.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.gplanet.commerce.api.configs.TestRedisConfiguration;
import com.gplanet.commerce.api.entities.User;
import com.gplanet.commerce.api.exceptions.ApiException;
import com.gplanet.commerce.api.repositories.UserRepository;
import com.gplanet.commerce.api.utilities.DatabaseOperationHandler;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfiguration.class) 
class DatabaseOperationHandlerTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository usuarioRepository;
    
    @Test
    void executeOperationSuccess() {
        // Arrange
        User usuario = new User();
        usuario.setName("Test");
        usuario.setEmail("test@example.com");
        usuario.setPassword("password");
        usuario.setRole(User.Role.USER);
        
        // Act
        User result = DatabaseOperationHandler.executeOperation(() -> 
            usuarioRepository.save(usuario));
            
        // Assert
        assertNotNull(result.getId());
        assertEquals("test@example.com", result.getEmail());
    }
    
    @Test
    void executeOperationDuplicateKeyThrowsException() {
        // Arrange
        User usuario1 = new User();
        usuario1.setEmail("duplicate@example.com");
        usuario1.setName("Test 1");
        usuario1.setPassword("password1");
        usuario1.setRole(User.Role.USER);
        usuarioRepository.save(usuario1);
        
        User usuario2 = new User();
        usuario2.setEmail("duplicate@example.com");
        usuario2.setName("Test 2");
        usuario2.setPassword("password2");
        usuario2.setRole(User.Role.USER);
        
        // Act & Assert
        assertThrows(ApiException.class, () -> 
            DatabaseOperationHandler.executeOperation(() -> 
                usuarioRepository.save(usuario2)));
    }
}
