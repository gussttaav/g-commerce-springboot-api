package com.gplanet.commerce.api.dtos.producto;

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
 * @param nombre The name of the product.
 * @param descripcion The detailed description of the product.
 * @param precio The price of the product.
 * @param activo Indicates if the product is active.
 */
@Schema(name = "ProductRequest", 
        description = "Product data for creation and updates")
public record ProductoDTO(
    @Schema(description = "Product name", example = "Laptop", required = true)
    @NotBlank(message = "El nombre es obligatorio")
    String nombre,

    @Schema(description = "Product description", example = "High performance laptop")
    String descripcion,

    @Schema(description = "Product price", example = "999.99", required = true)
    @Positive(message = "El precio debe ser mayor a 0")
    BigDecimal precio,

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
