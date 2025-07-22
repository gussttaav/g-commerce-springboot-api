package com.gplanet.commerce.api.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.gplanet.commerce.api.entities.Usuario;
import com.gplanet.commerce.api.exceptions.ApiException;
import com.gplanet.commerce.api.repositories.UsuarioRepository;
import com.gplanet.commerce.api.utilities.DatabaseOperationHandler;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DatabaseOperationHandlerTest {

    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Test
    void executeOperation_Success() {
        // Arrange
        Usuario usuario = new Usuario();
        usuario.setNombre("Test");
        usuario.setEmail("test@example.com");
        usuario.setPassword("password");
        usuario.setRol(Usuario.Role.USER);
        
        // Act
        Usuario result = DatabaseOperationHandler.executeOperation(() -> 
            usuarioRepository.save(usuario));
            
        // Assert
        assertNotNull(result.getId());
        assertEquals("test@example.com", result.getEmail());
    }
    
    @Test
    void executeOperation_DuplicateKey_ThrowsException() {
        // Arrange
        Usuario usuario1 = new Usuario();
        usuario1.setEmail("duplicate@example.com");
        usuario1.setNombre("Test 1");
        usuario1.setPassword("password");
        usuarioRepository.save(usuario1);
        
        Usuario usuario2 = new Usuario();
        usuario2.setEmail("duplicate@example.com");
        usuario2.setNombre("Test 2");
        usuario2.setPassword("password");
        
        // Act & Assert
        assertThrows(ApiException.class, () -> 
            DatabaseOperationHandler.executeOperation(() -> 
                usuarioRepository.save(usuario2)));
    }
}
