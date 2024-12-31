package com.mitienda.gestion_tienda.dtos.producto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductoResponseDTO {
    private Long id;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private LocalDateTime fechaCreacion;
    private boolean activo;
}