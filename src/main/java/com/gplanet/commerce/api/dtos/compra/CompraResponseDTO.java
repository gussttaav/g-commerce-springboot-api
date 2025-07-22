package com.gplanet.commerce.api.dtos.compra;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data Transfer Object (DTO) for purchase response information.
 * This record represents the complete purchase information including
 * customer details, purchase date, total amount, and the list of
 * purchased products.
 *
 * @author Gustavo
 * @version 1.0
 * @param id The unique identifier of the purchase.
 * @param usuarioNombre The name of the customer who made the purchase.
 * @param fecha The date and time when the purchase was made.
 * @param total The total amount of the purchase.
 * @param productos The list of products included in this purchase.
 */
@Schema(name = "Purchase", 
        description = "Logged in user's purchase details")
public record CompraResponseDTO(
    @Schema(description = "Purchase ID", example = "1")
    Long id,

    @Schema(description = "Customer name", example = "John Doe")
    String usuarioNombre,

    @Schema(description = "Purchase date and time")
    LocalDateTime fecha,

    @Schema(description = "Total purchase amount", example = "99.99")
    BigDecimal total,

    @Schema(description = "List of purchased products")
    List<CompraProductoResponseDTO> productos
) {}
