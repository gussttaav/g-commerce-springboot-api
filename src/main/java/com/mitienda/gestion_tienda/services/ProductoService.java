package com.mitienda.gestion_tienda.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mitienda.gestion_tienda.dtos.producto.ProductoDTO;
import com.mitienda.gestion_tienda.dtos.producto.ProductoMapper;
import com.mitienda.gestion_tienda.dtos.producto.ProductoResponseDTO;
import com.mitienda.gestion_tienda.entities.Producto;
import com.mitienda.gestion_tienda.exceptions.ResourceNotFoundException;
import com.mitienda.gestion_tienda.repositories.ProductoRepository;
import com.mitienda.gestion_tienda.utilities.DatabaseOperationHandler;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoMapper productoMapper;
    private final ProductoRepository productoRepository;

    public List<ProductoResponseDTO> listarProductos() {
        return productoRepository.findByActivoTrue()
            .stream()
            .map(productoMapper::toProductoResponseDTO)
            .toList();
    }

    @Transactional
    public ProductoResponseDTO crearProducto(ProductoDTO productoDTO) {
        Producto producto = productoMapper.toProducto(productoDTO);
        producto.setFechaCreacion(LocalDateTime.now());
        
        return productoMapper.toProductoResponseDTO((
            DatabaseOperationHandler.executeOperation(() -> 
                productoRepository.save(producto)
        )));
    }

    @Transactional
    public void eliminarProducto(Long id) {
        Producto producto = productoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        producto.setActivo(false);
        productoRepository.save(producto);
    }
}
