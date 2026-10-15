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
import com.gplanet.commerce.api.entities.User;

@DataJpaTest
@ActiveProfiles("test")
class PurchaseRepositoryTest {

    private static final BigDecimal PRICE_10 = new BigDecimal("10.00");
    private static final BigDecimal PRICE_20 = new BigDecimal("20.00");
    private static final Sort SORT_DESC_BY_DATE = Sort.by(Sort.Direction.DESC, "date");

    @Autowired
    private PurchaseRepository purchaseRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    private User user;
    private Product product;
    
    @BeforeEach
    void setup() {
        // Setup test user
        user = new User();
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setPassword("password");
        user.setRole(User.Role.USER);
        userRepository.save(user);
        
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
        purchase.setUser(user);
        purchase.setDate(LocalDateTime.now());
        purchase.setTotal(PRICE_20);
        
        PurchaseProduct purchaseProduct = new PurchaseProduct();
        purchaseProduct.setProduct(product);
        purchaseProduct.setQuantity(2);
        purchaseProduct.setSubtotal(PRICE_20);
        
        purchase.addPurchaseProduct(purchaseProduct);
        
        // Act
        Purchase savedPurchase = purchaseRepository.save(purchase);
        
        // Assert
        assertNotNull(savedPurchase.getId());
        assertEquals(user.getId(), savedPurchase.getUser().getId());
        assertEquals(1, savedPurchase.getProducts().size());
        assertEquals(product.getId(), savedPurchase.getProducts().get(0).getProduct().getId());
    }
    
    @Test
    void findByUserIdExistingUserReturnsPurchases() {
        // Arrange
        Purchase purchase1 = new Purchase();
        purchase1.setUser(user);
        purchase1.setDate(LocalDateTime.now());
        purchase1.setTotal(PRICE_10);
        purchaseRepository.save(purchase1);
        
        Purchase purchase2 = new Purchase();
        purchase2.setUser(user);
        purchase2.setDate(LocalDateTime.now());
        purchase2.setTotal(PRICE_20);
        purchaseRepository.save(purchase2);
        
        // Act
        Page<Purchase> purchasesPage = purchaseRepository.findByUser(
            user,
            PageRequest.of(0, 10, SORT_DESC_BY_DATE)
        );
        
        // Assert
        assertEquals(2, purchasesPage.getTotalElements());
        assertTrue(purchasesPage.getContent().stream()
            .allMatch(purchase -> purchase.getUser().getId().equals(user.getId())));
    }

    @Test
    void findByUserExistingUserReturnsPurchases() {
        // Arrange
        Purchase purchase1 = new Purchase();
        purchase1.setUser(user);
        purchase1.setDate(LocalDateTime.now());
        purchase1.setTotal(PRICE_10);
        purchaseRepository.save(purchase1);
        
        Purchase purchase2 = new Purchase();
        purchase2.setUser(user);
        purchase2.setDate(LocalDateTime.now());
        purchase2.setTotal(PRICE_20);
        purchaseRepository.save(purchase2);
        
        // Act
        Page<Purchase> purchasesPage = purchaseRepository.findByUser(
            user, 
            PageRequest.of(0, 10, SORT_DESC_BY_DATE)
        );
        
        // Assert
        assertEquals(2, purchasesPage.getTotalElements());
        assertTrue(purchasesPage.getContent().stream()
            .allMatch(purchase -> purchase.getUser().getId().equals(user.getId())));
    }

    @Test
    void findByDateBetweenDateRangeReturnsCorrectPurchases() {
        // Arrange
        Purchase purchase = new Purchase();
        purchase.setUser(user);
        purchase.setDate(LocalDateTime.now());
        purchase.setTotal(PRICE_10);
        purchaseRepository.save(purchase);
        
        // Act
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        Page<Purchase> purchasesPage = purchaseRepository.findByDateBetween(
            yesterday, 
            tomorrow,
            PageRequest.of(0, 10, SORT_DESC_BY_DATE)
        );
        
        // Assert
        assertEquals(1, purchasesPage.getTotalElements());
        assertTrue(purchasesPage.getContent().get(0).getDate().isAfter(yesterday));
        assertTrue(purchasesPage.getContent().get(0).getDate().isBefore(tomorrow));
    }
}
