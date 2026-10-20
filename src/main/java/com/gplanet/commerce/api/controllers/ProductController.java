package com.gplanet.commerce.api.controllers;

import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gplanet.commerce.api.configs.app.docs.ApiDocs;
import com.gplanet.commerce.api.dtos.api.PaginatedResponse;
import com.gplanet.commerce.api.dtos.product.ProductDTO;
import com.gplanet.commerce.api.dtos.product.ProductResponseDTO;
import com.gplanet.commerce.api.dtos.product.ProductStatus;
import com.gplanet.commerce.api.exceptions.ApiException;
import com.gplanet.commerce.api.exceptions.ResourceNotFoundException;
import com.gplanet.commerce.api.exceptions.UnauthorizedOperationException;
import com.gplanet.commerce.api.services.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;

/**
 * REST Controller for managing product operations.
 *
 * <p>This controller provides endpoints for:
 * <ul>
 *   <li>Product creation (admin only)</li>
 *   <li>Product listing and search (users only get the active products)</li>
 *   <li>Product update (admin only)</li>
 *   <li>Product deletion (admin only)</li>
 * </ul>
 * </p>
 * 
 * <p>Administrative operations require proper authentication and authorization.</p>
 *
 * @author Gustavo
 * @version 1.0
 */
@Tag(name = ApiDocs.TAG_PRODUCTS)
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Validated
public class ProductController {

    private static final String SORT_FIELDS = "name|price";

    private final ProductService productService;

