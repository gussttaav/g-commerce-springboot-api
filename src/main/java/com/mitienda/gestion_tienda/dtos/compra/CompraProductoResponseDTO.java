package com.mitienda.gestion_tienda.dtos.compra;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

/**
 * Data Transfer Object (DTO) for product purchase details.
 * This class represents detailed information about each product
 * in a purchase, including quantity, price, and subtotal calculations.
 *
 * @author Gustavo
 * @version 1.0
 */
@Schema(name = "PurchasedProduct", 
        description = "Purchased product details")
@Data
public class CompraProductoResponseDTO {
    /**
     * The unique identifier of the product.
     */
    @Schema(description = "Product ID", example = "1")
    private Long id;

    /**
     * The name of the purchased product.
     */
    @Schema(description = "Product name", example = "Product A")
    private String productoNombre;

    /**
     * The unit price of the product at the time of purchase.
     */
    @Schema(description = "Unit price", example = "19.99")
    private BigDecimal precioUnitario;

    /**
     * The quantity of the product purchased.
     */
    @Schema(description = "Quantity purchased", example = "2")
    private Integer cantidad;

    /**
     * The subtotal for this product (unit price * quantity).
     */
    @Schema(description = "Subtotal for this product", example = "39.98")
    private BigDecimal subtotal;
}