package com.gplanet.commerce.api.dtos.purchase;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.gplanet.commerce.api.entities.Purchase;
import com.gplanet.commerce.api.entities.PurchaseProduct;

/**
 * Mapper class responsible for converting between Purchase entities and DTOs.
 * This class provides methods to transform purchase-related entities into
 * their corresponding DTO representations for API responses.
 *
 * @author Gustavo
 * @version 1.0
 */
@Component
public class PurchaseMapper {
    
    /**
     * Converts a Purchase entity to its response DTO representation.
     * This method maps all the purchase information including the list
     * of purchased products to their DTO representations.
     *
     * @param entity the purchase entity to convert
     * @return the corresponding PurchaseResponseDTO with all purchase information
     */
    public PurchaseResponseDTO toPurchaseResponseDTO(Purchase entity) {
        return new PurchaseResponseDTO(
            entity.getId(),
            entity.getUser().getName(),
            entity.getDate(),
            entity.getTotal(),
            entity.getProducts().stream()
                .map(this::toPurchaseProductResponseDTO)
                .collect(Collectors.toList())
        );
    }

    /**
     * Converts a PurchasProduct entity to its response DTO representation.
     * This method maps the product details, quantity, and pricing information
     * to the corresponding DTO fields.
     *
     * @param entity the purchase product entity to convert
     * @return the corresponding PurchaseProductResponseDTO
     */
    private PurchaseProductResponseDTO toPurchaseProductResponseDTO(PurchaseProduct entity) {
        return new PurchaseProductResponseDTO(
            entity.getProduct().getId(),
            entity.getProduct().getName(),
            entity.getProduct().getPrice(),
            entity.getQuantity(),
            entity.getSubtotal()
        );
    }
}
