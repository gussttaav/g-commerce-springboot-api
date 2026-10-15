package com.gplanet.commerce.api.dtos.product;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.gplanet.commerce.api.entities.Product;

/**
 * Mapper interface for converting between Product entities and DTOs.
 * This interface uses MapStruct to automatically generate the implementation
 * of the mapping methods between different product representations.
 *
 * @author Gustavo
 * @version 1.0
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper {
    
    /**
     * Converts a Product entity to a ProductResponseDTO.
     *
     * @param product the product entity to convert
     * @return the corresponding ProductResponseDTO
     */
    ProductResponseDTO toProductResponseDTO(Product product);

    /**
     * Converts a ProductDTO to a Product entity.
     * The id and creationDate fields are ignored during the mapping.
     *
     * @param productDTO the product DTO to convert
     * @return the corresponding Product entity
     */
    @Mapping(target = "id", ignore = true)
    Product toProduct(ProductDTO productDTO);

    /**
     * Updates an existing Product entity with data from a ProductDTO.
     * Preserves the id and creationDate fields of the existing entity.
     *
     * @param productDTO the source DTO containing updated data
     * @param product the target Product entity to update
     */
    @Mapping(target = "id", ignore = true)
    void updateProductFromDTO(ProductDTO productDTO, @MappingTarget Product product);
}
