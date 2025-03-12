package com.mitienda.gestion_tienda.dtos.producto;

import com.mitienda.gestion_tienda.dtos.api.PaginatedResponse;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PaginatedResponseProducto", description = "Paginated response containing products")
public class PaginatedResponseProductoDTO extends PaginatedResponse<ProductoResponseDTO> {
}
