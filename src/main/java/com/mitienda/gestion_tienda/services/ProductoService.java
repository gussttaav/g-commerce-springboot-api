package com.mitienda.gestion_tienda.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mitienda.gestion_tienda.dtos.producto.ProductStatus;
import com.mitienda.gestion_tienda.dtos.producto.ProductoDTO;
import com.mitienda.gestion_tienda.dtos.producto.ProductoMapper;
import com.mitienda.gestion_tienda.dtos.producto.ProductoResponseDTO;
import com.mitienda.gestion_tienda.entities.Producto;
import com.mitienda.gestion_tienda.exceptions.ResourceNotFoundException;
import com.mitienda.gestion_tienda.repositories.ProductoRepository;
import com.mitienda.gestion_tienda.utilities.DatabaseOperationHandler;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service class that handles product-related operations including
 * listing, creation, and logical deletion of products.
 * 
 * @author Gustavo
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoMapper productoMapper;
    private final ProductoRepository productoRepository;

    /**
     * Lists products based on the specified status.
     * 
     * @param status The status to filter products by
     * @return List of ProductoResponseDTO containing filtered products' information
     */
    public List<ProductoResponseDTO> listarProductos(ProductStatus status) {
        List<Producto> productos = switch (status) {
            case ACTIVE -> productoRepository.findByActivoTrue();
            case INACTIVE -> productoRepository.findByActivoFalse();
            case ALL -> productoRepository.findAll();
        };
        
        return productos.stream()
                .map(productoMapper::toProductoResponseDTO)
                .toList();
    }

    /**
     * Creates a new product in the system.
     * 
     * @param productoDTO Data transfer object containing product information
     * @return ProductoResponseDTO containing the created product's information
     */
    @Transactional
    public ProductoResponseDTO crearProducto(ProductoDTO productoDTO) {
        Producto producto = productoMapper.toProducto(productoDTO);
        producto.setFechaCreacion(LocalDateTime.now());
        
        return productoMapper.toProductoResponseDTO((
            DatabaseOperationHandler.executeOperation(() -> 
                productoRepository.save(producto)
        )));
    }

    /**
     * Logically deletes a product by marking it as inactive.
     * 
     * @param id ID of the product to delete
     * @throws ResourceNotFoundException if product is not found
     */
    @Transactional
    public void eliminarProducto(Long id) {
        Producto producto = productoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        producto.setActivo(false);
        productoRepository.save(producto);
    }
}
