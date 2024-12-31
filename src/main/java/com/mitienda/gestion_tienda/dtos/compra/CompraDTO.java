package com.mitienda.gestion_tienda.dtos.compra;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class CompraDTO {
    @NotEmpty(message = "Debe incluir al menos un producto")
    private List<CompraProductoDTO> productos;
}
