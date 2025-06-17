package com.gplanet.commerce_api.integration;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gplanet.commerce_api.entities.Usuario;
import com.gplanet.commerce_api.repositories.CompraRepository;
import com.gplanet.commerce_api.repositories.ProductoRepository;
import com.gplanet.commerce_api.repositories.UsuarioRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {
    @Autowired
    protected MockMvc mockMvc;
    
    @Autowired
    protected ObjectMapper objectMapper;
    
    @Autowired
    protected UsuarioRepository usuarioRepository;
    
    @Autowired
    protected ProductoRepository productoRepository;
    
    @Autowired
    protected CompraRepository compraRepository;
    
    @Autowired
    protected PasswordEncoder passwordEncoder;
    
    protected static final String ADMIN_EMAIL = "admin@example.com";
    protected static final String ADMIN_PASSWORD = "admin123";
    
    @BeforeEach
    void limpiarBaseDeDatos() {
        compraRepository.deleteAll();
        productoRepository.deleteAll();
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
    
    protected String obtenerBasicAuthHeader(String username, String password) {
        String auth = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes());
    }
}