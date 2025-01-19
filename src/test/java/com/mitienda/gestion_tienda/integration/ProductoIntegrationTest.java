package com.mitienda.gestion_tienda.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import com.mitienda.gestion_tienda.dtos.producto.ProductoDTO;
import com.mitienda.gestion_tienda.entities.Usuario;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductoIntegrationTest extends BaseIntegrationTest {
    private static final String BASE_URL = "/api/productos";
    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String USER_EMAIL = "user@example.com";
    private static final String USER_PASSWORD = "user123";
    
    @Test
    void crearProducto_Admin_CreaCorrectamente() throws Exception {
        // Arrange
        crearUsuarioAdmin();
        ProductoDTO productoDTO = ProductoDTO.builder()
            .nombre("Nuevo Producto")
            .descripcion("Descripción del producto")
            .precio(new BigDecimal("99.99"))
            .build();
        
        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/crear")
                .header(HttpHeaders.AUTHORIZATION, obtenerBasicAuthHeader(ADMIN_EMAIL, ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productoDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Nuevo Producto"))
                .andExpect(jsonPath("$.precio").value("99.99"));
    }
    
    @Test
    void crearProducto_Usuario_RetornaForbidden() throws Exception {
        // Create regular user
        Usuario user = new Usuario();
        user.setNombre("User");
        user.setEmail(USER_EMAIL);
        user.setPassword(passwordEncoder.encode(USER_PASSWORD));
        user.setRol(Usuario.Role.USER);
        user.setFechaCreacion(LocalDateTime.now());
        usuarioRepository.save(user);

        // Arrange
        ProductoDTO productoDTO = ProductoDTO.builder()
            .nombre("Producto No Autorizado")
            .precio(new BigDecimal("50.00"))
            .build();
        
        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/crear")
                .header(HttpHeaders.AUTHORIZATION, obtenerBasicAuthHeader(USER_EMAIL, USER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productoDTO)))
                .andExpect(status().isForbidden());
    }
}