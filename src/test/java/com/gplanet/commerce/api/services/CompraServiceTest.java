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

import com.gplanet.commerce.api.dtos.compra.CompraDTO;
import com.gplanet.commerce.api.dtos.compra.CompraMapper;
import com.gplanet.commerce.api.dtos.compra.CompraProductoDTO;
import com.gplanet.commerce.api.dtos.compra.CompraResponseDTO;
import com.gplanet.commerce.api.entities.Compra;
import com.gplanet.commerce.api.entities.Producto;
import com.gplanet.commerce.api.entities.Usuario;
import com.gplanet.commerce.api.exceptions.ResourceNotFoundException;
import com.gplanet.commerce.api.repositories.CompraRepository;
import com.gplanet.commerce.api.repositories.ProductoRepository;
import com.gplanet.commerce.api.repositories.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class CompraServiceTest {
    @Mock
    private CompraMapper compraMapper;
    
    @Mock
    private CompraRepository compraRepository;
    
    @Mock
    private ProductoRepository productoRepository;
    
    @Mock
    private UsuarioRepository usuarioRepository;
    
    @InjectMocks
    private CompraService compraService;
    
    private Usuario usuarioNormal;
    private Usuario usuarioAdmin;
    private Producto producto1;
    private Producto producto2;
    private Compra compra;
    private CompraDTO compraDTO;
    private CompraResponseDTO compraResponseDTO;
    
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
        
        // Setup CompraDTO
        CompraProductoDTO compraProductoDTO1 = new CompraProductoDTO(1L, 2);
        CompraProductoDTO compraProductoDTO2 = new CompraProductoDTO(2L, 1);
        compraDTO = new CompraDTO(Arrays.asList(compraProductoDTO1, compraProductoDTO2));
        
        // Setup Compra
        compra = new Compra();
        compra.setId(1L);
        compra.setUsuario(usuarioNormal);
        compra.setFecha(LocalDateTime.now());
        compra.setTotal(new BigDecimal("400.00"));
        
        // Setup CompraResponseDTO
        compraResponseDTO = new CompraResponseDTO(
            1L,
            usuarioNormal.getNombre(),
            compra.getFecha(),
            compra.getTotal(),
            null // productos, no se usan en los asserts
        );
    }
    
    @Test
    void listarCompras_UserRole_ReturnsUserCompras() {
        // Arrange
        Page<Compra> comprasPage = new PageImpl<>(Collections.singletonList(compra));
        when(usuarioRepository.findByEmail(usuarioNormal.getEmail()))
            .thenReturn(Optional.of(usuarioNormal));
        when(compraRepository.findByUsuario(eq(usuarioNormal), any(Pageable.class)))
            .thenReturn(comprasPage);
        when(compraMapper.toCompraResponseDTO(compra))
            .thenReturn(compraResponseDTO);
            
        // Act
        Page<CompraResponseDTO> result = compraService.listarCompras(
            usuarioNormal.getEmail(), 
            0, 
            10, 
            "fecha", 
            "DESC"
        );
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(compraResponseDTO, result.getContent().get(0));
        verify(compraRepository).findByUsuario(eq(usuarioNormal), any(Pageable.class));
        verify(compraRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void listarCompras_AdminRole_ReturnsAllCompras() {
        // Arrange
        Page<Compra> comprasPage = new PageImpl<>(Collections.singletonList(compra));
        when(usuarioRepository.findByEmail(usuarioAdmin.getEmail()))
            .thenReturn(Optional.of(usuarioAdmin));
        when(compraRepository.findAll(any(Pageable.class)))
            .thenReturn(comprasPage);
        when(compraMapper.toCompraResponseDTO(compra))
            .thenReturn(compraResponseDTO);
            
        // Act
        Page<CompraResponseDTO> result = compraService.listarCompras(
            usuarioAdmin.getEmail(), 
            0, 
            10, 
            "fecha", 
            "DESC"
        );
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(compraResponseDTO, result.getContent().get(0));
        verify(compraRepository).findAll(any(Pageable.class));
        verify(compraRepository, never()).findByUsuario(any(), any(Pageable.class));
    }

    @Test
    void listarCompras_UserNotFound_ThrowsException() {
        // Arrange
        when(usuarioRepository.findByEmail(anyString()))
            .thenReturn(Optional.empty());
            
        // Act & Assert
        assertThrows(UsernameNotFoundException.class,
            () -> compraService.listarCompras("nonexistent@example.com", 0, 10, "fecha", "DESC"));
    }
    
    @Test
    void realizarCompra_Success() {
        // Arrange
        when(usuarioRepository.findByEmail(usuarioNormal.getEmail()))
            .thenReturn(Optional.of(usuarioNormal));
        when(productoRepository.findById(1L))
            .thenReturn(Optional.of(producto1));
        when(productoRepository.findById(2L))
            .thenReturn(Optional.of(producto2));
        when(compraRepository.save(any(Compra.class)))
            .thenReturn(compra);
        when(compraMapper.toCompraResponseDTO(any(Compra.class)))
            .thenReturn(compraResponseDTO);
            
        // Act
        CompraResponseDTO result = compraService.realizarCompra(usuarioNormal.getEmail(), compraDTO);
        
        // Assert
        assertNotNull(result);
        assertEquals(compraResponseDTO.id(), result.id());
        assertEquals(compraResponseDTO.total(), result.total());
        verify(compraRepository).save(any(Compra.class));
    }
    
    @Test
    void realizarCompra_UserNotFound_ThrowsException() {
        // Arrange
        when(usuarioRepository.findByEmail(anyString()))
            .thenReturn(Optional.empty());
            
        // Act & Assert
        assertThrows(RuntimeException.class,
            () -> compraService.realizarCompra("nonexistent@example.com", compraDTO));
    }
    
    @Test
    void realizarCompra_ProductNotFound_ThrowsException() {
        // Arrange
        when(usuarioRepository.findByEmail(usuarioNormal.getEmail()))
            .thenReturn(Optional.of(usuarioNormal));
        when(productoRepository.findById(anyLong()))
            .thenReturn(Optional.empty());
            
        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
            () -> compraService.realizarCompra(usuarioNormal.getEmail(), compraDTO));
    }
    
    @Test
    void realizarCompra_CalculatesCorrectTotal() {
        // Arrange
        when(usuarioRepository.findByEmail(usuarioNormal.getEmail()))
            .thenReturn(Optional.of(usuarioNormal));
        when(productoRepository.findById(1L))
            .thenReturn(Optional.of(producto1));
        when(productoRepository.findById(2L))
            .thenReturn(Optional.of(producto2));
        when(compraRepository.save(any(Compra.class)))
            .thenAnswer(invocation -> {
                Compra savedCompra = invocation.getArgument(0);
                savedCompra.setId(1L);
                return savedCompra;
            });
            
        // Act
        compraService.realizarCompra(usuarioNormal.getEmail(), compraDTO);
        
        // Assert
        ArgumentCaptor<Compra> compraCaptor = ArgumentCaptor.forClass(Compra.class);
        verify(compraRepository).save(compraCaptor.capture());
        
        Compra savedCompra = compraCaptor.getValue();
        BigDecimal expectedTotal = new BigDecimal("400.00"); // (100 * 2) + (200 * 1)
        assertEquals(expectedTotal, savedCompra.getTotal());
    }
}