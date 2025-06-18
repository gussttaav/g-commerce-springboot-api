package com.gplanet.commerce_api.dtos.compra;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object (DTO) for purchase response information.
 * This record represents the complete purchase information including
 * customer details, purchase date, total amount, and the list of
 * purchased products.
 *
 * @author Gustavo
 * @version 1.0
 */
@Schema(name = "Purchase", 
        description = "Logged in user's purchase details")
public record CompraResponseDTO(
    /**
     * The unique identifier of the purchase.
     */
    @Schema(description = "Purchase ID", example = "1")
    Long id,

    /**
     * The name of the customer who made the purchase.
     */
    @Schema(description = "Customer name", example = "John Doe")
    String usuarioNombre,

    /**
     * The date and time when the purchase was made.
     */
    @Schema(description = "Purchase date and time")
    LocalDateTime fecha,

    /**
     * The total amount of the purchase.
     */
    @Schema(description = "Total purchase amount", example = "99.99")
    BigDecimal total,

    /**
     * The list of products included in this purchase.
     */
    @Schema(description = "List of purchased products")
    List<CompraProductoResponseDTO> productos
) {}
