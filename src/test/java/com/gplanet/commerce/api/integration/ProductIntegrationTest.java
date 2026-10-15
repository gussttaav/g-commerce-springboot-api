package com.gplanet.commerce.api.integration;

import static com.gplanet.commerce.api.testutils.ApiErrorResultMatchers.errorDetailContains;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_PRODUCTS;
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
    private static final String ENDPOINT_PRODUCT = ENDPOINT_PRODUCTS + "/";
    
    @Test
    void createProductAsAdminCreatesProduct() throws Exception {
        // Arrange
        createAdminUser();
        ProductDTO productDTO = new ProductDTO(
            "New Product", 
            "Product description", 
            new BigDecimal("99.99"), true);
        
        // Act & Assert
        mockMvc.perform(post(ENDPOINT_PRODUCTS)
                .header(HttpHeaders.AUTHORIZATION, obtainJwtToken(ADMIN_EMAIL, ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Product"))
                .andExpect(jsonPath("$.price").value("99.99"));
    }
    
    @Test
    void listProductsUnknownSortFieldReturnsBadRequest() throws Exception {
        // Act & Assert - The sort field was passed to the database query, which failed with a 500
        mockMvc.perform(get(ENDPOINT_PRODUCTS)
                .param("sort", "unknown"))
                .andExpect(status().isBadRequest())
                .andExpect(errorDetailContains("sort: must be one of name|price"));
    }

    @Test
    void createProductAsUserReturnsForbidden() throws Exception {
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
            "Unauthorized Product", 
            "Description", 
            new BigDecimal("50.00"), true);
        
        // Act & Assert
        mockMvc.perform(post(ENDPOINT_PRODUCTS)
                .header(HttpHeaders.AUTHORIZATION, obtainJwtToken(USER_EMAIL, USER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateProductAsAdminUpdatesProduct() throws Exception {
        // Arrange
        createAdminUser();
        
        // First create a product
        ProductDTO originalProduct = new ProductDTO(
            "Original Product", 
            "Original description", 
            new BigDecimal("49.99"), true);
        
        MvcResult createResult = mockMvc.perform(post(ENDPOINT_PRODUCTS)
                .header(HttpHeaders.AUTHORIZATION, obtainJwtToken(ADMIN_EMAIL, ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(originalProduct)))
                .andReturn();
        
        ProductResponseDTO createdProduct = objectMapper.readValue(
            createResult.getResponse().getContentAsString(),
            ProductResponseDTO.class
        );
        
        // Prepare update data
        ProductDTO productUpdate = new ProductDTO(
            "Updated Product", 
            "New description", 
            new BigDecimal("149.99"), true);
        
        // Act & Assert
        mockMvc.perform(put(ENDPOINT_PRODUCT + createdProduct.id())
                .header(HttpHeaders.AUTHORIZATION, obtainJwtToken(ADMIN_EMAIL, ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Product"))
                .andExpect(jsonPath("$.description").value("New description"))
                .andExpect(jsonPath("$.price").value("149.99"));
    }

    @Test
    void updateProductAsUserReturnsForbidden() throws Exception {
        // Arrange
        createAdminUser();
        createUser("Regular user", USER_EMAIL, USER_PASSWORD, User.Role.USER);
        
        // First create a product as admin
        ProductDTO originalProduct = new ProductDTO(
            "Original Product", 
            "Original description", 
            new BigDecimal("17.99"), true);
        
        MvcResult createResult = mockMvc.perform(post(ENDPOINT_PRODUCTS)
                .header(HttpHeaders.AUTHORIZATION, obtainJwtToken(ADMIN_EMAIL, ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(originalProduct)))
                .andReturn();
        
        ProductResponseDTO createdProduct = objectMapper.readValue(
            createResult.getResponse().getContentAsString(),
            ProductResponseDTO.class
        );
        
        // Attempt to update as regular user
        ProductDTO productUpdate = new ProductDTO(
            "Update Attempt", 
            "Description", 
            new BigDecimal("50.00"), true);
        
        // Act & Assert
        mockMvc.perform(put(ENDPOINT_PRODUCT + createdProduct.id())
                .header(HttpHeaders.AUTHORIZATION, obtainJwtToken(USER_EMAIL, USER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productUpdate)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateNonExistentProductReturnsNotFound() throws Exception {
        // Arrange
        createAdminUser();
        ProductDTO productUpdate = new ProductDTO(
            "Nonexistent Product", 
            "Description of a nonexistent product", 
            new BigDecimal("19.99"), true);
        
        // Act & Assert
        mockMvc.perform(put(ENDPOINT_PRODUCT + "999")
                .header(HttpHeaders.AUTHORIZATION, obtainJwtToken(ADMIN_EMAIL, ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productUpdate)))
                .andExpect(status().isNotFound());
    }
}
