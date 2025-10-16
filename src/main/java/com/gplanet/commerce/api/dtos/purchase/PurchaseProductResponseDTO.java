package com.gplanet.commerce.api.dtos.purchase;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data Transfer Object (DTO) for product purchase details.
 * This record represents detailed information about each product
 * in a purchase, including quantity, price, and subtotal calculations.
 *
 * @author Gustavo
 * @version 1.0
 * @param id The unique identifier of the product.
 * @param productName The name of the purchased product.
 * @param unitPrice The unit price of the product at the time of purchase.
 * @param quantity The quantity of the product purchased.
 * @param subtotal The subtotal for this product (unit price * quantity).
 */
@Schema(name = "PurchasedProduct", 
        description = "Purchased product details")
public record PurchaseProductResponseDTO(
    @Schema(description = "Product ID", example = "1")
    Long id,

    @Schema(description = "Product name", example = "Product A")
    String productName,

    @Schema(description = "Unit price", example = "19.99")
    BigDecimal unitPrice,

    @Schema(description = "Quantity purchased", example = "2")
    Integer quantity,

    @Schema(description = "Subtotal for this product", example = "39.98")
    BigDecimal subtotal
) {}
