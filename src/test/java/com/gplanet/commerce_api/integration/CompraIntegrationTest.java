package com.gplanet.commerce_api.integration;

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

import com.gplanet.commerce_api.dtos.compra.CompraDTO;
import com.gplanet.commerce_api.dtos.compra.CompraProductoDTO;
import com.gplanet.commerce_api.dtos.compra.CompraResponseDTO;
import com.gplanet.commerce_api.entities.Compra;
import com.gplanet.commerce_api.entities.Producto;
import com.gplanet.commerce_api.entities.Usuario;

import jakarta.transaction.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CompraIntegrationTest extends BaseIntegrationTest {
    private static final String BASE_URL = "/api/compras";
    private static final String USER_EMAIL = "buyer@example.com";
    private static final String USER_PASSWORD = "buyer123";
    private Long productoId;
    
    @BeforeEach
    void configurarDatosPrueba() {
        // Create admin and buyer users
        crearUsuarioAdmin(); // Create admin user first
        crearUsuario("Buyer", USER_EMAIL, USER_PASSWORD, Usuario.Role.USER);
        
        // Create product
        Producto producto = new Producto();
        producto.setNombre("Producto Test");
        producto.setDescripcion("Producto para pruebas");
        producto.setPrecio(new BigDecimal("10.0"));
        producto.setActivo(true);
        producto.setFechaCreacion(LocalDateTime.now());
        producto = productoRepository.save(producto);
        productoId = producto.getId();
    }
    
    @Test
    void realizarCompra_ProductoDisponible_CompraExitosa() throws Exception {
        // Arrange
        List<CompraProductoDTO> productos = new ArrayList<>();
        CompraProductoDTO productoDTO = new CompraProductoDTO(productoId, 2);
        productos.add(productoDTO);
        CompraDTO compraDTO = new CompraDTO(productos);
        
        // Act & Assert
        MvcResult result = mockMvc.perform(post(BASE_URL + "/nueva")
                .header(HttpHeaders.AUTHORIZATION, obtenerBasicAuthHeader(USER_EMAIL, USER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(compraDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value("20.0"))
                .andExpect(jsonPath("$.productos[0].cantidad").value(2))
                .andExpect(jsonPath("$.productos[0].subtotal").value("20.0"))
                .andReturn();
        
        // Verify purchase in database
        CompraResponseDTO response = objectMapper.readValue(
            result.getResponse().getContentAsString(), 
            CompraResponseDTO.class
        );
        
        // Use findAllByUsuarioIdWithProductos instead
        Usuario usuario = usuarioRepository.findByEmail(USER_EMAIL).orElseThrow();
        Page<Compra> comprasPage = compraRepository.findAllByUsuarioIdWithProductos(
            usuario.getId(),
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "fecha"))
        );
        
        assertTrue(!comprasPage.isEmpty());
        Compra compraGuardada = comprasPage.getContent().get(0);
        assertEquals(response.id(), compraGuardada.getId());
        assertEquals(1, compraGuardada.getProductos().size());
    }
    
    @Test
    void listarCompras_UsuarioAutenticado_RetornaCompras() throws Exception {
        // First create a purchase
        realizarCompra_ProductoDisponible_CompraExitosa();
        
        // Act & Assert - List purchases
        mockMvc.perform(get(BASE_URL + "/listar")
                .header(HttpHeaders.AUTHORIZATION, obtenerBasicAuthHeader(USER_EMAIL, USER_PASSWORD))
                .param("page", "0")
                .param("size", "10")
                .param("sort", "fecha")
                .param("direction", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].total").value("20.0"))
                .andExpect(jsonPath("$.content[0].productos").isArray())
                .andExpect(jsonPath("$.content[0].productos[0].cantidad").value(2))
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber())
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.pageNumber").value(0));
    }
}