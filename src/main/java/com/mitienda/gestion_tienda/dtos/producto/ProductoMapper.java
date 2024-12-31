
package com.mitienda.gestion_tienda.dtos.producto;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.mitienda.gestion_tienda.entities.Producto;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductoMapper {
    ProductoResponseDTO toProductoResponseDTO(Producto producto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    Producto toProducto(ProductoDTO productoDTO);
}