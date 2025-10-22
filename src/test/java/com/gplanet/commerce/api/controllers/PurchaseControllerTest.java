package com.gplanet.commerce.api.controllers;

import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_COMPRAS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gplanet.commerce.api.configs.TestSecurityConfig;
import com.gplanet.commerce.api.dtos.purchase.PurchaseDTO;
import com.gplanet.commerce.api.dtos.purchase.PurchaseProductDTO;
import com.gplanet.commerce.api.dtos.purchase.PurchaseProductResponseDTO;
import com.gplanet.commerce.api.dtos.purchase.PurchaseResponseDTO;
import com.gplanet.commerce.api.services.CustomUserDetailsService;
import com.gplanet.commerce.api.services.PurchaseService;

@WebMvcTest(PurchaseController.class)
@Import(TestSecurityConfig.class)
class PurchaseControllerTest {

    public static final PageRequest PAGE_REQUEST_DESC_BY_FECHA = 
        PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "fecha"));
    
    public static final BigDecimal PRODUCT_PRICE_50 = new BigDecimal("50.00");
    public static final BigDecimal PRODUCT_PRICE_75 = new BigDecimal("75.00");
    public static final BigDecimal COMPRA_TOTAL_100 = new BigDecimal("100.00");
    public static final BigDecimal COMPRA_TOTAL_150 = new BigDecimal("150.00");
    private static final String TEST_USER_EMAIL = "test@example.com";
    private static final String TEST_ADMIN_EMAIL = "admin@example.com";
    private static final String SORT_FIELD_FECHA = "fecha";
    private static final String SORT_DIRECTION_DESC = "DESC";
    
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PurchaseService purchaseService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;
    
    @BeforeEach
    void setUp() {
        // Configure ObjectMapper for consistent decimal formatting
        objectMapper.configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Test
    @WithMockUser(username = TEST_USER_EMAIL, roles = "USER")
    @DisplayName("Should create purchase successfully when valid data is provided")
    void createPurchaseValidDataReturnsCreatedPurchase() throws Exception {
        // Arrange
        PurchaseDTO requestDTO = createValidPurchaseDTO();
        PurchaseResponseDTO responseDTO = createMockPurchaseResponseDTO();
        float expectedTotal = responseDTO.total().floatValue();

        when(purchaseService.createPurchase(eq(TEST_USER_EMAIL), any(PurchaseDTO.class)))
            .thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(post(ENDPOINT_COMPRAS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(responseDTO.id()))
            .andExpect(jsonPath("$.userName").value(responseDTO.userName()))
            .andExpect(jsonPath("$.total").value(expectedTotal))
            .andExpect(jsonPath("$.products", Matchers.hasSize(2)));

        verify(purchaseService).createPurchase(eq(TEST_USER_EMAIL), any(PurchaseDTO.class));
    }

    @Test
    @DisplayName("Should return 401 when user is not authenticated")
    void createPurchaseUnauthenticatedUserReturnsUnauthorized() throws Exception {
        // Arrange
        PurchaseDTO validPurchaseDTO = createValidPurchaseDTO();

        // Act & Assert
        mockMvc.perform(post(ENDPOINT_COMPRAS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validPurchaseDTO)))
            .andExpect(status().isUnauthorized());
            
        verifyNoInteractions(purchaseService);
    }

    @Test
    @WithMockUser(username = TEST_USER_EMAIL, roles = "USER")
    @DisplayName("Should return all purchases for authenticated user")
    void listPurchasesAuthenticatedUserReturnsUserPurchases() throws Exception {
        // Arrange
        Page<PurchaseResponseDTO> page = new PageImpl<>(
            createMockPurchasesList(),
            PAGE_REQUEST_DESC_BY_FECHA,
            2
        );
        
        when(purchaseService.listPurchases(
                eq(TEST_USER_EMAIL), 
                eq(0), 
                eq(10), 
                eq(SORT_FIELD_FECHA), 
                eq(SORT_DIRECTION_DESC)))
            .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get(ENDPOINT_COMPRAS)
                .param("page", "0")
                .param("size", "10")
                .param("sort", SORT_FIELD_FECHA)
                .param("direction", SORT_DIRECTION_DESC))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", Matchers.hasSize(2)))
            .andExpect(jsonPath("$.totalElements").value(2));

        verify(purchaseService).listPurchases(
            eq(TEST_USER_EMAIL), 
            eq(0), 
            eq(10), 
            eq(SORT_FIELD_FECHA), 
            eq(SORT_DIRECTION_DESC));
    }

    @Test
    @WithMockUser(username = TEST_ADMIN_EMAIL, roles = "ADMIN")
    @DisplayName("Should return all purchases for admin user")
    void listPurchasesAdminUserReturnsAllPurchases() throws Exception {
        // Arrange
        Page<PurchaseResponseDTO> page = new PageImpl<>(
            createMockPurchasesList(),
            PAGE_REQUEST_DESC_BY_FECHA,
            2
        );
        
        when(purchaseService.listPurchases(
                eq(TEST_ADMIN_EMAIL), 
                eq(0), 
                eq(10), 
                eq(SORT_FIELD_FECHA), 
                eq(SORT_DIRECTION_DESC)))
            .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get(ENDPOINT_COMPRAS)
                .param("page", "0")
                .param("size", "10")
                .param("sort", SORT_FIELD_FECHA)
                .param("direction", SORT_DIRECTION_DESC))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", Matchers.hasSize(2)));

        verify(purchaseService).listPurchases(
            eq(TEST_ADMIN_EMAIL), 
            eq(0), 
            eq(10), 
            eq(SORT_FIELD_FECHA), 
            eq(SORT_DIRECTION_DESC));
    }

    // Helper methods
    private PurchaseDTO createValidPurchaseDTO() {
        List<PurchaseProductDTO> productos = new ArrayList<>();
        
        PurchaseProductDTO producto1 = new PurchaseProductDTO(1L, 2);
        PurchaseProductDTO producto2 = new PurchaseProductDTO(2L, 1);
        
        productos.add(producto1);
        productos.add(producto2);
        
        return new PurchaseDTO(productos);
    }

    private PurchaseResponseDTO createMockPurchaseResponseDTO() {
        List<PurchaseProductResponseDTO> productos = new ArrayList<>();
        
        PurchaseProductResponseDTO producto1 = new PurchaseProductResponseDTO(
            1L, "Producto 1", PRODUCT_PRICE_50, 2, COMPRA_TOTAL_100
        );
        
        PurchaseProductResponseDTO producto2 = new PurchaseProductResponseDTO(
            2L, "Producto 2", PRODUCT_PRICE_50, 1, PRODUCT_PRICE_50
        );
        
        productos.add(producto1);
        productos.add(producto2);
        
        return new PurchaseResponseDTO(
            1L, "Test User", LocalDateTime.now(), COMPRA_TOTAL_150, productos
        );
    }

    private List<PurchaseResponseDTO> createMockPurchasesList() {
        List<PurchaseResponseDTO> purchases = new ArrayList<>();
        
        // First purchase
        PurchaseResponseDTO purchase1 = createMockPurchaseResponseDTO();
        
        // Second purchase with different data
        List<PurchaseProductResponseDTO> productos2 = new ArrayList<>();
        PurchaseProductResponseDTO producto = new PurchaseProductResponseDTO(
            3L, "Producto 3", PRODUCT_PRICE_75, 1, PRODUCT_PRICE_75
        );
        productos2.add(producto);
        
        PurchaseResponseDTO purchase2 = new PurchaseResponseDTO(
            2L, "Test User", LocalDateTime.now().minusDays(1), PRODUCT_PRICE_75, productos2
        );
        
        purchases.add(purchase1);
        purchases.add(purchase2);
        
        return purchases;
    }
}
