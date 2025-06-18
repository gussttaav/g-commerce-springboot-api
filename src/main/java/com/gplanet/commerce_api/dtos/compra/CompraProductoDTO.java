package com.gplanet.commerce_api.dtos.compra;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

/**
 * Data Transfer Object (DTO) for product purchase requests.
 * This record represents the information needed to purchase a specific
 * product, including the product identifier and the desired quantity.
 *
 * @author Gustavo
 * @version 1.0
 */
@Schema(name = "PurchaseProductRequest", description = "Product purchase request information")
public record CompraProductoDTO(
    /**
     * The ID of the product to be purchased.
     * Must be a positive number.
     */
    @Schema(description = "Product ID to purchase", example = "1")
    @Positive(message = "El ID del producto debe ser válido")
    Long productoId,
    
    /**
     * The quantity of the product to purchase.
     * Must be a positive number.
     */
    @Schema(description = "Quantity to purchase", example = "1")
    @Positive(message = "La cantidad debe ser mayor a 0")
    Integer cantidad
) {}
