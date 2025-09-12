package com.gplanet.commerce.api.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import com.gplanet.commerce.api.entities.Compra;
import com.gplanet.commerce.api.entities.CompraProducto;
import com.gplanet.commerce.api.entities.Producto;
import com.gplanet.commerce.api.entities.Usuario;

@DataJpaTest
@ActiveProfiles("test")
class CompraRepositoryTest {

    private static final BigDecimal PRICE_10 = new BigDecimal("10.00");
    private static final BigDecimal PRICE_20 = new BigDecimal("20.00");
    private static final Sort SORT_DESC_BY_DATE = Sort.by(Sort.Direction.DESC, "fecha");

    @Autowired
    private CompraRepository compraRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private ProductoRepository productoRepository;
    
    private Usuario usuario;
    private Producto producto;
    
    @BeforeEach
    void setup() {
        // Setup test user
        usuario = new Usuario();
        usuario.setNombre("Test User");
        usuario.setEmail("test@example.com");
        usuario.setPassword("password");
        usuario.setRol(Usuario.Role.USER);
        usuarioRepository.save(usuario);
        
        // Setup test product
        producto = new Producto();
        producto.setNombre("Test Product");
        producto.setPrecio(PRICE_10);
        producto.setActivo(true);
        productoRepository.save(producto);
    }
    
    @Test
    void saveValidCompraSuccess() {
        // Arrange
        Compra compra = new Compra();
        compra.setUsuario(usuario);
        compra.setFecha(LocalDateTime.now());
        compra.setTotal(PRICE_20);
        
        CompraProducto compraProducto = new CompraProducto();
        compraProducto.setProducto(producto);
        compraProducto.setCantidad(2);
        compraProducto.setSubtotal(PRICE_20);
        
        compra.addCompraProducto(compraProducto);
        
        // Act
        Compra savedCompra = compraRepository.save(compra);
        
        // Assert
        assertNotNull(savedCompra.getId());
        assertEquals(usuario.getId(), savedCompra.getUsuario().getId());
        assertEquals(1, savedCompra.getProductos().size());
        assertEquals(producto.getId(), savedCompra.getProductos().get(0).getProducto().getId());
    }
    
    @Test
    void findByUsuarioIdExistingUserReturnsCompras() {
        // Arrange
        Compra compra1 = new Compra();
        compra1.setUsuario(usuario);
        compra1.setFecha(LocalDateTime.now());
        compra1.setTotal(PRICE_10);
        compraRepository.save(compra1);
        
        Compra compra2 = new Compra();
        compra2.setUsuario(usuario);
        compra2.setFecha(LocalDateTime.now());
        compra2.setTotal(PRICE_20);
        compraRepository.save(compra2);
        
        // Act
        Page<Compra> comprasPage = compraRepository.findByUsuario(
            usuario,
            PageRequest.of(0, 10, SORT_DESC_BY_DATE)
        );
        
        // Assert
        assertEquals(2, comprasPage.getTotalElements());
        assertTrue(comprasPage.getContent().stream()
            .allMatch(compra -> compra.getUsuario().getId().equals(usuario.getId())));
    }

    @Test
    void findByUsuarioExistingUserReturnsCompras() {
        // Arrange
        Compra compra1 = new Compra();
        compra1.setUsuario(usuario);
        compra1.setFecha(LocalDateTime.now());
        compra1.setTotal(PRICE_10);
        compraRepository.save(compra1);
        
        Compra compra2 = new Compra();
        compra2.setUsuario(usuario);
        compra2.setFecha(LocalDateTime.now());
        compra2.setTotal(PRICE_20);
        compraRepository.save(compra2);
        
        // Act
        Page<Compra> comprasPage = compraRepository.findByUsuario(
            usuario, 
            PageRequest.of(0, 10, SORT_DESC_BY_DATE)
        );
        
        // Assert
        assertEquals(2, comprasPage.getTotalElements());
        assertTrue(comprasPage.getContent().stream()
            .allMatch(compra -> compra.getUsuario().getId().equals(usuario.getId())));
    }

    @Test
    void findByFechaBetweenDateRangeReturnsCorrectCompras() {
        // Arrange
        Compra compra = new Compra();
        compra.setUsuario(usuario);
        compra.setFecha(LocalDateTime.now());
        compra.setTotal(PRICE_10);
        compraRepository.save(compra);
        
        // Act
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        Page<Compra> comprasPage = compraRepository.findByFechaBetween(
            yesterday, 
            tomorrow,
            PageRequest.of(0, 10, SORT_DESC_BY_DATE)
        );
        
        // Assert
        assertEquals(1, comprasPage.getTotalElements());
        assertTrue(comprasPage.getContent().get(0).getFecha().isAfter(yesterday));
        assertTrue(comprasPage.getContent().get(0).getFecha().isBefore(tomorrow));
    }
}
