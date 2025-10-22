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

import com.gplanet.commerce.api.entities.User;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {
    public static final String TEST_NAME = "Test User";
    public static final String TEST_USERNAME = "test@example.com";
    public static final String TEST_PASSWD = "testPasswd123!";
    public static final String TEST_ADMIN_USERNAME = "admin@example.com";
    public static final String TEST_ADMIN_PASSWD = "adminPasswd123!";
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void saveValidUserSuccess() {
        // Arrange
        User user = new User();
        user.setName(TEST_NAME);
        user.setEmail(TEST_USERNAME);
        user.setPassword(TEST_ADMIN_PASSWD);
        user.setRole(User.Role.USER);
        user.setCreationDate(LocalDateTime.now());
        
        // Act
        User savedUsuario = userRepository.save(user);
        
        // Assert
        assertNotNull(savedUsuario.getId());
        assertEquals(TEST_USERNAME, savedUsuario.getEmail());
        assertEquals(User.Role.USER, savedUsuario.getRole());
    }
    
    @Test
    void saveDuplicateEmailThrowsException() {
        // Arrange
        User user1 = new User();
        user1.setName("User 1");
        user1.setEmail(TEST_USERNAME);
        user1.setPassword("password1");
        user1.setRole(User.Role.USER);
        userRepository.save(user1);
        
        User user2 = new User();
        user2.setName("User 2");
        user2.setEmail(TEST_USERNAME); // Same username
        user2.setPassword("password2");
        user2.setRole(User.Role.USER);
        
        // Act & Assert
        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.save(user2);
            userRepository.flush();
        });
    }
    
    @Test
    void findByEmailExistingEmailReturnsUser() {
        // Arrange
        User user = new User();
        user.setName(TEST_NAME);
        user.setEmail(TEST_USERNAME);
        user.setPassword(TEST_PASSWD);
        user.setRole(User.Role.USER);
        userRepository.save(user);
        
        // Act
        Optional<User> userFound = userRepository.findByEmail(TEST_USERNAME);
        
        // Assert
        assertTrue(userFound.isPresent());
        assertEquals(TEST_USERNAME, userFound.get().getEmail());
    }
    
    @Test
    void findByRolAdminRoleReturnsAdminUsers() {
        // Arrange
        User adminUser = new User();
        adminUser.setName(TEST_NAME);
        adminUser.setEmail(TEST_ADMIN_USERNAME);
        adminUser.setPassword(TEST_ADMIN_PASSWD);
        adminUser.setRole(User.Role.ADMIN);
        userRepository.save(adminUser);
        
        User regularUser = new User();
        regularUser.setName(TEST_NAME);
        regularUser.setEmail(TEST_USERNAME);
        regularUser.setPassword(TEST_ADMIN_PASSWD);
        regularUser.setRole(User.Role.USER);
        userRepository.save(regularUser);
        
        // Act
        List<User> adminUsers = userRepository.findByRole(User.Role.ADMIN);
        
        // Assert
        assertEquals(1, adminUsers.size());
        assertEquals(User.Role.ADMIN, adminUsers.get(0).getRole());
    }
}
