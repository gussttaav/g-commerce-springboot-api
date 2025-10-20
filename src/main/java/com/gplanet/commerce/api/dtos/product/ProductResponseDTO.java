package com.gplanet.commerce.api.dtos.product;

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
 * 
 * @param id The unique identifier of the product.
 * @param name The name of the product.
 * @param description The detailed description of the product.
 * @param price The price of the product.
 * @param creationDate The timestamp when the product was created.
 * @param active Indicates whether the product is currently active in the system.
 */
@Schema(name = "Product", description = "Product response data transfer object")
public record ProductResponseDTO(
    @Schema(description = "Product unique identifier", example = "1")
    Long id,

    @Schema(description = "Product name", example = "Laptop")
    String name,

    @Schema(description = "Product description", example = "High performance laptop")
    String description,

    @Schema(description = "Product price", example = "999.99")
    BigDecimal price,

    @Schema(description = "Product creation date")
    LocalDateTime creationDate,

    @Schema(description = "Is an active product", example = "true")
    boolean active
) {}
