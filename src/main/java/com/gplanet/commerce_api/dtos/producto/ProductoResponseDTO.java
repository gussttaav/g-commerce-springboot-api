package com.gplanet.commerce_api.dtos.producto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object (DTO) for product responses in the API.
 * This record represents the product data that is sent back to clients
 * in API responses, containing all relevant product information.
 *
 * @author Gustavo
 * @version 1.0
 */
@Schema(name = "Product", description = "Product response data transfer object")
public record ProductoResponseDTO(
    /**
     * The unique identifier of the product.
     */
    @Schema(description = "Product unique identifier", example = "1")
    Long id,

    /**
     * The name of the product.
     */
    @Schema(description = "Product name", example = "Laptop")
    String nombre,

    /**
     * The detailed description of the product.
     */
    @Schema(description = "Product description", example = "High performance laptop")
    String descripcion,

    /**
     * The price of the product.
     */
    @Schema(description = "Product price", example = "999.99")
    BigDecimal precio,

    /**
     * The timestamp when the product was created.
     */
    @Schema(description = "Product creation date")
    LocalDateTime fechaCreacion,

    /**
     * Indicates whether the product is currently active in the system.
     */
    @Schema(description = "Is an active product", example = "true")
    boolean activo
) {}