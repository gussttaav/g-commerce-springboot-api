package com.mitienda.gestion_tienda.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.mitienda.gestion_tienda.dtos.producto.*;
import com.mitienda.gestion_tienda.exceptions.ApiException;
import com.mitienda.gestion_tienda.exceptions.ResourceNotFoundException;
import com.mitienda.gestion_tienda.services.ProductoService;

/**
 * REST Controller for managing product operations.
 * 
 * This controller provides endpoints for:
 * <ul>
 *   <li>Product creation (admin only)</li>
 *   <li>Product listing (authenticated)</li>
 *   <li>Product deletion (admin only)</li>
 * </ul>
 * 
 * Administrative operations require proper authentication and authorization.
 * 
 * @author Gustavo
 * @version 1.0
 */
@Tag(name = "Productos", description = "API for product management")
@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
@Validated
public class ProductoController {

    private final ProductoService productoService;

    /**
     * Retrieves a list of all active products in the system.
     * This endpoint is accessible to all authenticated users.
     * 
     * @return List<ProductoResponseDTO> containing all active products
     */
    @Operation(summary = "List all products", description = "Returns a list of all active products")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Products found successfully", 
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ProductoResponseDTO.class)))),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AccessDenied")
    })
    @GetMapping("/listar")
    public List<ProductoResponseDTO> listarProductos() {
        return productoService.listarProductos();
    }


    /**
     * Creates a new product in the system.
     * This operation is restricted to administrators only.
     * 
     * @param productoDTO the product data containing name, price, and other details
     * @return ProductoResponseDTO containing the created product's information
     * @throws ApiException if the product could not be saved due to a constraint violation
     */
    @Operation(summary = "Create a new product", description = "Creates a new product with the provided data")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product created successfully", 
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductoResponseDTO.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/InvalidInput"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AccessDenied"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/AccessDeniedUser"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/DuplicatedProduct")
    })
    @PostMapping("/crear")
    public ProductoResponseDTO crearProducto(
            @Valid @RequestBody @Parameter(description = "New product parameters", required = true) 
            ProductoDTO productoDTO) {
        return productoService.crearProducto(productoDTO);
    }


    /**
     * Deletes a product from the system by its ID.
     * This operation is restricted to administrators only.
     * 
     * @param id the unique identifier of the product to delete
     * @throws ResourceNotFoundException if the product with the given ID doesn't exist
     */
    @Operation(summary = "Delete a product", description = "Deletes a product by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/InvalidInput"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AccessDenied"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/AccessDeniedUser"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/ProductNotFound")
    })
    @DeleteMapping("/eliminar/{id}")
    public void eliminarProducto(
        @Parameter(name = "id", description = "Unique identifier of the product to delete", 
                   required = true, 
                   example = "1",
                   schema = @Schema(type = "long", minimum = "1"))
        @PathVariable @Min(1) Long id) {
        productoService.eliminarProducto(id);
    }
}
