package com.mitienda.gestion_tienda.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductoDTO {
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    
    private String descripcion;
    
    @Positive(message = "El precio debe ser mayor a 0")
    private BigDecimal precio;
    
    private boolean activo = true;
}
