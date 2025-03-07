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

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
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
 *   <li>Product listing (users only get the active products)</li>
 *   <li>Product update (admin only)</li>
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
     * Retrieves a list of products based on their status and user's role.
     * - Users with ROLE_ADMIN can filter products by status (ACTIVE/INACTIVE/ALL)
     * - Users with ROLE_USER can only access active products
     * 
     * @param status Optional query parameter to filter products by status (ACTIVE/INACTIVE/ALL)
     * @param authentication Spring Security authentication object
     * @return List<ProductoResponseDTO> containing the filtered products
     * @throws AccessDeniedException if a non-admin user attempts to access non-active products
     */
    @Operation(
        summary = "List products", 
        description = """
            Returns a list of products filtered by status.
            - ROLE_ADMIN users can filter by ACTIVE, INACTIVE, or ALL status
            - ROLE_USER users can only access ACTIVE products
            """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Products found successfully", 
                    content = @Content(mediaType = "application/json", 
                    array = @ArraySchema(schema = @Schema(implementation = ProductoResponseDTO.class)))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/InvalidInput"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AccessDenied"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/AccessDeniedUser"),
            @ApiResponse(responseCode = "429", ref = "#/components/responses/UserRateLimitExceeded")
    })
    @GetMapping("/listar")
    public List<ProductoResponseDTO> listarProductos(
            @RequestParam(required = false, defaultValue = "ACTIVE") 
            @Schema(description = "Filter products by status (ADMIN only)", allowableValues = {"ACTIVE", "INACTIVE", "ALL"})
            ProductStatus status,
            Authentication authentication) {
        
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
        if (!isAdmin && status != ProductStatus.ACTIVE) {
            throw new AccessDeniedException("Only administrators can access non-active products");
        }
        
        return productoService.listarProductos(status);
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
            @ApiResponse(responseCode = "409", ref = "#/components/responses/DuplicatedProduct"),
            @ApiResponse(responseCode = "429", ref = "#/components/responses/AdminRateLimitExceeded")
    })
    @PostMapping("/crear")
    public ProductoResponseDTO crearProducto(
            @Valid @RequestBody @Parameter(description = "New product parameters", required = true) 
            ProductoDTO productoDTO) {
        return productoService.crearProducto(productoDTO);
    }


    /**
     * Updates an existing product in the system.
     * This operation is restricted to administrators only.
     * 
     * @param id the unique identifier of the product to update
     * @param productoDTO the updated product data
     * @return ProductoResponseDTO containing the updated product's information
     * @throws ResourceNotFoundException if the product with the given ID doesn't exist
     * @throws ApiException if the product could not be updated due to a constraint violation
     */
    @Operation(
        summary = "Update an existing product", 
        description = "Updates a product's information."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product updated successfully", 
                    content = @Content(mediaType = "application/json", 
                    schema = @Schema(implementation = ProductoResponseDTO.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/InvalidInput"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AccessDenied"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/AccessDeniedUser"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/ProductNotFound"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/DuplicatedProduct"),
            @ApiResponse(responseCode = "429", ref = "#/components/responses/AdminRateLimitExceeded")
    })
    @PutMapping("/actualizar/{id}")
    public ProductoResponseDTO actualizarProducto(
            @Parameter(name = "id", description = "Unique identifier of the product to update", 
                      required = true, 
                      example = "1",
                      schema = @Schema(type = "long", minimum = "1"))
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody 
            @Parameter(description = "Updated product parameters", required = true) 
            ProductoDTO productoDTO) {
        return productoService.actualizarProducto(id, productoDTO);
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
            @ApiResponse(responseCode = "404", ref = "#/components/responses/ProductNotFound"),
            @ApiResponse(responseCode = "429", ref = "#/components/responses/AdminRateLimitExceeded")
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
