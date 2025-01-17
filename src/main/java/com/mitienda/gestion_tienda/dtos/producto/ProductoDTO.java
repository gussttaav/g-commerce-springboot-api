package com.mitienda.gestion_tienda.dtos.producto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@Builder
public class ProductoDTO {
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    
    private String descripcion;
    
    @Positive(message = "El precio debe ser mayor a 0")
    private BigDecimal precio;
    
    @Builder.Default
    private boolean activo = true;
}
