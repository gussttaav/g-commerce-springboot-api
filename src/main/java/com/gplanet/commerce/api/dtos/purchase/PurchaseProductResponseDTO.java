package com.gplanet.commerce.api.dtos.purchase;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

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
@Schema(name = "PurchasedProduct", description = "A product of a purchase")
public record PurchaseProductResponseDTO(
    @Schema(description = "Product ID", example = "3", requiredMode = REQUIRED)
    Long id,

    @Schema(description = "Product name", example = "4K Monitor", requiredMode = REQUIRED)
    String productName,

    @Schema(description = "Price paid per unit, in US dollars", example = "399.99", requiredMode = REQUIRED)
    BigDecimal unitPrice,

    @Schema(description = "Quantity bought", example = "1", requiredMode = REQUIRED)
    Integer quantity,

    @Schema(description = "Unit price times quantity, in US dollars", example = "399.99", requiredMode = REQUIRED)
    BigDecimal subtotal
) {}
