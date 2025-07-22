package com.gplanet.commerce.api.dtos.compra;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

/**
 * Data Transfer Object (DTO) for product purchase requests.
 * This record represents the information needed to purchase a specific
 * product, including the product identifier and the desired quantity.
 *
 * @author Gustavo
 * @version 1.0
 * @param productoId The ID of the product to be purchased.
 * @param cantidad The quantity of the product to purchase.
 */
@Schema(name = "PurchaseProductRequest", description = "Product purchase request information")
public record CompraProductoDTO(
    @Schema(description = "Product ID to purchase", example = "1")
    @Positive(message = "El ID del producto debe ser válido")
    Long productoId,

    @Schema(description = "Quantity to purchase", example = "1")
    @Positive(message = "La cantidad debe ser mayor a 0")
    Integer cantidad
) {}
