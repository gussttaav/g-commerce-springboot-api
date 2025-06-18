package com.gplanet.commerce_api.dtos.producto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) for creating and updating products.
 * This record defines the structure of product data received from clients
 * when creating or updating products in the system.
 *
 * @author Gustavo
 * @version 1.0
 */
@Schema(name = "ProductRequest", 
        description = "Product data for creation and updates")
public record ProductoDTO(
    /**
     * The name of the product. This field is required.
     */
    @Schema(description = "Product name", example = "Laptop", required = true)
    @NotBlank(message = "El nombre es obligatorio")
    String nombre,
    
    /**
     * The detailed description of the product. Optional field.
     */
    @Schema(description = "Product description", example = "High performance laptop")
    String descripcion,
    
    /**
     * The price of the product. Must be a positive number.
     */
    @Schema(description = "Product price", example = "999.99", required = true)
    @Positive(message = "El precio debe ser mayor a 0")
    BigDecimal precio,
    
    /**
     * Indicates if the product is active. Defaults to true.
     */
    @Schema(description = "Product status", example = "true", defaultValue = "true")
    boolean activo
) {
    /**
     * Creates a ProductoDTO with default active status.
     */
    public ProductoDTO(String nombre, String descripcion, BigDecimal precio) {
        this(nombre, descripcion, precio, true);
    }
}