    /**
     * Retrieves a paginated list of products based on their status, search criteria, and authentication.
     *
     * <p>
     * - Unauthenticated users can only access products with ACTIVE status
     * - Users with ROLE_USER can only access products with ACTIVE status
     * - Users with ROLE_ADMIN can filter products by status (ACTIVE/INACTIVE/ALL)
     * - Optional search parameter filters products by matching text in name or description fields
     * </p>
     *
     * @param status Optional query parameter to filter products by status (ACTIVE/INACTIVE/ALL)
     * @param searchText Optional query parameter to search within product name and description
     * @param page The page number (zero-based)
     * @param size The page size, at most {@value PaginatedResponse#MAX_PAGE_SIZE}
     * @param sort The field to sort by: name or price
     * @param direction The sort direction (ASC or DESC)
     * @param authentication Spring Security authentication object, may be null for unauthenticated requests
     * @return {@code PaginatedResponse<ProductResponseDTO>} containing the filtered and searched paginated products
     * @throws AccessDeniedException if a non-admin user attempts to access non-active products
     */
    // OpenAPIConfig documents the access token of this endpoint as optional
    @Operation(operationId = "listProducts", summary = "List products",
               description = "Returns a page of the catalog, optionally filtered by a text found in the name or "
                           + "the description. Anyone can list the active products; only administrators can list "
                           + "the inactive ones with `status=INACTIVE` or `status=ALL`.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "A page of products",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = ApiDocs.PAGINATED_PRODUCTS))),
        @ApiResponse(responseCode = "400", ref = ApiDocs.INVALID_INPUT),
        @ApiResponse(responseCode = "401", ref = ApiDocs.AUTHENTICATION_REQUIRED),
        @ApiResponse(responseCode = "403", ref = ApiDocs.PRODUCT_STATUS_FORBIDDEN),
        @ApiResponse(responseCode = "429", ref = ApiDocs.RATE_LIMIT_EXCEEDED)
    })
    @GetMapping
    public PaginatedResponse<ProductResponseDTO> listProducts(
            @Parameter(description = "Which products to list: `ACTIVE`, or `INACTIVE` and `ALL` for administrators")
            @RequestParam(required = false, defaultValue = "ACTIVE") ProductStatus status,
            @Parameter(description = "Text to find in the name or the description, ignoring case",
                example = "monitor")
            @RequestParam(required = false) String searchText,
            @Parameter(description = "Page number, starting at 0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Products per page")
            @RequestParam(defaultValue = "10") @Min(1) @Max(PaginatedResponse.MAX_PAGE_SIZE) int size,
            @Parameter(description = "Field to sort by")
            @Schema(allowableValues = {"name", "price"})
            @RequestParam(defaultValue = "name")
            @Pattern(regexp = SORT_FIELDS, message = "must be one of " + SORT_FIELDS) String sort,
            @Parameter(description = "Sort direction")
            @Schema(allowableValues = {"ASC", "DESC"})
            @RequestParam(defaultValue = "ASC") String direction,
            @Parameter(hidden = true) Authentication authentication) {
    
        // Allow unauthenticated access for ACTIVE products
        if (authentication == null) {
            if (status != ProductStatus.ACTIVE) {
                throw new AccessDeniedException("Unauthenticated users can only access active products");
            }
            Page<ProductResponseDTO> pageResult = productService.listProducts(
                ProductStatus.ACTIVE, searchText, page, size, sort, direction
            );
            return PaginatedResponse.fromPage(pageResult);
        }
    
        // For authenticated users
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    
        if (!isAdmin && status != ProductStatus.ACTIVE) {
            throw new UnauthorizedOperationException("Only administrators can access non-active products");
        }
    
        Page<ProductResponseDTO> pageResult = 
            productService.listProducts(status, searchText, page, size, sort, direction);
        return PaginatedResponse.fromPage(pageResult);
    }


    /**
     * Creates a new product in the system.
     *
     * <p>This operation is restricted to administrators only.</p>
     *
     * @param productDTO the product data containing name, price, and other details
     * @return ProductResponseDTO containing the created product's information
     * @throws ApiException if the product could not be saved due to a constraint violation
     */
    @Operation(operationId = "createProduct", summary = "Create a product",
               description = "Adds a product to the catalog. Requires the `ADMIN` role.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "The new product"),
        @ApiResponse(responseCode = "400", ref = ApiDocs.INVALID_INPUT),
        @ApiResponse(responseCode = "401", ref = ApiDocs.AUTHENTICATION_REQUIRED),
        @ApiResponse(responseCode = "403", ref = ApiDocs.ADMIN_ROLE_REQUIRED),
        @ApiResponse(responseCode = "409", ref = ApiDocs.DUPLICATED_PRODUCT),
        @ApiResponse(responseCode = "429", ref = ApiDocs.RATE_LIMIT_EXCEEDED)
    })
    @PostMapping
    public ProductResponseDTO createProduct(@Valid @RequestBody ProductDTO productDTO) {
        return productService.createProduct(productDTO);
    }


    /**
     * Updates an existing product in the system.
     *
     * <p>This operation is restricted to administrators only.</p>
     *
     * @param id the unique identifier of the product to update
     * @param productDTO the updated product data
     * @return ProductResponseDTO containing the updated product's information
     * @throws ResourceNotFoundException if the product with the given ID doesn't exist
     * @throws ApiException if the product could not be updated due to a constraint violation
     */
    @Operation(operationId = "updateProduct", summary = "Update a product",
               description = "Sets the name, description, price and status of a product. Setting `active` to "
                           + "`true` restores a deactivated product. Requires the `ADMIN` role.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "The updated product"),
        @ApiResponse(responseCode = "400", ref = ApiDocs.INVALID_INPUT),
        @ApiResponse(responseCode = "401", ref = ApiDocs.AUTHENTICATION_REQUIRED),
        @ApiResponse(responseCode = "403", ref = ApiDocs.ADMIN_ROLE_REQUIRED),
        @ApiResponse(responseCode = "404", ref = ApiDocs.PRODUCT_NOT_FOUND),
        @ApiResponse(responseCode = "409", ref = ApiDocs.DUPLICATED_PRODUCT),
        @ApiResponse(responseCode = "429", ref = ApiDocs.RATE_LIMIT_EXCEEDED)
    })
    @PutMapping("/{id}")
    public ProductResponseDTO updateProduct(
            @Parameter(description = "ID of the product", example = "3")
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody ProductDTO productDTO) {
        return productService.updateProduct(id, productDTO);
    }


    /**
     * Deletes a product from the system by its ID.
     *
     * <p>This operation is restricted to administrators only.</p>
     *
     * @param id the unique identifier of the product to delete
     * @throws ResourceNotFoundException if the product with the given ID doesn't exist
     */
    @Operation(operationId = "deactivateProduct", summary = "Deactivate a product",
               description = "Removes a product from the public catalog by marking it inactive: it can't be "
                           + "bought anymore, but it stays in the purchase history. Restore it by updating it with "
                           + "`active` set to `true`. Requires the `ADMIN` role.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product deactivated; the response has no body"),
        @ApiResponse(responseCode = "400", ref = ApiDocs.INVALID_INPUT),
        @ApiResponse(responseCode = "401", ref = ApiDocs.AUTHENTICATION_REQUIRED),
        @ApiResponse(responseCode = "403", ref = ApiDocs.ADMIN_ROLE_REQUIRED),
        @ApiResponse(responseCode = "404", ref = ApiDocs.PRODUCT_NOT_FOUND),
        @ApiResponse(responseCode = "429", ref = ApiDocs.RATE_LIMIT_EXCEEDED)
    })
    @DeleteMapping("/{id}")
    public void removeProduct(
        @Parameter(description = "ID of the product", example = "3")
        @PathVariable @Min(1) Long id) {
        productService.removeProduct(id);
    }
}
