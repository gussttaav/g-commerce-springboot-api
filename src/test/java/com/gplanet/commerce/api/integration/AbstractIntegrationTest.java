package com.gplanet.commerce.api.integration;

import java.time.LocalDateTime;
import java.util.Base64;
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
import com.gplanet.commerce.api.entities.Usuario;
import com.gplanet.commerce.api.repositories.ProductRepository;
import com.gplanet.commerce.api.repositories.PurchaseRepository;
import com.gplanet.commerce.api.repositories.UsuarioRepository;
import com.gplanet.commerce.api.services.AuthService;

/**
 * Clase base abstracta para pruebas de integración.
 * 
 * <p>Provee utilidades comunes como limpieza de la base de datos,
 * creación de usuarios y generación de tokens JWT.
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
    protected UsuarioRepository usuarioRepository;
    
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
    void limpiarBaseDeDatos() {
        purchaseRepository.deleteAll();
        productRepository.deleteAll();
        usuarioRepository.deleteAll();
    }
    
    protected Usuario crearUsuarioAdmin() {
        return crearUsuario("Admin User", ADMIN_EMAIL, ADMIN_PASSWORD, Usuario.Role.ADMIN);
    }
    
    protected Usuario crearUsuario(String nombre, String email, String password, Usuario.Role rol) {
        // First check if user exists
        Optional<Usuario> existingUser = usuarioRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            return existingUser.get();
        }
        
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setEmail(email);
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setRol(rol);
        usuario.setFechaCreacion(LocalDateTime.now());
        return usuarioRepository.save(usuario);
    }
    
    protected String obtenerJwtToken(String username, String password) {
        try {
            LoginDTO loginDTO = new LoginDTO(username, password);
            LoginResponseDTO response = authService.login(loginDTO);
            return "Bearer " + response.token();
        } catch (Exception e) {
            throw new RuntimeException("Failed to obtain JWT token", e);
        }
    }
    
    // Keep for backward compatibility or remove if not needed
    @Deprecated
    protected String obtenerBasicAuthHeader(String username, String password) {
        String auth = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes());
    }
}
