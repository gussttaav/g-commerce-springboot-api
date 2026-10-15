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

import com.gplanet.commerce.api.entities.Product;

@DataJpaTest
@ActiveProfiles("test")
class ProductRepositoryTest {

    private static final String TEST_PRODUCT_NAME = "Test Product";
    private static final String TEST_PRODUCT_DESC = "Test Product Description";
    private static final BigDecimal PRICE_99 = new BigDecimal("99.99");
    

    @Autowired
    private ProductRepository productRepository;
    
    @Test
    void saveValidProductSuccess() {
        // Arrange
        Product product = new Product();
        product.setName(TEST_PRODUCT_NAME);
        product.setDescription(TEST_PRODUCT_DESC);
        product.setPrice(PRICE_99);
        product.setActive(true);
        product.setCreationDate(LocalDateTime.now());
        
        // Act
        Product savedProduct = productRepository.save(product);
        
        // Assert
        assertNotNull(savedProduct.getId());
        assertEquals(TEST_PRODUCT_NAME, savedProduct.getName());
        assertEquals(PRICE_99, savedProduct.getPrice());
    }
    
    @Test
    void saveDuplicateProductNameThrowsException() {
        // Arrange
        Product product1 = new Product();
        product1.setName(TEST_PRODUCT_NAME);
        product1.setPrice(PRICE_99);
        product1.setActive(true);
        productRepository.save(product1);
        
        Product product2 = new Product();
        product2.setName(TEST_PRODUCT_NAME); // Same name
        product2.setPrice(new BigDecimal("149.99"));
        product2.setActive(true);
        
        // Act & Assert
        assertThrows(DataIntegrityViolationException.class, () -> {
            productRepository.save(product2);
            productRepository.flush(); // Force the persistence
        });
    }
    
    @Test
    void findByActiveTrueReturnsOnlyActiveProducts() {
        // Arrange
        Product activeProduct = new Product();
        activeProduct.setName(TEST_PRODUCT_NAME);
        activeProduct.setPrice(PRICE_99);
        activeProduct.setActive(true);
        productRepository.save(activeProduct);
        
        Product inactiveProduct = new Product();
        inactiveProduct.setName("Inactive Product");
        inactiveProduct.setPrice(PRICE_99);
        inactiveProduct.setActive(false);
        productRepository.save(inactiveProduct);
        
        // Act
        Page<Product> activeProducts = productRepository.findByActiveTrue(PageRequest.of(0, 10));
        
        // Assert
        assertEquals(1, activeProducts.getTotalElements());
        assertTrue(activeProducts.getContent().get(0).isActive());
        assertEquals(TEST_PRODUCT_NAME, activeProducts.getContent().get(0).getName());
    }
}
