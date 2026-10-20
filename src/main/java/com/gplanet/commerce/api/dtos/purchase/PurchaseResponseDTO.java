package com.gplanet.commerce.api.dtos.purchase;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data Transfer Object (DTO) for purchase response information.
 * This record represents the complete purchase information including
 * customer details, purchase date, total amount, and the list of
 * purchased products.
 *
 * @author Gustavo
 * @version 1.0
 * 
 * @param id The unique identifier of the purchase.
 * @param userName The name of the customer who made the purchase.
 * @param date The date and time when the purchase was made.
 * @param total The total amount of the purchase.
 * @param products The list of products included in this purchase.
 */
@Schema(name = "Purchase", description = "A purchase and the products bought")
public record PurchaseResponseDTO(
    @Schema(description = "Purchase ID", example = "12", requiredMode = REQUIRED)
    Long id,

    @Schema(description = "Name of the user who made the purchase", example = "John Doe", requiredMode = REQUIRED)
    String userName,

    @Schema(description = "When the purchase was made", example = "2026-10-20T09:30:00", requiredMode = REQUIRED)
    LocalDateTime date,

    @Schema(description = "Total amount in US dollars", example = "399.99", requiredMode = REQUIRED)
    BigDecimal total,

    @Schema(description = "The products bought", requiredMode = REQUIRED)
    List<PurchaseProductResponseDTO> products
) {}
