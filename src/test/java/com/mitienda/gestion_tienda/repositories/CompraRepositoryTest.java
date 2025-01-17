package com.mitienda.gestion_tienda.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.mitienda.gestion_tienda.entities.Compra;
import com.mitienda.gestion_tienda.entities.CompraProducto;
import com.mitienda.gestion_tienda.entities.Producto;
import com.mitienda.gestion_tienda.entities.Usuario;

@DataJpaTest
@ActiveProfiles("test")
class CompraRepositoryTest {
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
        producto.setPrecio(new BigDecimal("10.00"));
        producto.setActivo(true);
        productoRepository.save(producto);
    }
    
    @Test
    void save_ValidCompra_Success() {
        // Arrange
        Compra compra = new Compra();
        compra.setUsuario(usuario);
        compra.setFecha(LocalDateTime.now());
        compra.setTotal(new BigDecimal("20.00"));
        
        CompraProducto compraProducto = new CompraProducto();
        compraProducto.setProducto(producto);
        compraProducto.setCantidad(2);
        compraProducto.setSubtotal(new BigDecimal("20.00"));
        
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
    void findByUsuarioId_ExistingUser_ReturnsCompras() {
        // Arrange
        Compra compra1 = new Compra();
        compra1.setUsuario(usuario);
        compra1.setFecha(LocalDateTime.now());
        compra1.setTotal(new BigDecimal("10.00"));
        compraRepository.save(compra1);
        
        Compra compra2 = new Compra();
        compra2.setUsuario(usuario);
        compra2.setFecha(LocalDateTime.now());
        compra2.setTotal(new BigDecimal("20.00"));
        compraRepository.save(compra2);
        
        // Act
        List<Compra> compras = compraRepository.findByUsuario(usuario);
        
        // Assert
        assertEquals(2, compras.size());
        assertTrue(compras.stream()
            .allMatch(compra -> compra.getUsuario().getId().equals(usuario.getId())));
    }
    
    @Test
    void findByFechaBetween_DateRange_ReturnsCorrectCompras() {
        // Arrange
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        
        Compra compra = new Compra();
        compra.setUsuario(usuario);
        compra.setFecha(LocalDateTime.now());
        compra.setTotal(new BigDecimal("10.00"));
        compraRepository.save(compra);
        
        // Act
        List<Compra> compras = compraRepository.findByFechaBetween(yesterday, tomorrow);
        
        // Assert
        assertEquals(1, compras.size());
        assertTrue(compras.get(0).getFecha().isAfter(yesterday));
        assertTrue(compras.get(0).getFecha().isBefore(tomorrow));
    }
}