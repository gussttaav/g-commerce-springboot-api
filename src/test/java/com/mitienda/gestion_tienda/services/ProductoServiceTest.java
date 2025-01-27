package com.mitienda.gestion_tienda.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

import com.mitienda.gestion_tienda.dtos.producto.ProductoDTO;
import com.mitienda.gestion_tienda.dtos.producto.ProductoMapper;
import com.mitienda.gestion_tienda.dtos.producto.ProductoResponseDTO;
import com.mitienda.gestion_tienda.dtos.producto.ProductStatus;
import com.mitienda.gestion_tienda.entities.Producto;
import com.mitienda.gestion_tienda.exceptions.ResourceNotFoundException;
import com.mitienda.gestion_tienda.repositories.ProductoRepository;

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
        productoDTO = ProductoDTO.builder()
                .nombre("Test Product")
                .descripcion("Test Description")
                .precio(new BigDecimal("99.99"))
                .activo(true)
                .build();

        producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Test Product");
        producto.setDescripcion("Test Description");
        producto.setPrecio(new BigDecimal("99.99"));
        producto.setFechaCreacion(LocalDateTime.now());
        producto.setActivo(true);

        productoResponseDTO = ProductoResponseDTO.builder()
                .id(1L)
                .nombre("Test Product")
                .descripcion("Test Description")
                .precio(new BigDecimal("99.99"))
                .fechaCreacion(producto.getFechaCreacion())
                .activo(true)
                .build();

        productoInactivo = new Producto();
        productoInactivo.setId(2L);
        productoInactivo.setNombre("Inactive Product");
        productoInactivo.setDescripcion("Inactive Description");
        productoInactivo.setPrecio(new BigDecimal("49.99"));
        productoInactivo.setFechaCreacion(LocalDateTime.now());
        productoInactivo.setActivo(false);

        productoInactivoResponseDTO = ProductoResponseDTO.builder()
                .id(2L)
                .nombre("Inactive Product")
                .descripcion("Inactive Description")
                .precio(new BigDecimal("49.99"))
                .fechaCreacion(productoInactivo.getFechaCreacion())
                .activo(false)
                .build();
    }

    /**
     * Verifies that products are correctly listed by status.
     */
    @Test
    void listarProductos_ConEstadoActivo_DebeRetornarSoloProductosActivos() {
        // Arrange
        when(productoRepository.findByActivoTrue()).thenReturn(List.of(producto));
        when(productoMapper.toProductoResponseDTO(producto)).thenReturn(productoResponseDTO);

        // Act
        List<ProductoResponseDTO> resultado = productoService.listarProductos(ProductStatus.ACTIVE);

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals(productoResponseDTO, resultado.get(0));
        verify(productoRepository).findByActivoTrue();
        verify(productoMapper).toProductoResponseDTO(producto);
    }

    /**
     * Verifies that products are correctly listed by status.
     */
    @Test
    void listarProductos_ConEstadoInactivo_DebeRetornarSoloProductosInactivos() {
        // Arrange
        when(productoRepository.findByActivoFalse()).thenReturn(List.of(productoInactivo));
        when(productoMapper.toProductoResponseDTO(productoInactivo))
            .thenReturn(productoInactivoResponseDTO);

        // Act
        List<ProductoResponseDTO> resultado = productoService.listarProductos(ProductStatus.INACTIVE);

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals(productoInactivoResponseDTO, resultado.get(0));
        verify(productoRepository).findByActivoFalse();
        verify(productoMapper).toProductoResponseDTO(productoInactivo);
    }

    /**
     * Verifies that products are correctly listed by status.
     */
    @Test
    void listarProductos_ConEstadoAll_DebeRetornarTodosLosProductos() {
        // Arrange
        List<Producto> todosLosProductos = List.of(producto, productoInactivo);
        when(productoRepository.findAll()).thenReturn(todosLosProductos);
        when(productoMapper.toProductoResponseDTO(producto)).thenReturn(productoResponseDTO);
        when(productoMapper.toProductoResponseDTO(productoInactivo))
            .thenReturn(productoInactivoResponseDTO);

        // Act
        List<ProductoResponseDTO> resultado = productoService.listarProductos(ProductStatus.ALL);

        // Assert
        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        assertTrue(resultado.contains(productoResponseDTO));
        assertTrue(resultado.contains(productoInactivoResponseDTO));
        verify(productoRepository).findAll();
        verify(productoMapper, times(2)).toProductoResponseDTO(any(Producto.class));
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
        assertEquals(productoResponseDTO.getId(), resultado.getId());
        assertEquals(productoResponseDTO.getNombre(), resultado.getNombre());
        assertNotNull(resultado.getFechaCreacion());
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
}