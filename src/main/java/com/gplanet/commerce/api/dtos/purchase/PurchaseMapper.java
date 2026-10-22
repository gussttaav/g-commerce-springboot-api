package com.gplanet.commerce.api.dtos.purchase;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
     * Converts a PurchaseProduct entity to its response DTO representation.
     * This method maps the product details, quantity, and pricing information
     * to the corresponding DTO fields.
     *
     * <p>The unit price is the price paid, derived from the stored subtotal: the product's
     * current price may have changed since the purchase.</p>
     *
     * @param entity the purchase product entity to convert
     * @return the corresponding PurchaseProductResponseDTO
     */
    private PurchaseProductResponseDTO toPurchaseProductResponseDTO(PurchaseProduct entity) {
        BigDecimal subtotal = entity.getSubtotal();
        BigDecimal unitPrice = subtotal.divide(
            BigDecimal.valueOf(entity.getQuantity()), subtotal.scale(), RoundingMode.HALF_UP);
        return new PurchaseProductResponseDTO(
            entity.getProduct().getId(),
            entity.getProduct().getName(),
            unitPrice,
            entity.getQuantity(),
            subtotal
        );
    }
}
