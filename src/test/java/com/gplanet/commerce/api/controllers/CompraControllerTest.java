package com.gplanet.commerce.api.controllers;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gplanet.commerce.api.dtos.compra.CompraDTO;
import com.gplanet.commerce.api.dtos.compra.CompraProductoDTO;
import com.gplanet.commerce.api.dtos.compra.CompraProductoResponseDTO;
import com.gplanet.commerce.api.dtos.compra.CompraResponseDTO;
import com.gplanet.commerce.api.exceptions.ResourceNotFoundException;
import com.gplanet.commerce.api.services.CompraService;
import com.gplanet.commerce.api.services.UsuarioDetallesService;

@WebMvcTest(CompraController.class)
@Import(TestSecurityConfig.class)
class CompraControllerTest {
    
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CompraService compraService;

    @MockitoBean
    private UsuarioDetallesService usuarioDetallesService;
    
    private static final String BASE_URL = "/api/compras";
    private static final String TEST_USER_EMAIL = "test@example.com";
    
    @BeforeEach
    void setUp() {
        // Create UserDetails
        UserDetails userDetails = User.builder()
            .username(TEST_USER_EMAIL)
            .password("password")
            .authorities(AuthorityUtils.createAuthorityList("ROLE_USER"))
            .build();
            
        // Configure UserDetailsService
        when(usuarioDetallesService.loadUserByUsername(TEST_USER_EMAIL))
            .thenReturn(userDetails);

        // Configure ObjectMapper for consistent decimal formatting
        objectMapper.configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Test
    @DisplayName("Should create purchase successfully when valid data is provided")
    void realizarCompra_ValidData_ReturnsCreatedPurchase() throws Exception {
        // Arrange
        CompraDTO requestDTO = createValidCompraDTO();
        CompraResponseDTO responseDTO = createMockCompraResponseDTO();

        when(compraService.realizarCompra(eq(TEST_USER_EMAIL), any(CompraDTO.class)))
            .thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/nueva")
                .with(user(TEST_USER_EMAIL).roles("USER"))  // Use SecurityMockMvcRequestPostProcessors
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(responseDTO.id()))
            .andExpect(jsonPath("$.usuarioNombre").value(responseDTO.usuarioNombre()))
            .andExpect(jsonPath("$.total").value(150.0))
            .andExpect(jsonPath("$.productos", hasSize(2)))
            .andExpect(jsonPath("$.productos[0].productoNombre").value("Producto 1"))
            .andExpect(jsonPath("$.productos[1].productoNombre").value("Producto 2"));

        verify(compraService).realizarCompra(eq(TEST_USER_EMAIL), any(CompraDTO.class));
    }

    @Test
    @DisplayName("Should return 400 when purchase request has no products")
    void realizarCompra_EmptyProductList_ReturnsBadRequest() throws Exception {
        // Arrange
        CompraDTO emptyCompraDTO = new CompraDTO(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/nueva")
                .with(user(TEST_USER_EMAIL).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyCompraDTO)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Validation Error"))
            .andExpect(jsonPath("$.details[0]").value(containsString("Debe incluir al menos un producto")));
    }

    @Test
    @DisplayName("Should return 400 when product quantity is invalid")
    void realizarCompra_InvalidQuantity_ReturnsBadRequest() throws Exception {
        // Arrange
        CompraProductoDTO invalidProductDTO = new CompraProductoDTO(1L, 0); // Invalid quantity
        CompraDTO invalidCompraDTO = new CompraDTO(Collections.singletonList(invalidProductDTO));

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/nueva")
                .with(user(TEST_USER_EMAIL).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidCompraDTO)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Validation Error"))
            .andExpect(jsonPath("$.details[0]").value(containsString("La cantidad debe ser mayor a 0")));
    }

    @Test
    @DisplayName("Should return 404 when product is not found")
    void realizarCompra_ProductNotFound_ReturnsNotFound() throws Exception {
        // Arrange
        CompraDTO validCompraDTO = createValidCompraDTO();
        
        when(compraService.realizarCompra(eq(TEST_USER_EMAIL), any(CompraDTO.class)))
            .thenThrow(new ResourceNotFoundException("Producto no encontrado"));

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/nueva")
                .with(user(TEST_USER_EMAIL).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCompraDTO)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Producto no encontrado"));
    }

    @Test
    @DisplayName("Should return 401 when user is not authenticated")
    void realizarCompra_UnauthenticatedUser_ReturnsUnauthorized() throws Exception {
        // Arrange
        CompraDTO validCompraDTO = createValidCompraDTO();

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/nueva")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCompraDTO)))
            .andExpect(status().isUnauthorized());
            
        verifyNoInteractions(compraService);
    }

    @Test
    @DisplayName("Should return all purchases for authenticated user")
    void listarCompras_AuthenticatedUser_ReturnsUserPurchases() throws Exception {
        // Arrange
        Page<CompraResponseDTO> page = new PageImpl<>(
            createMockComprasList(),
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "fecha")),
            2
        );
        
