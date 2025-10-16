package com.gplanet.commerce.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
import com.gplanet.commerce.api.entities.Producto;
import com.gplanet.commerce.api.entities.Purchase;
import com.gplanet.commerce.api.entities.Usuario;
import com.gplanet.commerce.api.exceptions.ResourceNotFoundException;
import com.gplanet.commerce.api.repositories.ProductoRepository;
import com.gplanet.commerce.api.repositories.PurchaseRepository;
import com.gplanet.commerce.api.repositories.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {
    @Mock
    private PurchaseMapper purchaseMapper;
    
    @Mock
    private PurchaseRepository purchaseRepository;
    
    @Mock
    private ProductoRepository productoRepository;
    
    @Mock
    private UsuarioRepository usuarioRepository;
    
    @InjectMocks
    private PurchaseService purchaseService;
    
    private Usuario usuarioNormal;
    private Usuario usuarioAdmin;
    private Producto producto1;
    private Producto producto2;
    private Purchase purchase;
    private PurchaseDTO purchaseDTO;
    private PurchaseResponseDTO purchaseResponseDTO;
    
    @BeforeEach
    void setUp() {
        // Setup Usuario
        usuarioNormal = new Usuario();
        usuarioNormal.setId(1L);
        usuarioNormal.setEmail("user@example.com");
        usuarioNormal.setNombre("Usuario Normal");
        usuarioNormal.setRol(Usuario.Role.USER);
        
        usuarioAdmin = new Usuario();
        usuarioAdmin.setId(2L);
        usuarioAdmin.setEmail("admin@example.com");
        usuarioAdmin.setNombre("Usuario Admin");
        usuarioAdmin.setRol(Usuario.Role.ADMIN);
        
        // Setup Productos
        producto1 = new Producto();
        producto1.setId(1L);
        producto1.setNombre("Producto 1");
        producto1.setPrecio(new BigDecimal("100.00"));
        
        producto2 = new Producto();
        producto2.setId(2L);
        producto2.setNombre("Producto 2");
        producto2.setPrecio(new BigDecimal("200.00"));
        
        // Setup PurchaseDTO
        PurchaseProductDTO purchaseProductDto1 = new PurchaseProductDTO(1L, 2);
        PurchaseProductDTO purchaseProductDto2 = new PurchaseProductDTO(2L, 1);
        purchaseDTO = new PurchaseDTO(Arrays.asList(purchaseProductDto1, purchaseProductDto2));
        
        // Setup Purchase
        purchase = new Purchase();
        purchase.setId(1L);
        purchase.setUsuario(usuarioNormal);
        purchase.setFecha(LocalDateTime.now());
        purchase.setTotal(new BigDecimal("400.00"));
        
        // Setup PurchaseResponseDTO
        purchaseResponseDTO = new PurchaseResponseDTO(
            1L,
            usuarioNormal.getNombre(),
            purchase.getFecha(),
            purchase.getTotal(),
            null // productos, no se usan en los asserts
        );
    }

    private Page<PurchaseResponseDTO> listOnePagePurchasesSortDescByDate(String username) {
        return purchaseService.listPurchases(username, 0, 10, "fecha", "DESC");
    }
    
    @Test
    void listPurchasesUserRoleReturnsUserPurchases() {
        // Arrange
        Page<Purchase> purchasesPage = new PageImpl<>(Collections.singletonList(purchase));
        when(usuarioRepository.findByEmail(usuarioNormal.getEmail()))
            .thenReturn(Optional.of(usuarioNormal));
        when(purchaseRepository.findByUsuario(eq(usuarioNormal), any(Pageable.class)))
            .thenReturn(purchasesPage);
        when(purchaseMapper.toPurchaseResponseDTO(purchase))
            .thenReturn(purchaseResponseDTO);
            
        // Act
        Page<PurchaseResponseDTO> result = listOnePagePurchasesSortDescByDate(usuarioNormal.getEmail());
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(purchaseResponseDTO, result.getContent().get(0));
        verify(purchaseRepository).findByUsuario(eq(usuarioNormal), any(Pageable.class));
        verify(purchaseRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void listPurchasesAdminRoleReturnsAllPurchases() {
        // Arrange
        Page<Purchase> purchasesPage = new PageImpl<>(Collections.singletonList(purchase));
        when(usuarioRepository.findByEmail(usuarioAdmin.getEmail()))
            .thenReturn(Optional.of(usuarioAdmin));
        when(purchaseRepository.findAll(any(Pageable.class)))
            .thenReturn(purchasesPage);
        when(purchaseMapper.toPurchaseResponseDTO(purchase))
            .thenReturn(purchaseResponseDTO);
            
        // Act
        Page<PurchaseResponseDTO> result = listOnePagePurchasesSortDescByDate(usuarioAdmin.getEmail());
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(purchaseResponseDTO, result.getContent().get(0));
        verify(purchaseRepository).findAll(any(Pageable.class));
        verify(purchaseRepository, never()).findByUsuario(any(), any(Pageable.class));
    }

    @Test
    void listPurchasesUserNotFoundThrowsException() {
        // Arrange
        when(usuarioRepository.findByEmail(anyString()))
            .thenReturn(Optional.empty());
            
        // Act & Assert
        assertThrows(UsernameNotFoundException.class,
            () -> listOnePagePurchasesSortDescByDate("nonexistent@example.com"));
    }
    
    @Test
    void createPurchaseSuccess() {
        // Arrange
        when(usuarioRepository.findByEmail(usuarioNormal.getEmail()))
            .thenReturn(Optional.of(usuarioNormal));
        when(productoRepository.findById(1L))
            .thenReturn(Optional.of(producto1));
        when(productoRepository.findById(2L))
            .thenReturn(Optional.of(producto2));
        when(purchaseRepository.save(any(Purchase.class)))
            .thenReturn(purchase);
        when(purchaseMapper.toPurchaseResponseDTO(any(Purchase.class)))
            .thenReturn(purchaseResponseDTO);
            
        // Act
        PurchaseResponseDTO result = purchaseService.createPurchase(usuarioNormal.getEmail(), purchaseDTO);
        
        // Assert
        assertNotNull(result);
        assertEquals(purchaseResponseDTO.id(), result.id());
        assertEquals(purchaseResponseDTO.total(), result.total());
        verify(purchaseRepository).save(any(Purchase.class));
    }
    
    @Test
    void createPurchaseUserNotFoundThrowsException() {
        // Arrange
        when(usuarioRepository.findByEmail(anyString()))
            .thenReturn(Optional.empty());
            
        // Act & Assert
        assertThrows(RuntimeException.class,
            () -> purchaseService.createPurchase("nonexistent@example.com", purchaseDTO));
    }
    
    @Test
    void createPurchaseProductNotFoundThrowsException() {
        // Arrange
        when(usuarioRepository.findByEmail(usuarioNormal.getEmail()))
            .thenReturn(Optional.of(usuarioNormal));
        when(productoRepository.findById(anyLong()))
            .thenReturn(Optional.empty());
            
        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
            () -> purchaseService.createPurchase(usuarioNormal.getEmail(), purchaseDTO));
    }
    
    @Test
    void createPurchaseCalculatesCorrectTotal() {
        // Arrange
        when(usuarioRepository.findByEmail(usuarioNormal.getEmail()))
            .thenReturn(Optional.of(usuarioNormal));
        when(productoRepository.findById(1L))
            .thenReturn(Optional.of(producto1));
        when(productoRepository.findById(2L))
            .thenReturn(Optional.of(producto2));
        when(purchaseRepository.save(any(Purchase.class)))
            .thenAnswer(invocation -> {
                Purchase savedPurchase = invocation.getArgument(0);
                savedPurchase.setId(1L);
                return savedPurchase;
            });
            
        // Act
        purchaseService.createPurchase(usuarioNormal.getEmail(), purchaseDTO);
        
        // Assert
        ArgumentCaptor<Purchase> purchaseCaptor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchaseRepository).save(purchaseCaptor.capture());
        
        Purchase savedPurchase = purchaseCaptor.getValue();
        BigDecimal expectedTotal = new BigDecimal("400.00"); // (100 * 2) + (200 * 1)
        assertEquals(expectedTotal, savedPurchase.getTotal());
    }
}
