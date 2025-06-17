package com.gplanet.commerce_api.dtos.producto;

import com.gplanet.commerce_api.dtos.api.PaginatedResponse;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO class for paginated product responses.
 * Extends the generic PaginatedResponse with ProductoResponseDTO as the content type.
 * Used with OpenApi documentation to automatically obtained the schema response.
 * 
 * @author Gustavo
 * @version 1.0
 */
@Schema(name = "PaginatedResponseProducto", description = "Paginated response containing products")
public class PaginatedResponseProductoDTO extends PaginatedResponse<ProductoResponseDTO> {
}
