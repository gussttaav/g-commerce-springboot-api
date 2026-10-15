package com.gplanet.commerce.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.gplanet.commerce.api.dtos.purchase.PurchaseDTO;
import com.gplanet.commerce.api.dtos.purchase.PurchaseMapper;
import com.gplanet.commerce.api.dtos.purchase.PurchaseProductDTO;
import com.gplanet.commerce.api.dtos.purchase.PurchaseResponseDTO;
import com.gplanet.commerce.api.entities.Product;
import com.gplanet.commerce.api.entities.Purchase;
import com.gplanet.commerce.api.entities.User;
import com.gplanet.commerce.api.exceptions.ResourceNotFoundException;
import com.gplanet.commerce.api.repositories.ProductRepository;
import com.gplanet.commerce.api.repositories.PurchaseRepository;
import com.gplanet.commerce.api.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {
    private static final Long NON_EXISTENT_USER_ID = 999L;

    @Mock
    private PurchaseMapper purchaseMapper;
    
    @Mock
    private PurchaseRepository purchaseRepository;
    
    @Mock
    private ProductRepository productRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private PurchaseService purchaseService;
    
    private User regularUser;
    private User adminUser;
    private Product product1;
    private Product product2;
    private Purchase purchase;
    private PurchaseDTO purchaseDTO;
    private PurchaseResponseDTO purchaseResponseDTO;
    
    @BeforeEach
    void setUp() {
        // Set up the users
        regularUser = new User();
        regularUser.setId(1L);
        regularUser.setEmail("user@example.com");
        regularUser.setName("Regular User");
        regularUser.setRole(User.Role.USER);
        
        adminUser = new User();
        adminUser.setId(2L);
        adminUser.setEmail("admin@example.com");
        adminUser.setName("Admin User");
        adminUser.setRole(User.Role.ADMIN);
        
        // Set up the products
        product1 = new Product();
        product1.setId(1L);
        product1.setName("Product 1");
        product1.setPrice(new BigDecimal("100.00"));
        
        product2 = new Product();
        product2.setId(2L);
        product2.setName("Product 2");
        product2.setPrice(new BigDecimal("200.00"));
        
        // Setup PurchaseDTO
        PurchaseProductDTO purchaseProductDto1 = new PurchaseProductDTO(1L, 2);
        PurchaseProductDTO purchaseProductDto2 = new PurchaseProductDTO(2L, 1);
        purchaseDTO = new PurchaseDTO(Arrays.asList(purchaseProductDto1, purchaseProductDto2));
        
        // Setup Purchase
        purchase = new Purchase();
        purchase.setId(1L);
        purchase.setUser(regularUser);
        purchase.setDate(LocalDateTime.now());
        purchase.setTotal(new BigDecimal("400.00"));
        
        // Setup PurchaseResponseDTO
        purchaseResponseDTO = new PurchaseResponseDTO(
            1L,
            regularUser.getName(),
            purchase.getDate(),
            purchase.getTotal(),
            null // products, not used by the assertions
        );
    }

    private Page<PurchaseResponseDTO> listOnePagePurchasesSortDescByDate(Long userId) {
        return purchaseService.listPurchases(userId, 0, 10, "date", "DESC");
    }
    
    @Test
    void listPurchasesUserRoleReturnsUserPurchases() {
        // Arrange
        Page<Purchase> purchasesPage = new PageImpl<>(Collections.singletonList(purchase));
        when(userRepository.findById(regularUser.getId()))
            .thenReturn(Optional.of(regularUser));
        when(purchaseRepository.findByUser(eq(regularUser), any(Pageable.class)))
            .thenReturn(purchasesPage);
        when(purchaseMapper.toPurchaseResponseDTO(purchase))
            .thenReturn(purchaseResponseDTO);
            
        // Act
        Page<PurchaseResponseDTO> result = listOnePagePurchasesSortDescByDate(regularUser.getId());
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(purchaseResponseDTO, result.getContent().get(0));
        verify(purchaseRepository).findByUser(eq(regularUser), any(Pageable.class));
        verify(purchaseRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void listPurchasesAdminRoleReturnsAllPurchases() {
        // Arrange
        Page<Purchase> purchasesPage = new PageImpl<>(Collections.singletonList(purchase));
        when(userRepository.findById(adminUser.getId()))
            .thenReturn(Optional.of(adminUser));
        when(purchaseRepository.findAll(any(Pageable.class)))
            .thenReturn(purchasesPage);
        when(purchaseMapper.toPurchaseResponseDTO(purchase))
            .thenReturn(purchaseResponseDTO);
            
        // Act
        Page<PurchaseResponseDTO> result = listOnePagePurchasesSortDescByDate(adminUser.getId());
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(purchaseResponseDTO, result.getContent().get(0));
        verify(purchaseRepository).findAll(any(Pageable.class));
        verify(purchaseRepository, never()).findByUser(any(), any(Pageable.class));
    }

    @Test
    void listPurchasesUserNotFoundThrowsException() {
        // Arrange
        when(userRepository.findById(anyLong()))
            .thenReturn(Optional.empty());
            
        // Act & Assert
        assertThrows(UsernameNotFoundException.class,
            () -> listOnePagePurchasesSortDescByDate(NON_EXISTENT_USER_ID));
    }
    
    @Test
    void createPurchaseSuccess() {
        // Arrange
        when(userRepository.findById(regularUser.getId()))
            .thenReturn(Optional.of(regularUser));
        when(productRepository.findByIdAndActiveTrue(1L))
            .thenReturn(Optional.of(product1));
        when(productRepository.findByIdAndActiveTrue(2L))
            .thenReturn(Optional.of(product2));
        when(purchaseRepository.save(any(Purchase.class)))
            .thenReturn(purchase);
        when(purchaseMapper.toPurchaseResponseDTO(any(Purchase.class)))
            .thenReturn(purchaseResponseDTO);
            
        // Act
        PurchaseResponseDTO result = purchaseService.createPurchase(regularUser.getId(), purchaseDTO);
        
        // Assert
        assertNotNull(result);
        assertEquals(purchaseResponseDTO.id(), result.id());
        assertEquals(purchaseResponseDTO.total(), result.total());
        verify(purchaseRepository).save(any(Purchase.class));
    }
    
    @Test
    void createPurchaseUserNotFoundThrowsException() {
        // Arrange
        when(userRepository.findById(anyLong()))
            .thenReturn(Optional.empty());
            
        // Act & Assert
        assertThrows(RuntimeException.class,
            () -> purchaseService.createPurchase(NON_EXISTENT_USER_ID, purchaseDTO));
    }
    
    @Test
    void createPurchaseProductNotFoundThrowsException() {
        // Arrange
        when(userRepository.findById(regularUser.getId()))
            .thenReturn(Optional.of(regularUser));
        when(productRepository.findByIdAndActiveTrue(anyLong()))
            .thenReturn(Optional.empty());
            
        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
            () -> purchaseService.createPurchase(regularUser.getId(), purchaseDTO));
    }
    
    @Test
    void createPurchaseCalculatesCorrectTotal() {
        // Arrange
        when(userRepository.findById(regularUser.getId()))
            .thenReturn(Optional.of(regularUser));
        when(productRepository.findByIdAndActiveTrue(1L))
            .thenReturn(Optional.of(product1));
        when(productRepository.findByIdAndActiveTrue(2L))
            .thenReturn(Optional.of(product2));
        when(purchaseRepository.save(any(Purchase.class)))
            .thenAnswer(invocation -> {
                Purchase savedPurchase = invocation.getArgument(0);
                savedPurchase.setId(1L);
                return savedPurchase;
            });
            
        // Act
        purchaseService.createPurchase(regularUser.getId(), purchaseDTO);
        
        // Assert
        ArgumentCaptor<Purchase> purchaseCaptor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchaseRepository).save(purchaseCaptor.capture());
        
        Purchase savedPurchase = purchaseCaptor.getValue();
        BigDecimal expectedTotal = new BigDecimal("400.00"); // (100 * 2) + (200 * 1)
        assertEquals(expectedTotal, savedPurchase.getTotal());
    }
}
