package com.gplanet.commerce_api.dtos.compra;

import com.gplanet.commerce_api.dtos.api.PaginatedResponse;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO class for paginated purchase responses.
 * Extends the generic PaginatedResponse with CompraResponseDTO as the content type.
 * Used with OpenApi documentation to automatically obtained the schema response.
 * 
 * @author Gustavo
 * @version 1.0
 */
@Schema(name = "PaginatedResponseCompra", description = "Paginated response containing purchases")
public class PaginatedResponseCompraDTO extends PaginatedResponse<CompraResponseDTO> {
}
