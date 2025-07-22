package com.gplanet.commerce.api.dtos.producto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data Transfer Object (DTO) for product responses in the API.
 * This record represents the product data that is sent back to clients
 * in API responses, containing all relevant product information.
 *
 * @author Gustavo
 * @version 1.0
 * @param id The unique identifier of the product.
 * @param nombre The name of the product.
 * @param descripcion The detailed description of the product.
 * @param precio The price of the product.
 * @param fechaCreacion The timestamp when the product was created.
 * @param activo Indicates whether the product is currently active in the system.
 */
@Schema(name = "Product", description = "Product response data transfer object")
public record ProductoResponseDTO(
    @Schema(description = "Product unique identifier", example = "1")
    Long id,

    @Schema(description = "Product name", example = "Laptop")
    String nombre,

    @Schema(description = "Product description", example = "High performance laptop")
    String descripcion,

    @Schema(description = "Product price", example = "999.99")
    BigDecimal precio,

    @Schema(description = "Product creation date")
    LocalDateTime fechaCreacion,

    @Schema(description = "Is an active product", example = "true")
    boolean activo
) {}
