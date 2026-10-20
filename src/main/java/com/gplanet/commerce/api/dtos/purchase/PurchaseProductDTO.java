package com.gplanet.commerce.api.dtos.purchase;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Data Transfer Object (DTO) for product purchase requests.
 * This record represents the information needed to purchase a specific
 * product, including the product identifier and the desired quantity.
 *
 * @author Gustavo
 * @version 1.0
 * @param productId The ID of the product to be purchased.
 * @param quantity The quantity of the product to purchase.
 */
@Schema(name = "PurchaseProductRequest", description = "A product to buy and its quantity")
public record PurchaseProductDTO(
    @Schema(description = "ID of an active product", example = "3")
    @NotNull(message = "Product id is required")
    @Positive(message = "Product id must be positive")
    Long productId,

    @Schema(description = "Quantity to buy", example = "1")
    @NotNull(message = "The quantity of the product is required")
    @Positive(message = "The quantity of the product must be greater than 0")
    Integer quantity
) {}