        when(compraService.listarCompras(
                eq(TEST_USER_EMAIL), 
                eq(0), 
                eq(10), 
                eq("fecha"), 
                eq("DESC")))
            .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/listar")
                .with(user(TEST_USER_EMAIL).roles("USER"))
                .param("page", "0")
                .param("size", "10")
                .param("sort", "fecha")
                .param("direction", "DESC"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(2)))
            .andExpect(jsonPath("$.content[0].id").value(1L))
            .andExpect(jsonPath("$.content[0].productos", hasSize(2)))
            .andExpect(jsonPath("$.content[1].id").value(2L))
            .andExpect(jsonPath("$.content[1].productos", hasSize(1)))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.pageSize").value(10))
            .andExpect(jsonPath("$.pageNumber").value(0));

        verify(compraService).listarCompras(eq(TEST_USER_EMAIL), eq(0), eq(10), eq("fecha"), eq("DESC"));
    }

    @Test
    @DisplayName("Should return empty list when user has no purchases")
    void listarCompras_UserWithNoPurchases_ReturnsEmptyList() throws Exception {
        // Arrange
        Page<CompraResponseDTO> emptyPage = new PageImpl<>(
            Collections.emptyList(),
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "fecha")),
            0
        );
        
        when(compraService.listarCompras(
            eq(TEST_USER_EMAIL),
            eq(0),
            eq(10),
            eq("fecha"),
            eq("DESC")))
        .thenReturn(emptyPage);

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/listar")
                .with(user(TEST_USER_EMAIL).roles("USER"))
                .param("page", "0")
                .param("size", "10")
                .param("sort", "fecha")
                .param("direction", "DESC"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(0)))
            .andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.totalPages").value(0));

        verify(compraService).listarCompras(
            eq(TEST_USER_EMAIL),
            eq(0),
            eq(10),
            eq("fecha"),
            eq("DESC"));
    }

    @Test
    @DisplayName("Should return 401 when user is not authenticated")
    void listarCompras_UnauthenticatedUser_ReturnsUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/listar"))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(compraService);
    }

    // Helper methods remain the same
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
            1L, "Producto 1", new BigDecimal("50.00"), 2, new BigDecimal("100.00")
        );
        
        CompraProductoResponseDTO producto2 = new CompraProductoResponseDTO(
            2L, "Producto 2", new BigDecimal("50.00"), 1, new BigDecimal("50.00")
        );
        
        productos.add(producto1);
        productos.add(producto2);
        
        return new CompraResponseDTO(
            1L, "Test User", LocalDateTime.now(), new BigDecimal("150.00"), productos
        );
    }

    private List<CompraResponseDTO> createMockComprasList() {
        List<CompraResponseDTO> compras = new ArrayList<>();
        
        // First purchase
        CompraResponseDTO compra1 = createMockCompraResponseDTO();
        
        // Second purchase with different data
        List<CompraProductoResponseDTO> productos2 = new ArrayList<>();
        CompraProductoResponseDTO producto = new CompraProductoResponseDTO(
            3L, "Producto 3", new BigDecimal("75.00"), 1, new BigDecimal("75.00")
        );
        productos2.add(producto);
        
        CompraResponseDTO compra2 = new CompraResponseDTO(
            2L, "Test User", LocalDateTime.now().minusDays(1), new BigDecimal("75.00"), productos2
        );
        
        compras.add(compra1);
        compras.add(compra2);
        
        return compras;
    }
}