package com.gplanet.commerce.api.dtos.compra;

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
 * @param productoNombre The name of the purchased product.
 * @param precioUnitario The unit price of the product at the time of purchase.
 * @param cantidad The quantity of the product purchased.
 * @param subtotal The subtotal for this product (unit price * quantity).
 */
@Schema(name = "PurchasedProduct", 
        description = "Purchased product details")
public record CompraProductoResponseDTO(
    @Schema(description = "Product ID", example = "1")
    Long id,

    @Schema(description = "Product name", example = "Product A")
    String productoNombre,

    @Schema(description = "Unit price", example = "19.99")
    BigDecimal precioUnitario,

    @Schema(description = "Quantity purchased", example = "2")
    Integer cantidad,

    @Schema(description = "Subtotal for this product", example = "39.98")
    BigDecimal subtotal
) {}
