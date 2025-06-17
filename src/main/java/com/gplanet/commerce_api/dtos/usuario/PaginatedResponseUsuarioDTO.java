package com.gplanet.commerce_api.dtos.usuario;

import com.gplanet.commerce_api.dtos.api.PaginatedResponse;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO class for paginated user responses.
 * Extends the generic PaginatedResponse with UsuarioResponseDTO as the content type.
 * Used with OpenApi documentation to automatically obtained the schema response.
 * 
 * @author Gustavo
 * @version 1.0
 */
@Schema(name = "PaginatedResponseUsuario", description = "Paginated response containing users")
public class PaginatedResponseUsuarioDTO extends PaginatedResponse<UsuarioResponseDTO> {
}
