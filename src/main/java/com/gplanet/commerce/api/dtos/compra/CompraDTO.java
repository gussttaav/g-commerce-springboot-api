package com.gplanet.commerce.api.dtos.compra;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

/**
 * Data Transfer Object (DTO) for purchase requests.
 * This record represents the structure of data received when a client
 * submits a new purchase order, containing a list of products to be purchased.
 *
 * @author Gustavo
 * @version 1.0
 * @param productos The list of products to be purchased.
 */
@Schema(name = "PurchaseRequest", 
        description = "Purchase request information")
public record CompraDTO(
    @Schema(description = "List of products to purchase")
    @NotEmpty(message = "Debe incluir al menos un producto")
    @Valid
    List<CompraProductoDTO> productos
) {}
