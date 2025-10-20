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

import com.gplanet.commerce.api.entities.Product;
import com.gplanet.commerce.api.entities.Purchase;
import com.gplanet.commerce.api.entities.PurchaseProduct;
import com.gplanet.commerce.api.entities.Usuario;

@DataJpaTest
@ActiveProfiles("test")
class PurchaseRepositoryTest {

    private static final BigDecimal PRICE_10 = new BigDecimal("10.00");
    private static final BigDecimal PRICE_20 = new BigDecimal("20.00");
    private static final Sort SORT_DESC_BY_DATE = Sort.by(Sort.Direction.DESC, "fecha");

    @Autowired
    private PurchaseRepository purchaseRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    private Usuario usuario;
    private Product product;
    
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
        product = new Product();
        product.setName("Test Product");
        product.setPrice(PRICE_10);
        product.setActive(true);
        productRepository.save(product);
    }
    
    @Test
    void saveValidPurchaseSuccess() {
        // Arrange
        Purchase purchase = new Purchase();
        purchase.setUsuario(usuario);
        purchase.setFecha(LocalDateTime.now());
        purchase.setTotal(PRICE_20);
        
        PurchaseProduct purchaseProduct = new PurchaseProduct();
        purchaseProduct.setProduct(product);
        purchaseProduct.setCantidad(2);
        purchaseProduct.setSubtotal(PRICE_20);
        
        purchase.addPurchaseProduct(purchaseProduct);
        
        // Act
        Purchase savedPurchase = purchaseRepository.save(purchase);
        
        // Assert
        assertNotNull(savedPurchase.getId());
        assertEquals(usuario.getId(), savedPurchase.getUsuario().getId());
        assertEquals(1, savedPurchase.getProductos().size());
        assertEquals(product.getId(), savedPurchase.getProductos().get(0).getProduct().getId());
    }
    
    @Test
    void findByUsuarioIdExistingUserReturnsPurchases() {
        // Arrange
        Purchase purchase1 = new Purchase();
        purchase1.setUsuario(usuario);
        purchase1.setFecha(LocalDateTime.now());
        purchase1.setTotal(PRICE_10);
        purchaseRepository.save(purchase1);
        
        Purchase purchase2 = new Purchase();
        purchase2.setUsuario(usuario);
        purchase2.setFecha(LocalDateTime.now());
        purchase2.setTotal(PRICE_20);
        purchaseRepository.save(purchase2);
        
        // Act
        Page<Purchase> purchasesPage = purchaseRepository.findByUsuario(
            usuario,
            PageRequest.of(0, 10, SORT_DESC_BY_DATE)
        );
        
        // Assert
        assertEquals(2, purchasesPage.getTotalElements());
        assertTrue(purchasesPage.getContent().stream()
            .allMatch(purchase -> purchase.getUsuario().getId().equals(usuario.getId())));
    }

    @Test
    void findByUsuarioExistingUserReturnsPurchases() {
        // Arrange
        Purchase purchase1 = new Purchase();
        purchase1.setUsuario(usuario);
        purchase1.setFecha(LocalDateTime.now());
        purchase1.setTotal(PRICE_10);
        purchaseRepository.save(purchase1);
        
        Purchase purchase2 = new Purchase();
        purchase2.setUsuario(usuario);
        purchase2.setFecha(LocalDateTime.now());
        purchase2.setTotal(PRICE_20);
        purchaseRepository.save(purchase2);
        
        // Act
        Page<Purchase> purchasesPage = purchaseRepository.findByUsuario(
            usuario, 
            PageRequest.of(0, 10, SORT_DESC_BY_DATE)
        );
        
        // Assert
        assertEquals(2, purchasesPage.getTotalElements());
        assertTrue(purchasesPage.getContent().stream()
            .allMatch(purchase -> purchase.getUsuario().getId().equals(usuario.getId())));
    }

    @Test
    void findByFechaBetweenDateRangeReturnsCorrectPurchases() {
        // Arrange
        Purchase purchase = new Purchase();
        purchase.setUsuario(usuario);
        purchase.setFecha(LocalDateTime.now());
        purchase.setTotal(PRICE_10);
        purchaseRepository.save(purchase);
        
        // Act
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        Page<Purchase> purchasesPage = purchaseRepository.findByFechaBetween(
            yesterday, 
            tomorrow,
            PageRequest.of(0, 10, SORT_DESC_BY_DATE)
        );
        
        // Assert
        assertEquals(1, purchasesPage.getTotalElements());
        assertTrue(purchasesPage.getContent().get(0).getFecha().isAfter(yesterday));
        assertTrue(purchasesPage.getContent().get(0).getFecha().isBefore(tomorrow));
    }
}
