package com.gplanet.commerce.api.dtos.purchase;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Data Transfer Object (DTO) for purchase requests.
 * This record represents the structure of data received when a client
 * submits a new purchase order, containing a list of products to be purchased.
 *
 * @author Gustavo
 * @version 1.0
 * @param products The list of products to be purchased.
 */
@Schema(name = "PurchaseRequest", 
        description = "Purchase request information")
public record PurchaseDTO(
    @Schema(description = "List of products to purchase")
    @NotEmpty(message = "The purchase must have at least one producto")
    @Valid
    List<@NotNull(message = "The products of the purchase cannot be null") PurchaseProductDTO> products
) {}
