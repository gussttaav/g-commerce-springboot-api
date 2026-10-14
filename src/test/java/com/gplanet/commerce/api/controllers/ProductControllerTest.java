package com.gplanet.commerce.api.controllers;

import static com.gplanet.commerce.api.testutils.ApiErrorResultMatchers.errorMessage;
import static com.gplanet.commerce.api.testutils.ApiErrorResultMatchers.validationError;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_PRODUCTOS;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gplanet.commerce.api.configs.TestSecurityConfig;
import com.gplanet.commerce.api.dtos.product.ProductDTO;
import com.gplanet.commerce.api.dtos.product.ProductResponseDTO;
import com.gplanet.commerce.api.dtos.product.ProductStatus;
import com.gplanet.commerce.api.exceptions.ResourceNotFoundException;
import com.gplanet.commerce.api.services.CustomUserDetailsService;
import com.gplanet.commerce.api.services.ProductService;

@WebMvcTest(ProductController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("Producto Controller Tests")
class ProductControllerTest {

    private static final String TEST_USER_EMAIL = "test@example.com";
    private static final String TEST_ADMIN_EMAIL = "admin@example.com";
    private static final Long TEST_PRODUCT_ID = 999L;
    private static final String PATH_PRODUCT_ID_1 = "/1";
    private static final String TEST_PRODUCT_NAME = "Test Product";
    private static final String TEST_PRODUCT_DESC = "Test Description";
    private static final String TEST_PRODUCT_PRICE = "99.99";
    private static final String SORT_FIELD_NOMBRE = "name";
    private static final String SORT_DIRECTION_ASC = "ASC";
    private static final String PARAM_STATUS = "status";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private CustomUserDetailsService usuarioDetallesService;

    @BeforeEach
    void setUp() {
        // Configure ObjectMapper for consistent decimal formatting
        objectMapper.configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    // Helper methods
    private ProductDTO createValidProductoDTO() {
        return new ProductDTO(TEST_PRODUCT_NAME, TEST_PRODUCT_DESC, new BigDecimal(TEST_PRODUCT_PRICE), true);
    }

    private ProductResponseDTO createProductoResponseDTO(Long id, boolean activo) {
        return new ProductResponseDTO(id, TEST_PRODUCT_NAME, TEST_PRODUCT_DESC, 
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
    @DisplayName("GET " + ENDPOINT_PRODUCTOS)
    class ListProducts {
        
        @Test
        @WithAnonymousUser
        @DisplayName("Should return active products when user is not authenticated")
        void listProductsUnauthenticatedUserReturnsActiveProducts() throws Exception {
            // Arrange
            ProductResponseDTO product1 = createProductoResponseDTO(1L, true);
            ProductResponseDTO product2 = createProductoResponseDTO(2L, true);
            Page<ProductResponseDTO> productPage = new PageImpl<>(Arrays.asList(product1, product2));
            
            when(productService.listProducts(
                eq(ProductStatus.ACTIVE), eq(null), eq(0), eq(10), eq(SORT_FIELD_NOMBRE), eq(SORT_DIRECTION_ASC)))
                .thenReturn(productPage);

            // Act & Assert
            mockMvc.perform(get(ENDPOINT_PRODUCTOS))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(productsCount(2))
                .andExpect(firstProductId(1))
                .andExpect(secondProductId(2))
                .andExpect(totalElements(2));

            verify(productService).listProducts(
                eq(ProductStatus.ACTIVE), eq(null), eq(0), eq(10), eq(SORT_FIELD_NOMBRE), eq(SORT_DIRECTION_ASC));
        }

        @Test
        @WithMockUser(username = TEST_USER_EMAIL, roles = "USER")
        @DisplayName("Should return active products for USER role")
        void listProductsUserRoleReturnsActiveProducts() throws Exception {
            // Arrange
            ProductResponseDTO product1 = createProductoResponseDTO(1L, true);
            ProductResponseDTO product2 = createProductoResponseDTO(2L, true);
            Page<ProductResponseDTO> productPage = new PageImpl<>(Arrays.asList(product1, product2));
            
            when(productService.listProducts(
                eq(ProductStatus.ACTIVE), eq(null), eq(0), eq(10), eq(SORT_FIELD_NOMBRE), eq(SORT_DIRECTION_ASC)))
                .thenReturn(productPage);

            // Act & Assert
            mockMvc.perform(get(ENDPOINT_PRODUCTOS))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(productsCount(2))
                .andExpect(firstProductId(1))
                .andExpect(secondProductId(2))
                .andExpect(totalElements(2));

            verify(productService).listProducts(
                eq(ProductStatus.ACTIVE), eq(null), eq(0), eq(10), eq(SORT_FIELD_NOMBRE), eq(SORT_DIRECTION_ASC));
        }

        @Test
        @WithMockUser(username = TEST_USER_EMAIL, roles = "USER")
        @DisplayName("Should return 403 when USER role tries to access inactive products")
        void listProductsUserRoleAccessInactiveProductsReturns403() throws Exception {
            // Act & Assert
            mockMvc.perform(get(ENDPOINT_PRODUCTOS)
                    .param(PARAM_STATUS, "INACTIVE"))
                .andExpect(status().isForbidden());

            verifyNoInteractions(productService);
        }

        @Test
        @WithMockUser(username = TEST_ADMIN_EMAIL, roles = "ADMIN")
        @DisplayName("Should return inactive products for ADMIN role when requested")
        void listProductsAdminRoleReturnsInactiveProducts() throws Exception {
            // Arrange
            ProductResponseDTO product = createProductoResponseDTO(1L, false);
            Page<ProductResponseDTO> productPage = new PageImpl<>(Collections.singletonList(product));
            
            when(productService.listProducts(
                eq(ProductStatus.INACTIVE), eq(null), eq(0), eq(10), eq(SORT_FIELD_NOMBRE), eq(SORT_DIRECTION_ASC)))
                .thenReturn(productPage);

            // Act & Assert
            mockMvc.perform(get(ENDPOINT_PRODUCTOS)
                    .param(PARAM_STATUS, "INACTIVE"))
                .andExpect(status().isOk())
                .andExpect(productsCount(1))
                .andExpect(jsonPath("$.content[0].active").value(false))
                .andExpect(totalElements(1));

            verify(productService).listProducts(
                eq(ProductStatus.INACTIVE), eq(null), eq(0), eq(10), eq(SORT_FIELD_NOMBRE), eq(SORT_DIRECTION_ASC));
        }

        @Test
        @WithMockUser(username = TEST_ADMIN_EMAIL, roles = "ADMIN")
        @DisplayName("Should return all products for ADMIN role when requested")
        void listProductsAdminRoleReturnsAllProducts() throws Exception {
            // Arrange
            ProductResponseDTO activeProduct = createProductoResponseDTO(1L, true);
            ProductResponseDTO inactiveProduct = createProductoResponseDTO(2L, false);
            Page<ProductResponseDTO> productPage = new PageImpl<>(Arrays.asList(activeProduct, inactiveProduct));
            
            when(productService.listProducts(
                eq(ProductStatus.ALL), eq(null), eq(0), eq(10), eq(SORT_FIELD_NOMBRE), eq(SORT_DIRECTION_ASC)))
                .thenReturn(productPage);

            // Act & Assert
            mockMvc.perform(get(ENDPOINT_PRODUCTOS)
                    .param(PARAM_STATUS, "ALL"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(productsCount(2))
                .andExpect(jsonPath("$.content[0].active").value(true))
                .andExpect(jsonPath("$.content[1].active").value(false))
                .andExpect(totalElements(2));

            verify(productService).listProducts(
                eq(ProductStatus.ALL), eq(null), eq(0), eq(10), eq(SORT_FIELD_NOMBRE), eq(SORT_DIRECTION_ASC));
        }

        @Test
        @WithMockUser(username = TEST_USER_EMAIL, roles = "USER")
        @DisplayName("Should return products matching search term for USER role")
        void listProductsUserRoleWithSearchTermReturnsMatchingProducts() throws Exception {
            // Arrange
            String searchTest = "Test";
            ProductResponseDTO product = createProductoResponseDTO(1L, true);
            Page<ProductResponseDTO> productPage = new PageImpl<>(Collections.singletonList(product));
            
            when(productService.listProducts(
                eq(ProductStatus.ACTIVE), eq(searchTest), eq(0), eq(10), eq(SORT_FIELD_NOMBRE), eq(SORT_DIRECTION_ASC)))
                .thenReturn(productPage);

            // Act & Assert
            mockMvc.perform(get(ENDPOINT_PRODUCTOS)
                    .param("searchText", searchTest))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(productsCount(1))
                .andExpect(firstProductId(1))
                .andExpect(totalElements(1));

            verify(productService).listProducts(
                eq(ProductStatus.ACTIVE), eq(searchTest), eq(0), eq(10), eq(SORT_FIELD_NOMBRE), eq(SORT_DIRECTION_ASC));
        }

        @Test
        @WithMockUser(username = TEST_USER_EMAIL, roles = "USER")
        @DisplayName("Should return empty list when no active products exist")
        void listProductsNoProductsReturnsEmptyList() throws Exception {
            // Arrange
            Page<ProductResponseDTO> emptyPage = new PageImpl<>(Collections.emptyList());
            
            when(productService.listProducts(
                eq(ProductStatus.ACTIVE), eq(null), eq(0), eq(10), eq(SORT_FIELD_NOMBRE), eq(SORT_DIRECTION_ASC)))
                .thenReturn(emptyPage);

            // Act & Assert
            mockMvc.perform(get(ENDPOINT_PRODUCTOS))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(productsCount(0))
                .andExpect(totalElements(0));

            verify(productService).listProducts(
                eq(ProductStatus.ACTIVE), eq(null), eq(0), eq(10), eq(SORT_FIELD_NOMBRE), eq(SORT_DIRECTION_ASC));
        }
    }

    @Nested
    @DisplayName("POST /api/products")
    class CreateProduct {

        @Test
        @WithAnonymousUser
        @DisplayName("Should return 401 when user is not authenticated")
        void createProductUnauthenticatedUserReturns401() throws Exception {
            // Arrange
            ProductDTO productDTO = createValidProductoDTO();

            // Act & Assert
            mockMvc.perform(post(ENDPOINT_PRODUCTOS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(productDTO)))
                .andExpect(status().isUnauthorized());
            
            verifyNoInteractions(productService);
        }

        @Test
        @WithMockUser(username = TEST_USER_EMAIL, roles = "USER")
        @DisplayName("Should return 403 when user is not admin")
        void createProductNonAdminUserReturns403() throws Exception {
            // Arrange
            ProductDTO productDTO = createValidProductoDTO();

            // Act & Assert
            mockMvc.perform(post(ENDPOINT_PRODUCTOS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(productDTO)))
                .andExpect(status().isForbidden());
            
            verifyNoInteractions(productService);
        }

        @Test
        @WithMockUser(username = TEST_ADMIN_EMAIL, roles = "ADMIN")
        @DisplayName("Should create product when data is valid")
        void createProductValidDataReturnsCreatedProduct() throws Exception {
            // Arrange
            ProductDTO productDTO = createValidProductoDTO();
            ProductResponseDTO responseDTO = createProductoResponseDTO(1L, true);

            when(productService.createProduct(any(ProductDTO.class)))
                .thenReturn(responseDTO);

            // Act & Assert
            mockMvc.perform(post(ENDPOINT_PRODUCTOS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(productDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value(TEST_PRODUCT_NAME))
                .andExpect(jsonPath("$.price").value(TEST_PRODUCT_PRICE))
                .andExpect(jsonPath("$.active").value(true));

            verify(productService).createProduct(any(ProductDTO.class));
        }

        @Test
        @WithMockUser(username = TEST_ADMIN_EMAIL, roles = "ADMIN")
        @DisplayName("Should return 400 when product data is invalid")
        void createProductInvalidDataReturns400() throws Exception {
            // Arrange
            ProductDTO invalidProducto = new ProductDTO("", "", new BigDecimal("-10.00"), false);

            // Act & Assert
            mockMvc.perform(post(ENDPOINT_PRODUCTOS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidProducto)))
                .andExpect(status().isBadRequest())
                .andExpect(validationError());
            
            verifyNoInteractions(productService);
        }
    }

    @Nested
    @DisplayName("PUT /api/products/{id}")
    class UpdateProduct {

        @Test
        @WithAnonymousUser
        @DisplayName("Should return 401 when user is not authenticated")
        void updateProductUnauthenticatedUserReturns401() throws Exception {
            // Arrange
            ProductDTO productDTO = createValidProductoDTO();

            // Act & Assert
            mockMvc.perform(put(ENDPOINT_PRODUCTOS + PATH_PRODUCT_ID_1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(productDTO)))
                .andExpect(status().isUnauthorized());
            
            verifyNoInteractions(productService);
        }

        @Test
        @WithMockUser(username = TEST_USER_EMAIL, roles = "USER")
        @DisplayName("Should return 403 when user is not admin")
        void updateProductNonAdminUserReturns403() throws Exception {
            // Arrange
            ProductDTO productDTO = createValidProductoDTO();

            // Act & Assert
            mockMvc.perform(put(ENDPOINT_PRODUCTOS + PATH_PRODUCT_ID_1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(productDTO)))
                .andExpect(status().isForbidden());
            
            verifyNoInteractions(productService);
        }

        @Test
        @WithMockUser(username = TEST_ADMIN_EMAIL, roles = "ADMIN")
        @DisplayName("Should update product when data is valid")
        void updateProductValidDataReturnsUpdatedProduct() throws Exception {
            // Arrange
            Long productId = 1L;
            ProductDTO productDTO = createValidProductoDTO();
            ProductResponseDTO responseDTO = createProductoResponseDTO(productId, true);

            when(productService.updateProduct(eq(productId), any(ProductDTO.class)))
                .thenReturn(responseDTO);

            // Act & Assert
            mockMvc.perform(put(ENDPOINT_PRODUCTOS + "/" + productId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(productDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.name").value(TEST_PRODUCT_NAME))
                .andExpect(jsonPath("$.price").value(TEST_PRODUCT_PRICE))
                .andExpect(jsonPath("$.active").value(true));

            verify(productService).updateProduct(eq(productId), any(ProductDTO.class));
        }

        @Test
        @WithMockUser(username = TEST_ADMIN_EMAIL, roles = "ADMIN")
        @DisplayName("Should return 404 when product doesn't exist")
        void updateProductNonExistentProductReturns404() throws Exception {
            // Arrange
            ProductDTO productDTO = createValidProductoDTO();

            when(productService.updateProduct(eq(TEST_PRODUCT_ID), any(ProductDTO.class)))
                .thenThrow(new ResourceNotFoundException("El producto no existe"));

            // Act & Assert
            mockMvc.perform(put(ENDPOINT_PRODUCTOS + "/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(productDTO)))
                .andExpect(status().isNotFound())
                .andExpect(errorMessage("El producto no existe"));

            verify(productService).updateProduct(eq(TEST_PRODUCT_ID), any(ProductDTO.class));
        }
    }

    @Nested
    @DisplayName("DELETE /api/products/{id}")
    class RemoveProduct {

        @Test
        @WithAnonymousUser
        @DisplayName("Should return 401 when user is not authenticated")
        void removeProductUnauthenticatedUserReturns401() throws Exception {
            // Act & Assert
            mockMvc.perform(delete(ENDPOINT_PRODUCTOS + PATH_PRODUCT_ID_1))
                .andExpect(status().isUnauthorized());
            
            verifyNoInteractions(productService);
        }

        @Test
        @WithMockUser(username = TEST_USER_EMAIL, roles = "USER")
        @DisplayName("Should return 403 when user is not admin")
        void removeProductNonAdminUserReturns403() throws Exception {
            // Act & Assert
            mockMvc.perform(delete(ENDPOINT_PRODUCTOS + PATH_PRODUCT_ID_1))
                .andExpect(status().isForbidden());
            
            verifyNoInteractions(productService);
        }

        @Test
        @WithMockUser(username = TEST_ADMIN_EMAIL, roles = "ADMIN")
        @DisplayName("Should delete product when it exists")
        void removeProductExistingProductReturnsOk() throws Exception {
            // Arrange
            doNothing().when(productService).removeProduct(anyLong());

            // Act & Assert
            mockMvc.perform(delete(ENDPOINT_PRODUCTOS + PATH_PRODUCT_ID_1))
                .andExpect(status().isOk());

            verify(productService).removeProduct(1L);
        }

        @Test
        @WithMockUser(username = TEST_ADMIN_EMAIL, roles = "ADMIN")
        @DisplayName("Should return 404 when product doesn't exist")
        void removeProductNonExistentProductReturns404() throws Exception {
            // Arrange
            doThrow(new ResourceNotFoundException("Producto no encontrado"))
                .when(productService).removeProduct(anyLong());

            // Act & Assert
            mockMvc.perform(delete(ENDPOINT_PRODUCTOS + "/999"))
                .andExpect(status().isNotFound())
                .andExpect(errorMessage("Producto no encontrado"));

            verify(productService).removeProduct(TEST_PRODUCT_ID);
        }
    }
}
