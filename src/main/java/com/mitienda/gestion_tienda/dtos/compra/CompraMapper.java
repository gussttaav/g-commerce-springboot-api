package com.mitienda.gestion_tienda.dtos.compra;

import org.springframework.stereotype.Component;

import com.mitienda.gestion_tienda.entities.Compra;
import com.mitienda.gestion_tienda.entities.CompraProducto;

import java.util.stream.Collectors;

@Component
public class CompraMapper {
    
    public CompraResponseDTO toCompraResponseDTO(Compra entity) {
        CompraResponseDTO dto = new CompraResponseDTO();
        dto.setId(entity.getId());
        dto.setUsuarioNombre(entity.getUsuario().getNombre());
        dto.setFecha(entity.getFecha());
        dto.setTotal(entity.getTotal());
        dto.setProductos(entity.getProductos().stream()
            .map(this::toCompraProductoResponseDTO)
            .collect(Collectors.toList()));
        return dto;
    }

    private CompraProductoResponseDTO toCompraProductoResponseDTO(CompraProducto entity) {
        CompraProductoResponseDTO dto = new CompraProductoResponseDTO();
        dto.setId(entity.getProducto().getId());
        dto.setProductoNombre(entity.getProducto().getNombre());
        dto.setPrecioUnitario(entity.getProducto().getPrecio());
        dto.setCantidad(entity.getCantidad());
        dto.setSubtotal(entity.getSubtotal());
        return dto;
    }
}
