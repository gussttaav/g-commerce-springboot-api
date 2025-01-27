package com.mitienda.gestion_tienda.controllers;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mitienda.gestion_tienda.dtos.producto.ProductStatus;
import com.mitienda.gestion_tienda.dtos.producto.ProductoDTO;
import com.mitienda.gestion_tienda.dtos.producto.ProductoResponseDTO;
import com.mitienda.gestion_tienda.exceptions.ResourceNotFoundException;
import com.mitienda.gestion_tienda.services.ProductoService;
import com.mitienda.gestion_tienda.services.UsuarioDetallesService;

@WebMvcTest(ProductoController.class)
@Import(TestSecurityConfig.class)
@DisplayName("Producto Controller Tests")
class ProductoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioDetallesService userDetailsService;

    @MockitoBean
    private ProductoService productoService;

    @Autowired
    private ObjectMapper objectMapper;

    // Test Data Helper Methods
    private ProductoDTO createValidProductoDTO() {
        return new ProductoDTO("Test Product",
        "Test Description", new BigDecimal("99.99"), true);
    }

    private ProductoResponseDTO createProductoResponseDTO(Long id, boolean activo) {
        return new ProductoResponseDTO(id, "Test Product", "Test Description", 
                new BigDecimal("99.99"), LocalDateTime.now(), activo);
    }

    @Nested
    @DisplayName("GET /api/productos/listar")
    class ListarProductos {
        
        @Test
        @WithAnonymousUser
        @DisplayName("Should return 401 when user is not authenticated")
        void listarProductos_UnauthenticatedUser_Returns401() throws Exception {
            mockMvc.perform(get("/api/productos")
                    .with(csrf()))
                    .andExpect(status().isUnauthorized())
                    .andDo(MockMvcResultHandlers.print());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return empty list when no active products exist")
        void listarProductos_NoProducts_ReturnsEmptyList() throws Exception {
            when(productoService.listarProductos(ProductStatus.ACTIVE))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/productos/listar")
                    .with(csrf())
                    .with(user("test@example.com").roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(0)))
                    .andDo(MockMvcResultHandlers.print());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return list of active products for USER role")
        void listarProductos_UserRole_ReturnsActiveProducts() throws Exception {
            ProductoResponseDTO product1 = createProductoResponseDTO(1L, true);
            ProductoResponseDTO product2 = createProductoResponseDTO(2L, true);
            
            when(productoService.listarProductos(ProductStatus.ACTIVE))
                    .thenReturn(Arrays.asList(product1, product2));

            mockMvc.perform(get("/api/productos/listar")
                    .with(csrf())
                    .with(user("test@example.com").roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[1].id").value(2))
                    .andDo(MockMvcResultHandlers.print());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return 403 when USER role tries to access inactive products")
        void listarProductos_UserRole_AccessInactiveProducts_Returns403() throws Exception {
            mockMvc.perform(get("/api/productos")
                    .param("status", "INACTIVE")
                    .with(csrf())
                    .with(user("test@example.com").roles("USER")))
                    .andExpect(status().isForbidden())
                    .andDo(MockMvcResultHandlers.print());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return active products for ADMIN role")
        void listarProductos_AdminRole_ReturnsActiveProducts() throws Exception {
            ProductoResponseDTO product = createProductoResponseDTO(1L, true);
            
            when(productoService.listarProductos(ProductStatus.ACTIVE))
                    .thenReturn(Collections.singletonList(product));

            mockMvc.perform(get("/api/productos/listar")
                    .with(csrf())
                    .with(user("admin@example.com").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andDo(MockMvcResultHandlers.print());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return inactive products for ADMIN role when requested")
        void listarProductos_AdminRole_ReturnsInactiveProducts() throws Exception {
            ProductoResponseDTO product = createProductoResponseDTO(1L, false);
            
            when(productoService.listarProductos(ProductStatus.INACTIVE))
                    .thenReturn(Collections.singletonList(product));

            mockMvc.perform(get("/api/productos/listar")
                    .param("status", "INACTIVE")
                    .with(csrf())
                    .with(user("admin@example.com").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].activo").value(false))
                    .andDo(MockMvcResultHandlers.print());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return all products for ADMIN role when requested")
        void listarProductos_AdminRole_ReturnsAllProducts() throws Exception {
            ProductoResponseDTO activeProduct = createProductoResponseDTO(1L, true);
            ProductoResponseDTO inactiveProduct = createProductoResponseDTO(2L, false);
            
            when(productoService.listarProductos(ProductStatus.ALL))
                    .thenReturn(Arrays.asList(activeProduct, inactiveProduct));

            mockMvc.perform(get("/api/productos/listar")
                    .param("status", "ALL")
                    .with(csrf())
                    .with(user("admin@example.com").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].activo").value(true))
                    .andExpect(jsonPath("$[1].activo").value(false))
                    .andDo(MockMvcResultHandlers.print());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return 400 for invalid status parameter")
        void listarProductos_AdminRole_InvalidStatus_Returns400() throws Exception {
            mockMvc.perform(get("/api/productos/listar")
                    .param("status", "INVALID_STATUS")
                    .with(csrf())
                    .with(user("admin@example.com").roles("ADMIN")))
                    .andExpect(status().isBadRequest())
                    .andDo(MockMvcResultHandlers.print());
        }
    }

    @Nested
    @WithAnonymousUser
    @DisplayName("POST /api/productos/crear")
    class CrearProducto {

        @Test
        @DisplayName("Should return 401 when user is not authenticated")
        void crearProducto_UnauthenticatedUser_Returns401() throws Exception {
            ProductoDTO productoDTO = createValidProductoDTO();

            mockMvc.perform(post("/api/productos/crear")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(productoDTO)))
                    .andExpect(status().isUnauthorized())
                    .andDo(MockMvcResultHandlers.print());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return 403 when user is not admin")
        void crearProducto_NonAdminUser_Returns403() throws Exception {
            ProductoDTO productoDTO = createValidProductoDTO();

            mockMvc.perform(post("/api/productos/crear")
                    .with(csrf())
                    .with(user("test@example.com").roles("USER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(productoDTO)))
                    .andExpect(status().isForbidden())
                    .andDo(MockMvcResultHandlers.print());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should create product when data is valid")
        void crearProducto_ValidData_ReturnsCreatedProduct() throws Exception {
            ProductoDTO productoDTO = createValidProductoDTO();
            ProductoResponseDTO responseDTO = createProductoResponseDTO(1L,true);

            when(productoService.crearProducto(any(ProductoDTO.class)))
                    .thenReturn(responseDTO);

            mockMvc.perform(post("/api/productos/crear")
                    .with(csrf())
                    .with(user("admin@example.com").roles("ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(productoDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.nombre").value("Test Product"))
                    .andExpect(jsonPath("$.precio").value("99.99"))
                    .andExpect(jsonPath("$.activo").value(true))
                    .andDo(MockMvcResultHandlers.print());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return 400 when product data is invalid")
        void crearProducto_InvalidData_Returns400() throws Exception {
            ProductoDTO invalidProducto = ProductoDTO.builder().build();
            // Empty product data

            mockMvc.perform(post("/api/productos/crear")
                    .with(csrf())
                    .with(user("admin@example.com").roles("ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidProducto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Validation Error"))
                    .andDo(MockMvcResultHandlers.print());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return 400 when price is negative")
        void crearProducto_NegativePrice_Returns400() throws Exception {
            ProductoDTO productoDTO = createValidProductoDTO();
            productoDTO.setPrecio(new BigDecimal("-10.00"));

            mockMvc.perform(post("/api/productos/crear")
                    .with(csrf())
                    .with(user("admin@example.com").roles("ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(productoDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Validation Error"))
                    .andExpect(jsonPath("$.details[0]").value(containsString("precio")))
                    .andDo(MockMvcResultHandlers.print());
        }
    }

    @Nested
    @WithAnonymousUser
    @DisplayName("DELETE /api/productos/eliminar/{id}")
    class EliminarProducto {

        @Test
        @DisplayName("Should return 401 when user is not authenticated")
        void eliminarProducto_UnauthenticatedUser_Returns401() throws Exception {
            mockMvc.perform(delete("/api/productos/eliminar/1")
                    .with(csrf()))
                    .andExpect(status().isUnauthorized())
                    .andDo(MockMvcResultHandlers.print());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return 403 when user is not admin")
        void eliminarProducto_NonAdminUser_Returns403() throws Exception {
            mockMvc.perform(delete("/api/productos/eliminar/1")
                    .with(csrf())
                    .with(user("test@example.com").roles("USER")))
                    .andExpect(status().isForbidden())
                    .andDo(MockMvcResultHandlers.print());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should delete product when it exists")
        void eliminarProducto_ExistingProduct_ReturnsOk() throws Exception {
            doNothing().when(productoService).eliminarProducto(anyLong());

            mockMvc.perform(delete("/api/productos/eliminar/1")
                    .with(csrf())
                    .with(user("admin@example.com").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andDo(MockMvcResultHandlers.print());

            verify(productoService).eliminarProducto(1L);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return 404 when product doesn't exist")
        void eliminarProducto_NonExistentProduct_Returns404() throws Exception {
            doThrow(new ResourceNotFoundException("Producto no encontrado"))
                    .when(productoService).eliminarProducto(anyLong());

            mockMvc.perform(delete("/api/productos/eliminar/999")
                    .with(csrf())
                    .with(user("admin@example.com").roles("ADMIN")))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Producto no encontrado"))
                    .andDo(MockMvcResultHandlers.print());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return 400 when ID is invalid")
        void eliminarProducto_InvalidId_Returns400() throws Exception {
            mockMvc.perform(delete("/api/productos/eliminar/-1")
                    .with(csrf())
                    .with(user("admin@example.com").roles("ADMIN")))
                    .andExpect(status().isBadRequest())
                    .andDo(MockMvcResultHandlers.print());
        }
    }

    private UserDetails mockUser(String username, String... roles) {
        return User.builder()
                .username(username)
                .password("password")
                .roles(roles)
                .build();
    }

    @BeforeEach
    void setup() {
        // Configure UserDetailsService to return the mock users
        when(userDetailsService.loadUserByUsername("test@example.com"))
            .thenReturn(mockUser("test@example.com", "USER"));
        when(userDetailsService.loadUserByUsername("admin@example.com"))
            .thenReturn(mockUser("admin@example.com", "ADMIN"));
    }
}
