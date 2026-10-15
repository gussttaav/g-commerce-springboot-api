package com.gplanet.commerce.api.integration;

import static com.gplanet.commerce.api.testutils.ApiErrorResultMatchers.errorDetailContains;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_PRODUCTOS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

import com.gplanet.commerce.api.dtos.product.ProductDTO;
import com.gplanet.commerce.api.dtos.product.ProductResponseDTO;
import com.gplanet.commerce.api.entities.User;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductIntegrationTest extends AbstractIntegrationTest {
    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String USER_EMAIL = "user@example.com";
    private static final String USER_PASSWORD = "user123";
    private static final String ENDPOINT_ACTUALIZAR = ENDPOINT_PRODUCTOS + "/";
    
    @Test
    void crearProductoAdminCreaCorrectamente() throws Exception {
        // Arrange
        crearUsuarioAdmin();
        ProductDTO productDTO = new ProductDTO(
            "Nuevo Producto", 
            "Descripción del producto", 
            new BigDecimal("99.99"), true);
        
        // Act & Assert
        mockMvc.perform(post(ENDPOINT_PRODUCTOS)
                .header(HttpHeaders.AUTHORIZATION, obtenerJwtToken(ADMIN_EMAIL, ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nuevo Producto"))
                .andExpect(jsonPath("$.price").value("99.99"));
    }
    
    @Test
    void listProductsUnknownSortFieldReturnsBadRequest() throws Exception {
        // Act & Assert - The sort field was passed to the database query, which failed with a 500
        mockMvc.perform(get(ENDPOINT_PRODUCTOS)
                .param("sort", "unknown"))
                .andExpect(status().isBadRequest())
                .andExpect(errorDetailContains("sort: must be one of name|price"));
    }

    @Test
    void crearProductoUsuarioRetornaForbidden() throws Exception {
        // Create regular user
        User user = new User();
        user.setName("User");
        user.setEmail(USER_EMAIL);
        user.setPassword(passwordEncoder.encode(USER_PASSWORD));
        user.setRole(User.Role.USER);
        user.setCreationDate(LocalDateTime.now());
        userRepository.save(user);

        // Arrange
        ProductDTO productDTO = new ProductDTO(
            "Producto No Autorizado", 
            "Descripción", 
            new BigDecimal("50.00"), true);
        
        // Act & Assert
        mockMvc.perform(post(ENDPOINT_PRODUCTOS)
                .header(HttpHeaders.AUTHORIZATION, obtenerJwtToken(USER_EMAIL, USER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void actualizarProductoAdminActualizaCorrectamente() throws Exception {
        // Arrange
        crearUsuarioAdmin();
        
        // First create a product
        ProductDTO originalProducto = new ProductDTO(
            "Producto Original", 
            "Descripción original", 
            new BigDecimal("49.99"), true);
        
        MvcResult createResult = mockMvc.perform(post(ENDPOINT_PRODUCTOS)
                .header(HttpHeaders.AUTHORIZATION, obtenerJwtToken(ADMIN_EMAIL, ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(originalProducto)))
                .andReturn();
        
        ProductResponseDTO createdProducto = objectMapper.readValue(
            createResult.getResponse().getContentAsString(),
            ProductResponseDTO.class
        );
        
        // Prepare update data
        ProductDTO updateProducto = new ProductDTO(
            "Producto Actualizado", 
            "Nueva descripción", 
            new BigDecimal("149.99"), true);
        
        // Act & Assert
        mockMvc.perform(put(ENDPOINT_ACTUALIZAR + createdProducto.id())
                .header(HttpHeaders.AUTHORIZATION, obtenerJwtToken(ADMIN_EMAIL, ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateProducto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Producto Actualizado"))
                .andExpect(jsonPath("$.description").value("Nueva descripción"))
                .andExpect(jsonPath("$.price").value("149.99"));
    }

    @Test
    void actualizarProductoUsuarioRetornaForbidden() throws Exception {
        // Arrange
        crearUsuarioAdmin();
        crearUsuario("Regular user", USER_EMAIL, USER_PASSWORD, User.Role.USER);
        
        // First create a product as admin
        ProductDTO originalProducto = new ProductDTO(
            "Producto Original", 
            "Descripción original", 
            new BigDecimal("17.99"), true);
        
        MvcResult createResult = mockMvc.perform(post(ENDPOINT_PRODUCTOS)
                .header(HttpHeaders.AUTHORIZATION, obtenerJwtToken(ADMIN_EMAIL, ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(originalProducto)))
                .andReturn();
        
        ProductResponseDTO createdProducto = objectMapper.readValue(
            createResult.getResponse().getContentAsString(),
            ProductResponseDTO.class
        );
        
        // Attempt to update as regular user
        ProductDTO updateProducto = new ProductDTO(
            "Intento Actualización", 
            "Descripción", 
            new BigDecimal("50.00"), true);
        
        // Act & Assert
        mockMvc.perform(put(ENDPOINT_ACTUALIZAR + createdProducto.id())
                .header(HttpHeaders.AUTHORIZATION, obtenerJwtToken(USER_EMAIL, USER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateProducto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void actualizarProductoProductoNoExistenteRetornaNotFound() throws Exception {
        // Arrange
        crearUsuarioAdmin();
        ProductDTO updateProducto = new ProductDTO(
            "Producto Inexistente", 
            "Descripción de Producto Inexistente", 
            new BigDecimal("19.99"), true);
        
        // Act & Assert
        mockMvc.perform(put(ENDPOINT_ACTUALIZAR + "999")
                .header(HttpHeaders.AUTHORIZATION, obtenerJwtToken(ADMIN_EMAIL, ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateProducto)))
                .andExpect(status().isNotFound());
    }
}
