package com.gplanet.commerce.api.dtos.product;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;


/**
 * Data Transfer Object (DTO) for creating and updating products.
 * This record defines the structure of product data received from clients
 * when creating or updating products in the system.
 *
 * @author Gustavo
 * @version 1.0
 * 
 * @param name The name of the product.
 * @param description The detailed description of the product.
 * @param price The price of the product.
 * @param active Indicates if the product is active.
 */
@Schema(name = "ProductRequest", 
        description = "Product data for creation and updates")
public record ProductDTO(
    @Schema(description = "Product name", example = "Laptop", required = true)
    @NotBlank(message = "The name of the product is mandatory")
    String name,

    @Schema(description = "Product description", example = "High performance laptop")
    String description,

    @Schema(description = "Product price", example = "999.99", required = true)
    @Positive(message = "The price of the product must be greater than 0")
    BigDecimal price,

    @Schema(description = "Product status", example = "true", defaultValue = "true")
    boolean active
) {
    /**
     * Creates a ProductoDTO with default active status.
     */
    public ProductDTO(String name, String description, BigDecimal price) {
        this(name, description, price, true);
    }
}
