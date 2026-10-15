package com.gplanet.commerce.api.integration;

import static com.gplanet.commerce.api.testutils.ApiErrorResultMatchers.errorMessage;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_PURCHASES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

import com.gplanet.commerce.api.dtos.purchase.PurchaseDTO;
import com.gplanet.commerce.api.dtos.purchase.PurchaseProductDTO;
import com.gplanet.commerce.api.dtos.purchase.PurchaseResponseDTO;
import com.gplanet.commerce.api.entities.Product;
import com.gplanet.commerce.api.entities.Purchase;
import com.gplanet.commerce.api.entities.User;

import jakarta.transaction.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PurchaseIntegrationTest extends AbstractIntegrationTest {
    private static final String USER_EMAIL = "buyer@example.com";
    private static final String USER_PASSWORD = "buyer123";
    private static final String PRODUCT_PRICE = "10.0";
    private static final String PURCHASE_TOTAL_PRICE = "20.0";
    private Long productId;
    
    @BeforeEach
    void setUpTestData() {
        // Create admin and buyer users
        createAdminUser(); // Create admin user first
        createUser("Buyer", USER_EMAIL, USER_PASSWORD, User.Role.USER);
        
        // Create product
        Product product = new Product();
        product.setName("Test Product");
        product.setDescription("Product for the purchase tests");
        product.setPrice(new BigDecimal(PRODUCT_PRICE));
        product.setActive(true);
        product.setCreationDate(LocalDateTime.now());
        product = productRepository.save(product);
        productId = product.getId();
    }
    
    @Test
    void createPurchaseAvailableProductSucceeds() throws Exception {
        // Arrange
        List<PurchaseProductDTO> products = new ArrayList<>();
        PurchaseProductDTO productDTO = new PurchaseProductDTO(productId, 2);
        products.add(productDTO);
        PurchaseDTO purchaseDTO = new PurchaseDTO(products);
        
        // Act & Assert
        MvcResult result = mockMvc.perform(post(ENDPOINT_PURCHASES)
                .header(HttpHeaders.AUTHORIZATION, obtainJwtToken(USER_EMAIL, USER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(purchaseDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(PURCHASE_TOTAL_PRICE))
                .andExpect(jsonPath("$.products[0].quantity").value(2))
                .andExpect(jsonPath("$.products[0].subtotal").value(PURCHASE_TOTAL_PRICE))
                .andReturn();
        
        // Verify purchase in database
        PurchaseResponseDTO response = objectMapper.readValue(
            result.getResponse().getContentAsString(), 
            PurchaseResponseDTO.class
        );
        
        User user = userRepository.findByEmail(USER_EMAIL).orElseThrow();
        Page<Purchase> purchasesPage = purchaseRepository.findAllByUserIdWithProducts(
            user.getId(),
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "date"))
        );
        
        assertTrue(!purchasesPage.isEmpty());
        Purchase purchaseSaved = purchasesPage.getContent().get(0);
        assertEquals(response.id(), purchaseSaved.getId());
        assertEquals(1, purchaseSaved.getProducts().size());
    }
    
    @Test
    void createPurchaseWithInactiveProductReturnsNotFound() throws Exception {
        // Arrange - Removing a product only marks it as inactive
        Product inactiveProduct = new Product();
        inactiveProduct.setName("Removed Product");
        inactiveProduct.setPrice(new BigDecimal(PRODUCT_PRICE));
        inactiveProduct.setActive(false);
        inactiveProduct.setCreationDate(LocalDateTime.now());
        Long inactiveProductId = productRepository.save(inactiveProduct).getId();

        PurchaseDTO purchaseDTO = new PurchaseDTO(List.of(
            new PurchaseProductDTO(productId, 1),
            new PurchaseProductDTO(inactiveProductId, 1)));

        // Act & Assert
        mockMvc.perform(post(ENDPOINT_PURCHASES)
                .header(HttpHeaders.AUTHORIZATION, obtainJwtToken(USER_EMAIL, USER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(purchaseDTO)))
                .andExpect(status().isNotFound())
                .andExpect(errorMessage("Product " + inactiveProductId + " not found"));

        assertEquals(0, purchaseRepository.count());
    }

    @Test
    void listPurchasesAuthenticatedUserReturnsPurchases() throws Exception {
        // First create a purchase
        createPurchaseAvailableProductSucceeds();
        
        // Act & Assert - List purchases
        mockMvc.perform(get(ENDPOINT_PURCHASES)
                .header(HttpHeaders.AUTHORIZATION, obtainJwtToken(USER_EMAIL, USER_PASSWORD))
                .param("page", "0")
                .param("size", "10")
                .param("sort", "date")
                .param("direction", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].total").value(PURCHASE_TOTAL_PRICE))
                .andExpect(jsonPath("$.content[0].products").isArray())
                .andExpect(jsonPath("$.content[0].products[0].quantity").value(2))
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber())
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.pageNumber").value(0));
    }
}
