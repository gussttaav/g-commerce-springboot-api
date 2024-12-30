package com.mitienda.gestion_tienda.dtos;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class CompraProductoResponseDTO {
    private Long id;
    private String productoNombre;
    private BigDecimal precioUnitario;
    private Integer cantidad;
    private BigDecimal subtotal;
}