package com.gplanet.commerce.api.controllers;

import static com.gplanet.commerce.api.testutils.ApiErrorResultMatchers.errorMessage;
import static com.gplanet.commerce.api.testutils.ApiErrorResultMatchers.validationError;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_PRODUCTOS_ACTUALIZAR;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_PRODUCTOS_CREAR;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_PRODUCTOS_ELIMINAR;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_PRODUCTOS_LISTAR;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gplanet.commerce.api.configs.TestSecurityConfig;
import com.gplanet.commerce.api.dtos.producto.ProductStatus;
import com.gplanet.commerce.api.dtos.producto.ProductoDTO;
import com.gplanet.commerce.api.dtos.producto.ProductoResponseDTO;
import com.gplanet.commerce.api.exceptions.ResourceNotFoundException;
import com.gplanet.commerce.api.services.ProductoService;
import com.gplanet.commerce.api.services.UsuarioDetallesService;

@WebMvcTest(ProductoController.class)
@Import(TestSecurityConfig.class)
@DisplayName("Producto Controller Tests")
class ProductoControllerTest {

    private static final String TEST_USER_EMAIL = "test@example.com";
    private static final String TEST_ADMIN_EMAIL = "admin@example.com";
    private static final Long TEST_PRODUCT_ID = 999L;
    private static final String PATH_PRODUCT_ID_1 = "/1";
    private static final String TEST_PRODUCT_NAME = "Test Product";
    private static final String TEST_PRODUCT_DESC = "Test Description";
    private static final String TEST_PRODUCT_PRICE = "99.99";
    private static final String SORT_FIELD_NOMBRE = "nombre";
    private static final String SORT_DIRECTION_ASC = "ASC";
    private static final String PARAM_STATUS = "status";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductoService productoService;

    @MockitoBean
    private UsuarioDetallesService usuarioDetallesService;

