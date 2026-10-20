package com.gplanet.commerce.api.dtos.product;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

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
@Schema(name = "ProductRequest", description = "The data of a product to create or update")
public record ProductDTO(
    @Schema(description = "Product name, unique in the catalog", example = "Espresso Machine", requiredMode = REQUIRED)
    @NotBlank(message = "The name of the product is mandatory")
    String name,

    @Schema(description = "Product description", example = "15-bar espresso machine with milk frother")
    String description,

    @Schema(description = "Price in US dollars, greater than 0", example = "249.99", requiredMode = REQUIRED)
    @Positive(message = "The price of the product must be greater than 0")
    BigDecimal price,

    @Schema(description = "Whether the product is in the public catalog and can be bought", example = "true",
            defaultValue = "true")
    boolean active
) {
    /**
     * Creates a ProductDTO with default active status.
     */
    public ProductDTO(String name, String description, BigDecimal price) {
        this(name, description, price, true);
    }
}
