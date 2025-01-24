package com.mitienda.gestion_tienda.dtos.compra;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * Data Transfer Object (DTO) for purchase response information.
 * This class represents the complete purchase information including
 * customer details, purchase date, total amount, and the list of
 * purchased products.
 *
 * @author Gustavo
 * @version 1.0
 */
@Schema(name = "Purchase", 
        description = "Logged in user's purchase details")
@Data
public class CompraResponseDTO {
    /**
     * The unique identifier of the purchase.
     */
    @Schema(description = "Purchase ID", example = "1")
    private Long id;

    /**
     * The name of the customer who made the purchase.
     */
    @Schema(description = "Customer name", example = "John Doe")
    private String usuarioNombre;

    /**
     * The date and time when the purchase was made.
     */
    @Schema(description = "Purchase date and time")
    private LocalDateTime fecha;

    /**
     * The total amount of the purchase.
     */
    @Schema(description = "Total purchase amount", example = "99.99")
    private BigDecimal total;

    /**
     * The list of products included in this purchase.
     */
    @Schema(description = "List of purchased products")
    private List<CompraProductoResponseDTO> productos;
}
