package com.mitienda.gestion_tienda.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class CompraResponseDTO {
    private Long id;
    private String usuarioNombre;
    private LocalDateTime fecha;
    private BigDecimal total;
    private List<CompraProductoResponseDTO> productos;
}
