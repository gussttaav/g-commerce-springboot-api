package com.gplanet.commerce.api.controllers;

import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_COMPRAS_LISTAR;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_COMPRAS_NUEVA;
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
import com.gplanet.commerce.api.dtos.compra.CompraDTO;
import com.gplanet.commerce.api.dtos.compra.CompraProductoDTO;
import com.gplanet.commerce.api.dtos.compra.CompraProductoResponseDTO;
import com.gplanet.commerce.api.dtos.compra.CompraResponseDTO;
import com.gplanet.commerce.api.services.CompraService;
import com.gplanet.commerce.api.services.UsuarioDetallesService;

@WebMvcTest(CompraController.class)
@Import(TestSecurityConfig.class)
class CompraControllerTest {

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
    private CompraService compraService;

    @MockitoBean
    private UsuarioDetallesService usuarioDetallesService;
    
    @BeforeEach
    void setUp() {
        // Configure ObjectMapper for consistent decimal formatting
        objectMapper.configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Test
    @WithMockUser(username = TEST_USER_EMAIL, roles = "USER")
    @DisplayName("Should create purchase successfully when valid data is provided")
    void realizarCompraValidDataReturnsCreatedPurchase() throws Exception {
        // Arrange
        CompraDTO requestDTO = createValidCompraDTO();
        CompraResponseDTO responseDTO = createMockCompraResponseDTO();
        float expectedTotal = responseDTO.total().floatValue();

        when(compraService.realizarCompra(eq(TEST_USER_EMAIL), any(CompraDTO.class)))
            .thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(post(ENDPOINT_COMPRAS_NUEVA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(responseDTO.id()))
            .andExpect(jsonPath("$.usuarioNombre").value(responseDTO.usuarioNombre()))
            .andExpect(jsonPath("$.total").value(expectedTotal))
            .andExpect(jsonPath("$.productos", Matchers.hasSize(2)));

        verify(compraService).realizarCompra(eq(TEST_USER_EMAIL), any(CompraDTO.class));
    }

    @Test
    @DisplayName("Should return 401 when user is not authenticated")
    void realizarCompraUnauthenticatedUserReturnsUnauthorized() throws Exception {
        // Arrange
        CompraDTO validCompraDTO = createValidCompraDTO();

        // Act & Assert
        mockMvc.perform(post(ENDPOINT_COMPRAS_NUEVA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCompraDTO)))
            .andExpect(status().isUnauthorized());
            
        verifyNoInteractions(compraService);
    }

    @Test
    @WithMockUser(username = TEST_USER_EMAIL, roles = "USER")
    @DisplayName("Should return all purchases for authenticated user")
    void listarComprasAuthenticatedUserReturnsUserPurchases() throws Exception {
        // Arrange
        Page<CompraResponseDTO> page = new PageImpl<>(
            createMockComprasList(),
            PAGE_REQUEST_DESC_BY_FECHA,
            2
        );
        
        when(compraService.listarCompras(
                eq(TEST_USER_EMAIL), 
                eq(0), 
                eq(10), 
                eq(SORT_FIELD_FECHA), 
                eq(SORT_DIRECTION_DESC)))
            .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get(ENDPOINT_COMPRAS_LISTAR)
                .param("page", "0")
                .param("size", "10")
                .param("sort", SORT_FIELD_FECHA)
                .param("direction", SORT_DIRECTION_DESC))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", Matchers.hasSize(2)))
            .andExpect(jsonPath("$.totalElements").value(2));

        verify(compraService).listarCompras(
            eq(TEST_USER_EMAIL), 
            eq(0), 
            eq(10), 
            eq(SORT_FIELD_FECHA), 
            eq(SORT_DIRECTION_DESC));
    }

    @Test
    @WithMockUser(username = TEST_ADMIN_EMAIL, roles = "ADMIN")
    @DisplayName("Should return all purchases for admin user")
    void listarComprasAdminUserReturnsAllPurchases() throws Exception {
        // Arrange
        Page<CompraResponseDTO> page = new PageImpl<>(
            createMockComprasList(),
            PAGE_REQUEST_DESC_BY_FECHA,
            2
        );
        
        when(compraService.listarCompras(
                eq(TEST_ADMIN_EMAIL), 
                eq(0), 
                eq(10), 
                eq(SORT_FIELD_FECHA), 
                eq(SORT_DIRECTION_DESC)))
            .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get(ENDPOINT_COMPRAS_LISTAR)
                .param("page", "0")
                .param("size", "10")
                .param("sort", SORT_FIELD_FECHA)
                .param("direction", SORT_DIRECTION_DESC))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", Matchers.hasSize(2)));

        verify(compraService).listarCompras(
            eq(TEST_ADMIN_EMAIL), 
            eq(0), 
            eq(10), 
            eq(SORT_FIELD_FECHA), 
            eq(SORT_DIRECTION_DESC));
    }

    // Helper methods
    private CompraDTO createValidCompraDTO() {
        List<CompraProductoDTO> productos = new ArrayList<>();
        
        CompraProductoDTO producto1 = new CompraProductoDTO(1L, 2);
        CompraProductoDTO producto2 = new CompraProductoDTO(2L, 1);
        
        productos.add(producto1);
        productos.add(producto2);
        
        return new CompraDTO(productos);
    }

    private CompraResponseDTO createMockCompraResponseDTO() {
        List<CompraProductoResponseDTO> productos = new ArrayList<>();
        
        CompraProductoResponseDTO producto1 = new CompraProductoResponseDTO(
            1L, "Producto 1", PRODUCT_PRICE_50, 2, COMPRA_TOTAL_100
        );
        
        CompraProductoResponseDTO producto2 = new CompraProductoResponseDTO(
            2L, "Producto 2", PRODUCT_PRICE_50, 1, PRODUCT_PRICE_50
        );
        
        productos.add(producto1);
        productos.add(producto2);
        
        return new CompraResponseDTO(
            1L, "Test User", LocalDateTime.now(), COMPRA_TOTAL_150, productos
        );
    }

    private List<CompraResponseDTO> createMockComprasList() {
        List<CompraResponseDTO> compras = new ArrayList<>();
        
        // First purchase
        CompraResponseDTO compra1 = createMockCompraResponseDTO();
        
        // Second purchase with different data
        List<CompraProductoResponseDTO> productos2 = new ArrayList<>();
        CompraProductoResponseDTO producto = new CompraProductoResponseDTO(
            3L, "Producto 3", PRODUCT_PRICE_75, 1, PRODUCT_PRICE_75
        );
        productos2.add(producto);
        
        CompraResponseDTO compra2 = new CompraResponseDTO(
            2L, "Test User", LocalDateTime.now().minusDays(1), PRODUCT_PRICE_75, productos2
        );
        
        compras.add(compra1);
        compras.add(compra2);
        
        return compras;
    }
}
