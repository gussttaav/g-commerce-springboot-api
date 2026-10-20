package com.gplanet.commerce.api.dtos.product;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

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
@Schema(name = "Product", description = "A product of the catalog")
public record ProductResponseDTO(
    @Schema(description = "Product ID", example = "3", requiredMode = REQUIRED)
    Long id,

    @Schema(description = "Product name, unique in the catalog", example = "4K Monitor", requiredMode = REQUIRED)
    String name,

    @Schema(description = "Product description", example = "27-inch 4K Ultra HD Monitor")
    String description,

    @Schema(description = "Price in US dollars", example = "399.99", requiredMode = REQUIRED)
    BigDecimal price,

    @Schema(description = "When the product was created", example = "2026-10-01T09:30:00", requiredMode = REQUIRED)
    LocalDateTime creationDate,

    @Schema(description = "Whether the product is in the public catalog and can be bought", example = "true",
            requiredMode = REQUIRED)
    boolean active
) {}
