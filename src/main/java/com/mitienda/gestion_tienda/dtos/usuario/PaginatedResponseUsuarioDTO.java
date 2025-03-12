package com.mitienda.gestion_tienda.dtos.usuario;

import com.mitienda.gestion_tienda.dtos.api.PaginatedResponse;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PaginatedResponseUsuario", description = "Paginated response containing users")
public class PaginatedResponseUsuarioDTO extends PaginatedResponse<UsuarioResponseDTO> {
}
