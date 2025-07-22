package com.gplanet.commerce.api.dtos.compra;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.gplanet.commerce.api.entities.Compra;
import com.gplanet.commerce.api.entities.CompraProducto;

/**
 * Mapper class responsible for converting between Compra entities and DTOs.
 * This class provides methods to transform purchase-related entities into
 * their corresponding DTO representations for API responses.
 *
 * @author Gustavo
 * @version 1.0
 */
@Component
public class CompraMapper {
    
    /**
     * Converts a Compra entity to its response DTO representation.
     * This method maps all the purchase information including the list
     * of purchased products to their DTO representations.
     *
     * @param entity the purchase entity to convert
     * @return the corresponding CompraResponseDTO with all purchase information
     */
    public CompraResponseDTO toCompraResponseDTO(Compra entity) {
        return new CompraResponseDTO(
            entity.getId(),
            entity.getUsuario().getNombre(),
            entity.getFecha(),
            entity.getTotal(),
            entity.getProductos().stream()
                .map(this::toCompraProductoResponseDTO)
                .collect(Collectors.toList())
        );
    }

    /**
     * Converts a CompraProducto entity to its response DTO representation.
     * This method maps the product details, quantity, and pricing information
     * to the corresponding DTO fields.
     *
     * @param entity the purchase product entity to convert
     * @return the corresponding CompraProductoResponseDTO
     */
    private CompraProductoResponseDTO toCompraProductoResponseDTO(CompraProducto entity) {
        return new CompraProductoResponseDTO(
            entity.getProducto().getId(),
            entity.getProducto().getNombre(),
            entity.getProducto().getPrecio(),
            entity.getCantidad(),
            entity.getSubtotal()
        );
    }
}
