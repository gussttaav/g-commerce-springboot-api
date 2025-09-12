package com.gplanet.commerce.api.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.gplanet.commerce.api.entities.Producto;

@DataJpaTest
@ActiveProfiles("test")
class ProductoRepositoryTest {

    private static final String TEST_PRODUCT_NAME = "Test Product";
    private static final String TEST_PRODUCT_DESC = "Test Product Description";
    private static final BigDecimal PRICE_99 = new BigDecimal("99.99");
    

    @Autowired
    private ProductoRepository productoRepository;
    
    @Test
    void saveValidProductSuccess() {
        // Arrange
        Producto producto = new Producto();
        producto.setNombre(TEST_PRODUCT_NAME);
        producto.setDescripcion(TEST_PRODUCT_DESC);
        producto.setPrecio(PRICE_99);
        producto.setActivo(true);
        producto.setFechaCreacion(LocalDateTime.now());
        
        // Act
        Producto savedProducto = productoRepository.save(producto);
        
        // Assert
        assertNotNull(savedProducto.getId());
        assertEquals(TEST_PRODUCT_NAME, savedProducto.getNombre());
        assertEquals(PRICE_99, savedProducto.getPrecio());
    }
    
    @Test
    void saveDuplicateProductNameThrowsException() {
        // Arrange
        Producto producto1 = new Producto();
        producto1.setNombre(TEST_PRODUCT_NAME);
        producto1.setPrecio(PRICE_99);
        producto1.setActivo(true);
        productoRepository.save(producto1);
        
        Producto producto2 = new Producto();
        producto2.setNombre(TEST_PRODUCT_NAME); // Same name
        producto2.setPrecio(new BigDecimal("149.99"));
        producto2.setActivo(true);
        
        // Act & Assert
        assertThrows(DataIntegrityViolationException.class, () -> {
            productoRepository.save(producto2);
            productoRepository.flush(); // Force the persistence
        });
    }
    
    @Test
    void findByActivoActiveProductsReturnsOnlyActive() {
        // Arrange
        Producto activeProduct = new Producto();
        activeProduct.setNombre(TEST_PRODUCT_NAME);
        activeProduct.setPrecio(PRICE_99);
        activeProduct.setActivo(true);
        productoRepository.save(activeProduct);
        
        Producto inactiveProduct = new Producto();
        inactiveProduct.setNombre("Inactive Product");
        inactiveProduct.setPrecio(PRICE_99);
        inactiveProduct.setActivo(false);
        productoRepository.save(inactiveProduct);
        
        // Act
        Page<Producto> activeProducts = productoRepository.findByActivoTrue(PageRequest.of(0, 10));
        
        // Assert
        assertEquals(1, activeProducts.getTotalElements());
        assertTrue(activeProducts.getContent().get(0).isActivo());
        assertEquals(TEST_PRODUCT_NAME, activeProducts.getContent().get(0).getNombre());
    }
}
