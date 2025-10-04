package com.gplanet.commerce.api.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import com.gplanet.commerce.api.entities.Usuario;

@DataJpaTest
@ActiveProfiles("test")
class UsuarioRepositoryTest {
    public static final String TEST_NAME = "Test User";
    public static final String TEST_USERNAME = "test@example.com";
    public static final String TEST_PASSWD = "testPasswd123!";
    public static final String TEST_ADMIN_USERNAME = "admin@example.com";
    public static final String TEST_ADMIN_PASSWD = "adminPasswd123!";
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Test
    void saveValidUserSuccess() {
        // Arrange
        Usuario usuario = new Usuario();
        usuario.setNombre(TEST_NAME);
        usuario.setEmail(TEST_USERNAME);
        usuario.setPassword(TEST_ADMIN_PASSWD);
        usuario.setRol(Usuario.Role.USER);
        usuario.setFechaCreacion(LocalDateTime.now());
        
        // Act
        Usuario savedUsuario = usuarioRepository.save(usuario);
        
        // Assert
        assertNotNull(savedUsuario.getId());
        assertEquals(TEST_USERNAME, savedUsuario.getEmail());
        assertEquals(Usuario.Role.USER, savedUsuario.getRol());
    }
    
    @Test
    void saveDuplicateEmailThrowsException() {
        // Arrange
        Usuario usuario1 = new Usuario();
        usuario1.setNombre("User 1");
        usuario1.setEmail(TEST_USERNAME);
        usuario1.setPassword("password1");
        usuario1.setRol(Usuario.Role.USER);
        usuarioRepository.save(usuario1);
        
        Usuario usuario2 = new Usuario();
        usuario2.setNombre("User 2");
        usuario2.setEmail(TEST_USERNAME); // Same username
        usuario2.setPassword("password2");
        usuario2.setRol(Usuario.Role.USER);
        
        // Act & Assert
        assertThrows(DataIntegrityViolationException.class, () -> {
            usuarioRepository.save(usuario2);
            usuarioRepository.flush();
        });
    }
    
    @Test
    void findByEmailExistingEmailReturnsUser() {
        // Arrange
        Usuario usuario = new Usuario();
        usuario.setNombre(TEST_NAME);
        usuario.setEmail(TEST_USERNAME);
        usuario.setPassword(TEST_PASSWD);
        usuario.setRol(Usuario.Role.USER);
        usuarioRepository.save(usuario);
        
        // Act
        Optional<Usuario> foundUsuario = usuarioRepository.findByEmail(TEST_USERNAME);
        
        // Assert
        assertTrue(foundUsuario.isPresent());
        assertEquals(TEST_USERNAME, foundUsuario.get().getEmail());
    }
    
    @Test
    void findByRolAdminRoleReturnsAdminUsers() {
        // Arrange
        Usuario adminUser = new Usuario();
        adminUser.setNombre(TEST_NAME);
        adminUser.setEmail(TEST_ADMIN_USERNAME);
        adminUser.setPassword(TEST_ADMIN_PASSWD);
        adminUser.setRol(Usuario.Role.ADMIN);
        usuarioRepository.save(adminUser);
        
        Usuario regularUser = new Usuario();
        regularUser.setNombre(TEST_NAME);
        regularUser.setEmail(TEST_USERNAME);
        regularUser.setPassword(TEST_ADMIN_PASSWD);
        regularUser.setRol(Usuario.Role.USER);
        usuarioRepository.save(regularUser);
        
        // Act
        List<Usuario> adminUsers = usuarioRepository.findByRol(Usuario.Role.ADMIN);
        
        // Assert
        assertEquals(1, adminUsers.size());
        assertEquals(Usuario.Role.ADMIN, adminUsers.get(0).getRol());
    }
}
