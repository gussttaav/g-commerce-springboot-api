package com.mitienda.gestion_tienda.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
        log.debug("Listing products with status: {}", status);
        List<Producto> productos = switch (status) {
            case ACTIVE -> productoRepository.findByActivoTrue();
            case INACTIVE -> productoRepository.findByActivoFalse();
            case ALL -> productoRepository.findAll();
        };
        log.debug("Found {} products", productos.size());
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
        log.info("Creating new product: {}", productoDTO.getNombre());
        Producto producto = productoMapper.toProducto(productoDTO);
        producto.setFechaCreacion(LocalDateTime.now());
        
        Producto savedProducto = DatabaseOperationHandler.executeOperation(() -> 
            productoRepository.save(producto)
        );
        log.info("Product created with ID: {}", savedProducto.getId());
        return productoMapper.toProductoResponseDTO(savedProducto);
    }

    /**
     * Updates an existing product in the system.
     * 
     * @param id ID of the product to update
     * @param productoDTO Data transfer object containing updated product information
     * @return ProductoResponseDTO containing the updated product's information
     * @throws ResourceNotFoundException if product is not found
     */
    @Transactional
    public ProductoResponseDTO actualizarProducto(Long id, ProductoDTO productoDTO) {
        log.info("Updating product with ID: {}", id);
        Producto producto = productoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        
        productoMapper.updateProductoFromDTO(productoDTO, producto);
        
        Producto updatedProducto = DatabaseOperationHandler.executeOperation(() -> 
            productoRepository.save(producto)
        );
        log.info("Product successfully updated - ID: {}", updatedProducto.getId());
        return productoMapper.toProductoResponseDTO(updatedProducto);
    }

    /**
     * Logically deletes a product by marking it as inactive.
     * 
     * @param id ID of the product to delete
     * @throws ResourceNotFoundException if product is not found
     */
    @Transactional
    public void eliminarProducto(Long id) {
        log.info("Attempting to delete product with ID: {}", id);
        Producto producto = productoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        producto.setActivo(false);
        productoRepository.save(producto);
        log.info("Product successfully marked as inactive - ID: {}", id);
    }
}
