package com.mitienda.gestion_tienda.dtos;

import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CompraProductoDTO {
    @Positive(message = "El ID del producto debe ser válido")
    private Long productoId;
    
    @Positive(message = "La cantidad debe ser mayor a 0")
    private Integer cantidad;
}
