package com.mitienda.gestion_tienda.dtos.compra;

import com.mitienda.gestion_tienda.dtos.api.PaginatedResponse;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PaginatedResponseCompra", description = "Paginated response containing purchases")
public class PaginatedResponseCompraDTO extends PaginatedResponse<CompraResponseDTO> {
}
