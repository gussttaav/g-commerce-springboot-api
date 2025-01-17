package com.mitienda.gestion_tienda.controllers;

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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mitienda.gestion_tienda.configs.SecurityConfig;
import com.mitienda.gestion_tienda.dtos.compra.CompraDTO;
import com.mitienda.gestion_tienda.dtos.compra.CompraProductoDTO;
import com.mitienda.gestion_tienda.dtos.compra.CompraProductoResponseDTO;
import com.mitienda.gestion_tienda.dtos.compra.CompraResponseDTO;
import com.mitienda.gestion_tienda.exceptions.ResourceNotFoundException;
import com.mitienda.gestion_tienda.services.CompraService;
import com.mitienda.gestion_tienda.services.UsuarioDetallesService;

@WebMvcTest(CompraController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
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
            .andExpect(jsonPath("$.id").value(responseDTO.getId()))
            .andExpect(jsonPath("$.usuarioNombre").value(responseDTO.getUsuarioNombre()))
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
        CompraDTO emptyCompraDTO = new CompraDTO();
        emptyCompraDTO.setProductos(Collections.emptyList());

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
        CompraDTO invalidCompraDTO = new CompraDTO();
        CompraProductoDTO invalidProductDTO = new CompraProductoDTO();
        invalidProductDTO.setProductoId(1L);
        invalidProductDTO.setCantidad(0); // Invalid quantity
        invalidCompraDTO.setProductos(Collections.singletonList(invalidProductDTO));

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
        List<CompraResponseDTO> mockCompras = createMockComprasList();
        
        when(compraService.listarCompras(TEST_USER_EMAIL))
            .thenReturn(mockCompras);

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/listar")
                .with(user(TEST_USER_EMAIL).roles("USER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].id").value(1L))
            .andExpect(jsonPath("$[0].productos", hasSize(2)))
            .andExpect(jsonPath("$[1].id").value(2L))
            .andExpect(jsonPath("$[1].productos", hasSize(1)));

        verify(compraService).listarCompras(TEST_USER_EMAIL);
    }

    @Test
    @DisplayName("Should return empty list when user has no purchases")
    void listarCompras_UserWithNoPurchases_ReturnsEmptyList() throws Exception {
        // Arrange
        when(compraService.listarCompras(TEST_USER_EMAIL))
            .thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/listar")
                .with(user(TEST_USER_EMAIL).roles("USER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));

        verify(compraService).listarCompras(TEST_USER_EMAIL);
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
        CompraDTO compraDTO = new CompraDTO();
        List<CompraProductoDTO> productos = new ArrayList<>();
        
        CompraProductoDTO producto1 = new CompraProductoDTO();
        producto1.setProductoId(1L);
        producto1.setCantidad(2);
        
        CompraProductoDTO producto2 = new CompraProductoDTO();
        producto2.setProductoId(2L);
        producto2.setCantidad(1);
        
        productos.add(producto1);
        productos.add(producto2);
        compraDTO.setProductos(productos);
        
        return compraDTO;
    }

    private CompraResponseDTO createMockCompraResponseDTO() {
        CompraResponseDTO responseDTO = new CompraResponseDTO();
        responseDTO.setId(1L);
        responseDTO.setUsuarioNombre("Test User");
        responseDTO.setFecha(LocalDateTime.now());
        responseDTO.setTotal(new BigDecimal("150.00"));
        
        List<CompraProductoResponseDTO> productos = new ArrayList<>();
        
        CompraProductoResponseDTO producto1 = new CompraProductoResponseDTO();
        producto1.setId(1L);
        producto1.setProductoNombre("Producto 1");
        producto1.setPrecioUnitario(new BigDecimal("50.00"));
        producto1.setCantidad(2);
        producto1.setSubtotal(new BigDecimal("100.00"));
        
        CompraProductoResponseDTO producto2 = new CompraProductoResponseDTO();
        producto2.setId(2L);
        producto2.setProductoNombre("Producto 2");
        producto2.setPrecioUnitario(new BigDecimal("50.00"));
        producto2.setCantidad(1);
        producto2.setSubtotal(new BigDecimal("50.00"));
        
        productos.add(producto1);
        productos.add(producto2);
        responseDTO.setProductos(productos);
        
        return responseDTO;
    }

    private List<CompraResponseDTO> createMockComprasList() {
        List<CompraResponseDTO> compras = new ArrayList<>();
        
        // First purchase
        CompraResponseDTO compra1 = createMockCompraResponseDTO();
        
        // Second purchase with different data
        CompraResponseDTO compra2 = new CompraResponseDTO();
        compra2.setId(2L);
        compra2.setUsuarioNombre("Test User");
        compra2.setFecha(LocalDateTime.now().minusDays(1));
        compra2.setTotal(new BigDecimal("75.00"));
        
        List<CompraProductoResponseDTO> productos2 = new ArrayList<>();
        CompraProductoResponseDTO producto = new CompraProductoResponseDTO();
        producto.setId(3L);
        producto.setProductoNombre("Producto 3");
        producto.setPrecioUnitario(new BigDecimal("75.00"));
        producto.setCantidad(1);
        producto.setSubtotal(new BigDecimal("75.00"));
        productos2.add(producto);
        compra2.setProductos(productos2);
        
        compras.add(compra1);
        compras.add(compra2);
        
        return compras;
    }
}