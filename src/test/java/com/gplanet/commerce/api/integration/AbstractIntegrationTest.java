package com.gplanet.commerce.api.integration;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gplanet.commerce.api.configs.TestRedisConfiguration;
import com.gplanet.commerce.api.dtos.auth.LoginDTO;
import com.gplanet.commerce.api.dtos.auth.LoginResponseDTO;
import com.gplanet.commerce.api.entities.User;
import com.gplanet.commerce.api.repositories.ProductRepository;
import com.gplanet.commerce.api.repositories.PurchaseRepository;
import com.gplanet.commerce.api.repositories.UserRepository;
import com.gplanet.commerce.api.services.AuthService;

/**
 * Abstract base class for integration tests.
 * 
 * <p>Provides common utilities such as database cleanup,
 * user creation and JWT token generation.
 * 
 * @author Gustavo
 * @version 1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfiguration.class) 
public abstract class AbstractIntegrationTest {

    protected static final String ADMIN_EMAIL = "admin@example.com";
    protected static final String ADMIN_PASSWORD = "admin123";

    @Autowired
    protected MockMvc mockMvc;
    
    @Autowired
    protected ObjectMapper objectMapper;
    
    @Autowired
    protected UserRepository userRepository;
    
    @Autowired
    protected ProductRepository productRepository;
    
    @Autowired
    protected PurchaseRepository purchaseRepository;
    
    @Autowired
    protected PasswordEncoder passwordEncoder;
    
    @Autowired
    protected AuthService authService;

    @Autowired
    protected StringRedisTemplate stringRedisTemplate;
    
    @BeforeEach
    void cleanDatabase() {
        purchaseRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
    }
    
    protected User createAdminUser() {
        return createUser("Admin User", ADMIN_EMAIL, ADMIN_PASSWORD, User.Role.ADMIN);
    }
    
    protected User createUser(String name, String email, String password, User.Role role) {
        // First check if user exists
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            return existingUser.get();
        }
        
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setCreationDate(LocalDateTime.now());
        return userRepository.save(user);
    }
    
    protected String obtainJwtToken(String username, String password) {
        try {
            LoginDTO loginDTO = new LoginDTO(username, password);
            LoginResponseDTO response = authService.login(loginDTO);
            return "Bearer " + response.token();
        } catch (Exception e) {
            throw new RuntimeException("Failed to obtain JWT token", e);
        }
    }
}
