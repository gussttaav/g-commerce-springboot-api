package com.gplanet.commerce_api.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.test.web.servlet.MvcResult;

import com.gplanet.commerce_api.dtos.producto.ProductoDTO;
import com.gplanet.commerce_api.dtos.producto.ProductoResponseDTO;
import com.gplanet.commerce_api.entities.Usuario;

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
        ProductoDTO productoDTO = new ProductoDTO("Nuevo Producto", "Descripción del producto", new BigDecimal("99.99"), true);
        
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
        ProductoDTO productoDTO = new ProductoDTO("Producto No Autorizado", "Descripción", new BigDecimal("50.00"), true);
        
        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/crear")
                .header(HttpHeaders.AUTHORIZATION, obtenerBasicAuthHeader(USER_EMAIL, USER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productoDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void actualizarProducto_Admin_ActualizaCorrectamente() throws Exception {
        // Arrange
        crearUsuarioAdmin();
        
        // First create a product
        ProductoDTO originalProducto = new ProductoDTO("Producto Original", "Descripción original", new BigDecimal("99.99"), true);
        
        MvcResult createResult = mockMvc.perform(post(BASE_URL + "/crear")
                .header(HttpHeaders.AUTHORIZATION, obtenerBasicAuthHeader(ADMIN_EMAIL, ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(originalProducto)))
                .andReturn();
        
        ProductoResponseDTO createdProducto = objectMapper.readValue(
            createResult.getResponse().getContentAsString(),
            ProductoResponseDTO.class
        );
        
        // Prepare update data
        ProductoDTO updateProducto = new ProductoDTO("Producto Actualizado", "Nueva descripción", new BigDecimal("149.99"), true);
        
        // Act & Assert
        mockMvc.perform(put(BASE_URL + "/actualizar/" + createdProducto.id())
                .header(HttpHeaders.AUTHORIZATION, obtenerBasicAuthHeader(ADMIN_EMAIL, ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateProducto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Producto Actualizado"))
                .andExpect(jsonPath("$.descripcion").value("Nueva descripción"))
                .andExpect(jsonPath("$.precio").value("149.99"));
    }

    @Test
    void actualizarProducto_Usuario_RetornaForbidden() throws Exception {
        // Arrange
        crearUsuarioAdmin();
        crearUsuario("Regular user", USER_EMAIL, USER_PASSWORD, Usuario.Role.USER);
        
        // First create a product as admin
        ProductoDTO originalProducto = new ProductoDTO("Producto Original", "Descripción original", new BigDecimal("99.99"), true);
        
        MvcResult createResult = mockMvc.perform(post(BASE_URL + "/crear")
                .header(HttpHeaders.AUTHORIZATION, obtenerBasicAuthHeader(ADMIN_EMAIL, ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(originalProducto)))
                .andReturn();
        
        ProductoResponseDTO createdProducto = objectMapper.readValue(
            createResult.getResponse().getContentAsString(),
            ProductoResponseDTO.class
        );
        
        // Attempt to update as regular user
        ProductoDTO updateProducto = new ProductoDTO("Intento Actualización", "Descripción", new BigDecimal("50.00"), true);
        
        // Act & Assert
        mockMvc.perform(put(BASE_URL + "/actualizar/" + createdProducto.id())
                .header(HttpHeaders.AUTHORIZATION, obtenerBasicAuthHeader(USER_EMAIL, USER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateProducto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void actualizarProducto_ProductoNoExistente_RetornaNotFound() throws Exception {
        // Arrange
        crearUsuarioAdmin();
        ProductoDTO updateProducto = new ProductoDTO("Producto Inexistente", "Descripción", new BigDecimal("99.99"), true);
        
        // Act & Assert
        mockMvc.perform(put(BASE_URL + "/actualizar/999")
                .header(HttpHeaders.AUTHORIZATION, obtenerBasicAuthHeader(ADMIN_EMAIL, ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateProducto)))
                .andExpect(status().isNotFound());
    }
}