    @BeforeEach
    void setUp() {
        // Configure ObjectMapper for consistent decimal formatting
        objectMapper.configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    // Helper methods
    private ProductoDTO createValidProductoDTO() {
        return new ProductoDTO(TEST_PRODUCT_NAME, TEST_PRODUCT_DESC, new BigDecimal(TEST_PRODUCT_PRICE), true);
    }

    private ProductoResponseDTO createProductoResponseDTO(Long id, boolean activo) {
        return new ProductoResponseDTO(id, TEST_PRODUCT_NAME, TEST_PRODUCT_DESC, 
                new BigDecimal(TEST_PRODUCT_PRICE), LocalDateTime.now(), activo);
    }

    public static ResultMatcher productsCount(int size) throws Exception {
        return jsonPath("$.content", hasSize(size));
    }

    public static ResultMatcher totalElements(int total) throws Exception {
        return jsonPath("$.totalElements").value(total);
    }

    public static ResultMatcher firstProductId(int id) throws Exception {
        return jsonPath("$.content[0].id").value(id);
    }

    public static ResultMatcher secondProductId(int id) throws Exception {
        return jsonPath("$.content[1].id").value(id);
    }

    @Nested
    @DisplayName("GET " + ENDPOINT_PRODUCTOS_LISTAR)
    class ListarProductos {
        
        @Test
        @WithAnonymousUser
        @DisplayName("Should return active products when user is not authenticated")
        void listarProductosUnauthenticatedUserReturnsActiveProducts() throws Exception {
            // Arrange
            ProductoResponseDTO product1 = createProductoResponseDTO(1L, true);
            ProductoResponseDTO product2 = createProductoResponseDTO(2L, true);
            Page<ProductoResponseDTO> productPage = new PageImpl<>(Arrays.asList(product1, product2));
            
            when(productoService.listarProductos(
                eq(ProductStatus.ACTIVE), eq(null), eq(0), eq(10), eq(SORT_FIELD_NOMBRE), eq(SORT_DIRECTION_ASC)))
                .thenReturn(productPage);

            // Act & Assert
            mockMvc.perform(get(ENDPOINT_PRODUCTOS_LISTAR))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(productsCount(2))
                .andExpect(firstProductId(1))
                .andExpect(secondProductId(2))
                .andExpect(totalElements(2));

            verify(productoService).listarProductos(
                eq(ProductStatus.ACTIVE), eq(null), eq(0), eq(10), eq(SORT_FIELD_NOMBRE), eq(SORT_DIRECTION_ASC));
        }

        @Test
        @WithMockUser(username = TEST_USER_EMAIL, roles = "USER")
        @DisplayName("Should return active products for USER role")
        void listarProductosUserRoleReturnsActiveProducts() throws Exception {
            // Arrange
            ProductoResponseDTO product1 = createProductoResponseDTO(1L, true);
            ProductoResponseDTO product2 = createProductoResponseDTO(2L, true);
            Page<ProductoResponseDTO> productPage = new PageImpl<>(Arrays.asList(product1, product2));
            
            when(productoService.listarProductos(
                eq(ProductStatus.ACTIVE), eq(null), eq(0), eq(10), eq(SORT_FIELD_NOMBRE), eq(SORT_DIRECTION_ASC)))
                .thenReturn(productPage);

            // Act & Assert
            mockMvc.perform(get(ENDPOINT_PRODUCTOS_LISTAR))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(productsCount(2))
                .andExpect(firstProductId(1))
                .andExpect(secondProductId(2))
                .andExpect(totalElements(2));

            verify(productoService).listarProductos(
                eq(ProductStatus.ACTIVE), eq(null), eq(0), eq(10), eq(SORT_FIELD_NOMBRE), eq(SORT_DIRECTION_ASC));
        }

        @Test
        @WithMockUser(username = TEST_USER_EMAIL, roles = "USER")
        @DisplayName("Should return 403 when USER role tries to access inactive products")
        void listarProductosUserRoleAccessInactiveProductsReturns403() throws Exception {
            // Act & Assert
            mockMvc.perform(get(ENDPOINT_PRODUCTOS_LISTAR)
                    .param(PARAM_STATUS, "INACTIVE"))
                .andExpect(status().isForbidden());

            verifyNoInteractions(productoService);
        }

        @Test
        @WithMockUser(username = TEST_ADMIN_EMAIL, roles = "ADMIN")
        @DisplayName("Should return inactive products for ADMIN role when requested")
        void listarProductosAdminRoleReturnsInactiveProducts() throws Exception {
            // Arrange
            ProductoResponseDTO product = createProductoResponseDTO(1L, false);
            Page<ProductoResponseDTO> productPage = new PageImpl<>(Collections.singletonList(product));
            
            when(productoService.listarProductos(
                eq(ProductStatus.INACTIVE), eq(null), eq(0), eq(10), eq(SORT_FIELD_NOMBRE), eq(SORT_DIRECTION_ASC)))
                .thenReturn(productPage);

            // Act & Assert
            mockMvc.perform(get(ENDPOINT_PRODUCTOS_LISTAR)
                    .param(PARAM_STATUS, "INACTIVE"))
                .andExpect(status().isOk())
                .andExpect(productsCount(1))
                .andExpect(jsonPath("$.content[0].activo").value(false))
                .andExpect(totalElements(1));

            verify(productoService).listarProductos(
                eq(ProductStatus.INACTIVE), eq(null), eq(0), eq(10), eq(SORT_FIELD_NOMBRE), eq(SORT_DIRECTION_ASC));
        }

        @Test
        @WithMockUser(username = TEST_ADMIN_EMAIL, roles = "ADMIN")
        @DisplayName("Should return all products for ADMIN role when requested")
        void listarProductosAdminRoleReturnsAllProducts() throws Exception {
            // Arrange
            ProductoResponseDTO activeProduct = createProductoResponseDTO(1L, true);
            ProductoResponseDTO inactiveProduct = createProductoResponseDTO(2L, false);
            Page<ProductoResponseDTO> productPage = new PageImpl<>(Arrays.asList(activeProduct, inactiveProduct));
            
            when(productoService.listarProductos(
                eq(ProductStatus.ALL), eq(null), eq(0), eq(10), eq(SORT_FIELD_NOMBRE), eq(SORT_DIRECTION_ASC)))
                .thenReturn(productPage);

            // Act & Assert
            mockMvc.perform(get(ENDPOINT_PRODUCTOS_LISTAR)
                    .param(PARAM_STATUS, "ALL"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(productsCount(2))
                .andExpect(jsonPath("$.content[0].activo").value(true))
                .andExpect(jsonPath("$.content[1].activo").value(false))
                .andExpect(totalElements(2));

            verify(productoService).listarProductos(
                eq(ProductStatus.ALL), eq(null), eq(0), eq(10), eq(SORT_FIELD_NOMBRE), eq(SORT_DIRECTION_ASC));
        }

        @Test
        @WithMockUser(username = TEST_USER_EMAIL, roles = "USER")
        @DisplayName("Should return products matching search term for USER role")
        void listarProductosUserRoleWithSearchTermReturnsMatchingProducts() throws Exception {
            // Arrange
            String searchTest = "Test";
            ProductoResponseDTO product = createProductoResponseDTO(1L, true);
            Page<ProductoResponseDTO> productPage = new PageImpl<>(Collections.singletonList(product));
            
            when(productoService.listarProductos(
                eq(ProductStatus.ACTIVE), eq(searchTest), eq(0), eq(10), eq(SORT_FIELD_NOMBRE), eq(SORT_DIRECTION_ASC)))
                .thenReturn(productPage);

            // Act & Assert
            mockMvc.perform(get(ENDPOINT_PRODUCTOS_LISTAR)
                    .param("searchText", searchTest))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(productsCount(1))
                .andExpect(firstProductId(1))
                .andExpect(totalElements(1));

            verify(productoService).listarProductos(
                eq(ProductStatus.ACTIVE), eq(searchTest), eq(0), eq(10), eq(SORT_FIELD_NOMBRE), eq(SORT_DIRECTION_ASC));
        }

        @Test
        @WithMockUser(username = TEST_USER_EMAIL, roles = "USER")
        @DisplayName("Should return empty list when no active products exist")
        void listarProductosNoProductsReturnsEmptyList() throws Exception {
            // Arrange
            Page<ProductoResponseDTO> emptyPage = new PageImpl<>(Collections.emptyList());
            
            when(productoService.listarProductos(
                eq(ProductStatus.ACTIVE), eq(null), eq(0), eq(10), eq(SORT_FIELD_NOMBRE), eq(SORT_DIRECTION_ASC)))
                .thenReturn(emptyPage);

            // Act & Assert
            mockMvc.perform(get(ENDPOINT_PRODUCTOS_LISTAR))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(productsCount(0))
                .andExpect(totalElements(0));

            verify(productoService).listarProductos(
                eq(ProductStatus.ACTIVE), eq(null), eq(0), eq(10), eq(SORT_FIELD_NOMBRE), eq(SORT_DIRECTION_ASC));
        }
    }

    @Nested
    @DisplayName("POST /api/productos/crear")
    class CrearProducto {

        @Test
        @WithAnonymousUser
        @DisplayName("Should return 401 when user is not authenticated")
        void crearProductoUnauthenticatedUserReturns401() throws Exception {
            // Arrange
            ProductoDTO productoDTO = createValidProductoDTO();

            // Act & Assert
            mockMvc.perform(post(ENDPOINT_PRODUCTOS_CREAR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(productoDTO)))
                .andExpect(status().isUnauthorized());
            
            verifyNoInteractions(productoService);
        }

        @Test
        @WithMockUser(username = TEST_USER_EMAIL, roles = "USER")
        @DisplayName("Should return 403 when user is not admin")
        void crearProductoNonAdminUserReturns403() throws Exception {
            // Arrange
            ProductoDTO productoDTO = createValidProductoDTO();

            // Act & Assert
            mockMvc.perform(post(ENDPOINT_PRODUCTOS_CREAR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(productoDTO)))
                .andExpect(status().isForbidden());
            
            verifyNoInteractions(productoService);
        }

        @Test
        @WithMockUser(username = TEST_ADMIN_EMAIL, roles = "ADMIN")
        @DisplayName("Should create product when data is valid")
        void crearProductoValidDataReturnsCreatedProduct() throws Exception {
            // Arrange
            ProductoDTO productoDTO = createValidProductoDTO();
            ProductoResponseDTO responseDTO = createProductoResponseDTO(1L, true);

            when(productoService.crearProducto(any(ProductoDTO.class)))
                .thenReturn(responseDTO);

            // Act & Assert
            mockMvc.perform(post(ENDPOINT_PRODUCTOS_CREAR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(productoDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value(TEST_PRODUCT_NAME))
                .andExpect(jsonPath("$.precio").value(TEST_PRODUCT_PRICE))
                .andExpect(jsonPath("$.activo").value(true));

            verify(productoService).crearProducto(any(ProductoDTO.class));
        }

        @Test
        @WithMockUser(username = TEST_ADMIN_EMAIL, roles = "ADMIN")
        @DisplayName("Should return 400 when product data is invalid")
        void crearProductoInvalidDataReturns400() throws Exception {
            // Arrange
            ProductoDTO invalidProducto = new ProductoDTO("", "", new BigDecimal("-10.00"), false);

            // Act & Assert
            mockMvc.perform(post(ENDPOINT_PRODUCTOS_CREAR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidProducto)))
                .andExpect(status().isBadRequest())
                .andExpect(validationError())
                .andExpect(jsonPath("$.details[0]").value(containsString("precio")));
            
            verifyNoInteractions(productoService);
        }
    }

    @Nested
    @DisplayName("PUT /api/productos/actualizar/{id}")
    class ActualizarProducto {

        @Test
        @WithAnonymousUser
        @DisplayName("Should return 401 when user is not authenticated")
        void actualizarProductoUnauthenticatedUserReturns401() throws Exception {
            // Arrange
            ProductoDTO productoDTO = createValidProductoDTO();

            // Act & Assert
            mockMvc.perform(put(ENDPOINT_PRODUCTOS_ACTUALIZAR + PATH_PRODUCT_ID_1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(productoDTO)))
                .andExpect(status().isUnauthorized());
            
            verifyNoInteractions(productoService);
        }

        @Test
        @WithMockUser(username = TEST_USER_EMAIL, roles = "USER")
        @DisplayName("Should return 403 when user is not admin")
        void actualizarProductoNonAdminUserReturns403() throws Exception {
            // Arrange
            ProductoDTO productoDTO = createValidProductoDTO();

            // Act & Assert
            mockMvc.perform(put(ENDPOINT_PRODUCTOS_ACTUALIZAR + PATH_PRODUCT_ID_1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(productoDTO)))
                .andExpect(status().isForbidden());
            
            verifyNoInteractions(productoService);
        }

        @Test
        @WithMockUser(username = TEST_ADMIN_EMAIL, roles = "ADMIN")
        @DisplayName("Should update product when data is valid")
        void actualizarProductoValidDataReturnsUpdatedProduct() throws Exception {
            // Arrange
            Long productId = 1L;
            ProductoDTO productoDTO = createValidProductoDTO();
            ProductoResponseDTO responseDTO = createProductoResponseDTO(productId, true);

            when(productoService.actualizarProducto(eq(productId), any(ProductoDTO.class)))
                .thenReturn(responseDTO);

            // Act & Assert
            mockMvc.perform(put(ENDPOINT_PRODUCTOS_ACTUALIZAR + "/" + productId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(productoDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.nombre").value(TEST_PRODUCT_NAME))
                .andExpect(jsonPath("$.precio").value(TEST_PRODUCT_PRICE))
                .andExpect(jsonPath("$.activo").value(true));

            verify(productoService).actualizarProducto(eq(productId), any(ProductoDTO.class));
        }

        @Test
        @WithMockUser(username = TEST_ADMIN_EMAIL, roles = "ADMIN")
        @DisplayName("Should return 404 when product doesn't exist")
        void actualizarProductoNonExistentProductReturns404() throws Exception {
            // Arrange
            ProductoDTO productoDTO = createValidProductoDTO();

            when(productoService.actualizarProducto(eq(TEST_PRODUCT_ID), any(ProductoDTO.class)))
                .thenThrow(new ResourceNotFoundException("El producto no existe"));

            // Act & Assert
            mockMvc.perform(put(ENDPOINT_PRODUCTOS_ACTUALIZAR + "/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(productoDTO)))
                .andExpect(status().isNotFound())
                .andExpect(errorMessage("El producto no existe"));

            verify(productoService).actualizarProducto(eq(TEST_PRODUCT_ID), any(ProductoDTO.class));
        }
    }

    @Nested
    @DisplayName("DELETE /api/productos/eliminar/{id}")
    class EliminarProducto {

        @Test
        @WithAnonymousUser
        @DisplayName("Should return 401 when user is not authenticated")
        void eliminarProductoUnauthenticatedUserReturns401() throws Exception {
            // Act & Assert
            mockMvc.perform(delete(ENDPOINT_PRODUCTOS_ELIMINAR + PATH_PRODUCT_ID_1))
                .andExpect(status().isUnauthorized());
            
            verifyNoInteractions(productoService);
        }

        @Test
        @WithMockUser(username = TEST_USER_EMAIL, roles = "USER")
        @DisplayName("Should return 403 when user is not admin")
        void eliminarProductoNonAdminUserReturns403() throws Exception {
            // Act & Assert
            mockMvc.perform(delete(ENDPOINT_PRODUCTOS_ELIMINAR + PATH_PRODUCT_ID_1))
                .andExpect(status().isForbidden());
            
            verifyNoInteractions(productoService);
        }

        @Test
        @WithMockUser(username = TEST_ADMIN_EMAIL, roles = "ADMIN")
        @DisplayName("Should delete product when it exists")
        void eliminarProductoExistingProductReturnsOk() throws Exception {
            // Arrange
            doNothing().when(productoService).eliminarProducto(anyLong());

            // Act & Assert
            mockMvc.perform(delete(ENDPOINT_PRODUCTOS_ELIMINAR + PATH_PRODUCT_ID_1))
                .andExpect(status().isOk());

            verify(productoService).eliminarProducto(1L);
        }

        @Test
        @WithMockUser(username = TEST_ADMIN_EMAIL, roles = "ADMIN")
        @DisplayName("Should return 404 when product doesn't exist")
        void eliminarProductoNonExistentProductReturns404() throws Exception {
            // Arrange
            doThrow(new ResourceNotFoundException("Producto no encontrado"))
                .when(productoService).eliminarProducto(anyLong());

            // Act & Assert
            mockMvc.perform(delete(ENDPOINT_PRODUCTOS_ELIMINAR + "/999"))
                .andExpect(status().isNotFound())
                .andExpect(errorMessage("Producto no encontrado"));

            verify(productoService).eliminarProducto(TEST_PRODUCT_ID);
        }
    }
}
