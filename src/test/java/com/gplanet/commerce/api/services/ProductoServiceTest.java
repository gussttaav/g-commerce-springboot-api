package com.gplanet.commerce.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.gplanet.commerce.api.dtos.producto.ProductStatus;
import com.gplanet.commerce.api.dtos.producto.ProductoDTO;
import com.gplanet.commerce.api.dtos.producto.ProductoMapper;
import com.gplanet.commerce.api.dtos.producto.ProductoResponseDTO;
import com.gplanet.commerce.api.entities.Producto;
import com.gplanet.commerce.api.exceptions.ResourceNotFoundException;
import com.gplanet.commerce.api.repositories.ProductoRepository;

/**
 * Unit tests for ProductoService.
 * Tests product management operations.
 */
@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoMapper productoMapper;

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private ProductoService productoService;

    private ProductoDTO productoDTO;
    private Producto producto;
    private ProductoResponseDTO productoResponseDTO;
    private Producto productoInactivo;
    private ProductoResponseDTO productoInactivoResponseDTO;

    @BeforeEach
    void setUp() {
        productoDTO = new ProductoDTO(
            "Test Product",
            "Test Description",
            new BigDecimal("99.99"),
            true
        );

        producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Test Product");
        producto.setDescripcion("Test Description");
        producto.setPrecio(new BigDecimal("99.99"));
        producto.setFechaCreacion(LocalDateTime.now());
        producto.setActivo(true);

        productoResponseDTO = new ProductoResponseDTO(
            1L,
            "Test Product",
            "Test Description",
            new BigDecimal("99.99"),
            producto.getFechaCreacion(),
            true
        );

        productoInactivo = new Producto();
        productoInactivo.setId(2L);
        productoInactivo.setNombre("Inactive Product");
        productoInactivo.setDescripcion("Inactive Description");
        productoInactivo.setPrecio(new BigDecimal("49.99"));
        productoInactivo.setFechaCreacion(LocalDateTime.now());
        productoInactivo.setActivo(false);

        productoInactivoResponseDTO = new ProductoResponseDTO(
            2L,
            "Inactive Product",
            "Inactive Description",
            new BigDecimal("49.99"),
            productoInactivo.getFechaCreacion(),
            false
        );
    }

    /**
     * Verifies that products are correctly listed by status without search.
     */
    @Test
    void listarProductos_ConEstadoActivo_DebeRetornarSoloProductosActivos() {
        // Arrange
        Page<Producto> mockPage = new PageImpl<>(List.of(producto));
        when(productoRepository.findByActivoTrue(any(Pageable.class))).thenReturn(mockPage);
        when(productoMapper.toProductoResponseDTO(producto)).thenReturn(productoResponseDTO);

        // Act - now with null searchText
        Page<ProductoResponseDTO> resultado = productoService.listarProductos(ProductStatus.ACTIVE, null, 0, 10, "nombre", "ASC");

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.getTotalElements());
        assertEquals(productoResponseDTO, resultado.getContent().get(0));
        verify(productoRepository).findByActivoTrue(any(Pageable.class));
        verify(productoMapper).toProductoResponseDTO(producto);
    }

    /**
     * Verifies that products are correctly listed by status without search.
     */
    @Test
    void listarProductos_ConEstadoInactivo_DebeRetornarSoloProductosInactivos() {
        // Arrange
        Page<Producto> mockPage = new PageImpl<>(List.of(productoInactivo));
        when(productoRepository.findByActivoFalse(any(Pageable.class))).thenReturn(mockPage);
        when(productoMapper.toProductoResponseDTO(productoInactivo))
            .thenReturn(productoInactivoResponseDTO);

        // Act - now with null searchText
        Page<ProductoResponseDTO> resultado = productoService.listarProductos(ProductStatus.INACTIVE, null, 0, 10, "nombre", "ASC");

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.getTotalElements());
        assertEquals(productoInactivoResponseDTO, resultado.getContent().get(0));
        verify(productoRepository).findByActivoFalse(any(Pageable.class));
        verify(productoMapper).toProductoResponseDTO(productoInactivo);
    }

    /**
     * Verifies that products are correctly listed by status without search.
     */
    @Test
    void listarProductos_ConEstadoAll_DebeRetornarTodosLosProductos() {
        // Arrange
        List<Producto> todosLosProductos = List.of(producto, productoInactivo);
        Page<Producto> mockPage = new PageImpl<>(todosLosProductos);
        when(productoRepository.findAll(any(Pageable.class))).thenReturn(mockPage);
        when(productoMapper.toProductoResponseDTO(producto)).thenReturn(productoResponseDTO);
        when(productoMapper.toProductoResponseDTO(productoInactivo))
            .thenReturn(productoInactivoResponseDTO);

        // Act - now with null searchText
        Page<ProductoResponseDTO> resultado = productoService.listarProductos(ProductStatus.ALL, null, 0, 10, "nombre", "ASC");

        // Assert
        assertNotNull(resultado);
        assertEquals(2, resultado.getTotalElements());
        assertTrue(resultado.getContent().contains(productoResponseDTO));
        assertTrue(resultado.getContent().contains(productoInactivoResponseDTO));
        verify(productoRepository).findAll(any(Pageable.class));
        verify(productoMapper, times(2)).toProductoResponseDTO(any(Producto.class));
    }
    
    /**
     * Verifies that search works correctly for active products.
     */
    @Test
    void listarProductos_ConEstadoActivoYBusqueda_DebeRetornarProductosActivosConCoincidencia() {
        // Arrange
        String searchTerm = "Test";
        String searchTermWithWildcards = "%test%";
        Page<Producto> mockPage = new PageImpl<>(List.of(producto));
        
        when(productoRepository.findByActivoTrueAndSearch(eq(searchTermWithWildcards), any(Pageable.class)))
            .thenReturn(mockPage);
        when(productoMapper.toProductoResponseDTO(producto)).thenReturn(productoResponseDTO);

        // Act
        Page<ProductoResponseDTO> resultado = productoService.listarProductos(
            ProductStatus.ACTIVE, searchTerm, 0, 10, "nombre", "ASC");

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.getTotalElements());
        assertEquals(productoResponseDTO, resultado.getContent().get(0));
        verify(productoRepository).findByActivoTrueAndSearch(eq(searchTermWithWildcards), any(Pageable.class));
        verify(productoMapper).toProductoResponseDTO(producto);
    }
    
    /**
     * Verifies that search works correctly for inactive products.
     */
    @Test
    void listarProductos_ConEstadoInactivoYBusqueda_DebeRetornarProductosInactivosConCoincidencia() {
        // Arrange
        String searchTerm = "Inactive";
        String searchTermWithWildcards = "%inactive%";
        Page<Producto> mockPage = new PageImpl<>(List.of(productoInactivo));
        
        when(productoRepository.findByActivoFalseAndSearch(eq(searchTermWithWildcards), any(Pageable.class)))
            .thenReturn(mockPage);
        when(productoMapper.toProductoResponseDTO(productoInactivo))
            .thenReturn(productoInactivoResponseDTO);

        // Act
        Page<ProductoResponseDTO> resultado = productoService.listarProductos(
            ProductStatus.INACTIVE, searchTerm, 0, 10, "nombre", "ASC");

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.getTotalElements());
        assertEquals(productoInactivoResponseDTO, resultado.getContent().get(0));
        verify(productoRepository).findByActivoFalseAndSearch(eq(searchTermWithWildcards), any(Pageable.class));
        verify(productoMapper).toProductoResponseDTO(productoInactivo);
    }
    
    /**
     * Verifies that search works correctly for all products.
     */
    @Test
    void listarProductos_ConEstadoAllYBusqueda_DebeRetornarTodosLosProductosConCoincidencia() {
        // Arrange
        String searchTerm = "Product";
        String searchTermWithWildcards = "%product%";
        List<Producto> todosLosProductos = List.of(producto, productoInactivo);
        Page<Producto> mockPage = new PageImpl<>(todosLosProductos);
        
        when(productoRepository.findBySearch(eq(searchTermWithWildcards), any(Pageable.class)))
            .thenReturn(mockPage);
        when(productoMapper.toProductoResponseDTO(producto)).thenReturn(productoResponseDTO);
        when(productoMapper.toProductoResponseDTO(productoInactivo))
            .thenReturn(productoInactivoResponseDTO);

        // Act
        Page<ProductoResponseDTO> resultado = productoService.listarProductos(
            ProductStatus.ALL, searchTerm, 0, 10, "nombre", "ASC");

        // Assert
        assertNotNull(resultado);
        assertEquals(2, resultado.getTotalElements());
        assertTrue(resultado.getContent().contains(productoResponseDTO));
        assertTrue(resultado.getContent().contains(productoInactivoResponseDTO));
        verify(productoRepository).findBySearch(eq(searchTermWithWildcards), any(Pageable.class));
        verify(productoMapper, times(2)).toProductoResponseDTO(any(Producto.class));
    }
    
    /**
     * Verifies that empty search text is handled correctly.
     */
    @Test
    void listarProductos_ConBusquedaVacia_DebeIgnorarBusqueda() {
        // Arrange
        String searchTerm = "";  // Empty search
        Page<Producto> mockPage = new PageImpl<>(List.of(producto));
        when(productoRepository.findByActivoTrue(any(Pageable.class))).thenReturn(mockPage);
        when(productoMapper.toProductoResponseDTO(producto)).thenReturn(productoResponseDTO);

        // Act
        Page<ProductoResponseDTO> resultado = productoService.listarProductos(
            ProductStatus.ACTIVE, searchTerm, 0, 10, "nombre", "ASC");

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.getTotalElements());
        verify(productoRepository).findByActivoTrue(any(Pageable.class));
        // Verify search methods were not called
        verify(productoRepository, never()).findByActivoTrueAndSearch(anyString(), any(Pageable.class));
    }

    /**
     * Verifies successful product creation.
     */
    @Test
    void crearProducto_DebeCrearYRetornarNuevoProducto() {
        // Arrange
        when(productoMapper.toProducto(productoDTO)).thenReturn(producto);
        when(productoRepository.save(any(Producto.class))).thenReturn(producto);
        when(productoMapper.toProductoResponseDTO(producto)).thenReturn(productoResponseDTO);

        // Act
        ProductoResponseDTO resultado = productoService.crearProducto(productoDTO);

        // Assert
        assertNotNull(resultado);
        assertEquals(productoResponseDTO.id(), resultado.id());
        assertEquals(productoResponseDTO.nombre(), resultado.nombre());
        assertNotNull(resultado.fechaCreacion());
        verify(productoMapper).toProducto(productoDTO);
        verify(productoRepository).save(any(Producto.class));
        verify(productoMapper).toProductoResponseDTO(producto);
    }

    /**
     * Verifies product deactivation.
     */
    @Test
    void eliminarProducto_DebeDesactivarProductoExistente() {
        // Arrange
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        // Act
        productoService.eliminarProducto(1L);

        // Assert
        assertFalse(producto.isActivo());
        verify(productoRepository).findById(1L);
        verify(productoRepository).save(producto);
    }

    @Test
    void eliminarProducto_DebeLanzarExcepcionCuandoProductoNoExiste() {
        // Arrange
        when(productoRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> 
            productoService.eliminarProducto(1L)
        );
        verify(productoRepository).findById(1L);
        verify(productoRepository, never()).save(any());
    }

    @Test
    void crearProducto_DebeManipularErroresDeBD() {
        // Arrange
        when(productoMapper.toProducto(productoDTO)).thenReturn(producto);
        when(productoRepository.save(any(Producto.class)))
            .thenThrow(new RuntimeException("Error de BD"));

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
            productoService.crearProducto(productoDTO)
        );
        verify(productoMapper).toProducto(productoDTO);
        verify(productoRepository).save(any(Producto.class));
    }

    /**
     * Verifies successful product update.
     */
    @Test
    void actualizarProducto_DebeActualizarYRetornarProductoExistente() {
        // Arrange
        ProductoDTO productoActualizadoDTO = new ProductoDTO(
            "Updated Product",
            "Updated Description",
            new BigDecimal("199.99"),
            true
        );

        Producto productoActualizado = new Producto();
        productoActualizado.setId(1L);
        productoActualizado.setNombre("Updated Product");
        productoActualizado.setDescripcion("Updated Description");
        productoActualizado.setPrecio(new BigDecimal("199.99"));
        productoActualizado.setFechaCreacion(producto.getFechaCreacion());
        productoActualizado.setActivo(true);

        ProductoResponseDTO productoActualizadoResponseDTO = new ProductoResponseDTO(
            1L,
            "Updated Product",
            "Updated Description",
            new BigDecimal("199.99"),
            producto.getFechaCreacion(),
            true
        );

        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        doNothing().when(productoMapper).updateProductoFromDTO(productoActualizadoDTO, producto);
        when(productoRepository.save(any(Producto.class))).thenReturn(productoActualizado);
        when(productoMapper.toProductoResponseDTO(productoActualizado))
            .thenReturn(productoActualizadoResponseDTO);

        // Act
        ProductoResponseDTO resultado = productoService.actualizarProducto(1L, productoActualizadoDTO);

        // Assert
        assertNotNull(resultado);
        assertEquals(productoActualizadoResponseDTO.id(), resultado.id());
        assertEquals(productoActualizadoResponseDTO.nombre(), resultado.nombre());
        assertEquals(productoActualizadoResponseDTO.descripcion(), resultado.descripcion());
        assertEquals(productoActualizadoResponseDTO.precio(), resultado.precio());
        verify(productoRepository).findById(1L);
        verify(productoMapper).updateProductoFromDTO(productoActualizadoDTO, producto);
        verify(productoRepository).save(any(Producto.class));
        verify(productoMapper).toProductoResponseDTO(productoActualizado);
    }

    /**
     * Verifies that updating a non-existent product throws ResourceNotFoundException.
     */
    @Test
    void actualizarProducto_DebeLanzarExcepcionCuandoProductoNoExiste() {
        // Arrange
        when(productoRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> 
            productoService.actualizarProducto(1L, productoDTO)
        );
        verify(productoRepository).findById(1L);
        verify(productoMapper, never()).updateProductoFromDTO(any(), any());
        verify(productoRepository, never()).save(any());
    }

    /**
     * Verifies that database errors during update are properly handled.
     */
    @Test
    void actualizarProducto_DebeManipularErroresDeBD() {
        // Arrange
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        doNothing().when(productoMapper).updateProductoFromDTO(productoDTO, producto);
        when(productoRepository.save(any(Producto.class)))
            .thenThrow(new RuntimeException("Error de BD"));

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
            productoService.actualizarProducto(1L, productoDTO)
        );
        verify(productoRepository).findById(1L);
        verify(productoMapper).updateProductoFromDTO(productoDTO, producto);
        verify(productoRepository).save(any(Producto.class));
    }
}