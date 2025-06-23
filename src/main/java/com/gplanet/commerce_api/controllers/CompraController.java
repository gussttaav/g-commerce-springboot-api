package com.gplanet.commerce_api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import com.gplanet.commerce_api.dtos.api.PaginatedResponse;
import com.gplanet.commerce_api.dtos.compra.*;
import com.gplanet.commerce_api.exceptions.ApiException;
import com.gplanet.commerce_api.exceptions.ResourceNotFoundException;
import com.gplanet.commerce_api.services.CompraService;

/**
 * REST controller for managing purchase operations in the store.
 * 
 * This controller handles all purchase-related operations including:
 * <ul>
 *   <li>Creating new purchases</li>
 *   <li>Retrieving purchase history for users</li>
 * </ul>
 * 
 * All endpoints in this controller require authentication except where noted.
 * Purchase operations are restricted to regular users only.
 * 
 * @author Gustavo
 * @version 1.0
 */
@Tag(name = "Compras", description = "API endpoints to manage user purchases")
@RestController
@RequestMapping("/api/compras")
@RequiredArgsConstructor
public class CompraController {

    private final CompraService compraService;

    /**
     * Creates a new purchase for the authenticated user.
     * Validates product availability and user's privileges before processing the purchase.
     * 
     * @param authentication the current user's authentication object
     * @param compraDTO the purchase details including products and quantities
     * @return CompraResponseDTO containing the created purchase information
     * @throws UsernameNotFoundException if the user is not found
     * @throws ResourceNotFoundException if any of the products don't exist
     * @throws ApiException if the purchase could not be added due to a constraint violation
     */
    @Operation(summary = "Create a new purchase", 
               description = "Creates a new purchase for the authenticated user with the provided information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Purchase created successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CompraResponseDTO.class))),
        @ApiResponse(responseCode = "400", ref = "#/components/responses/InvalidInput"),
        @ApiResponse(responseCode = "401", ref = "#/components/responses/AccessDenied"),
        @ApiResponse(responseCode = "403", ref = "#/components/responses/AccessDeniedAdmin"),
        @ApiResponse(responseCode = "404", ref = "#/components/responses/ProductNotFound"),
        @ApiResponse(responseCode = "409", ref = "#/components/responses/ConstraintError"),
        @ApiResponse(responseCode = "429", ref = "#/components/responses/UserRateLimitExceeded"),
    })
    @PostMapping("/nueva")
    public CompraResponseDTO realizarCompra(
            Authentication authentication,
            @Valid @RequestBody @Parameter(description = "New purchase details", required = true) 
            CompraDTO compraDTO) {
        return compraService.realizarCompra(authentication.getName(), compraDTO);
    }


    /**
     * Retrieves the paginated purchase history for the authenticated user.
     * Regular users can only see their own purchases.
     * Administrators can see all purchases.
     * 
     * @param page The page number (zero-based)
     * @param size The page size
     * @param sort The field to sort by
     * @param direction The sort direction (ASC or DESC)
     * @param authentication the current user's authentication object
     * @return PaginatedResponse<CompraResponseDTO> containing paginated purchases
     * @throws UsernameNotFoundException if the user is not found
     */
    @Operation(summary = "Lists user purchases with pagination", 
               description = "Returns a paginated list of purchases made by the authenticated user. Administrators can see all purchases.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Purchases found successfully",
            content = @Content(mediaType = "application/json", 
            schema = @Schema(ref = "#/components/schemas/PaginatedPurchases"))),
        @ApiResponse(responseCode = "401", ref = "#/components/responses/AccessDenied"),
        @ApiResponse(responseCode = "429", ref = "#/components/responses/UserRateLimitExceeded"),
    })
    @GetMapping("/listar")
    public PaginatedResponse<CompraResponseDTO> listarCompras(
           @RequestParam(defaultValue = "0") @Schema(description = "Page number (zero-based)", example = "0") int page,
           @RequestParam(defaultValue = "10") @Schema(description = "Page size", example = "10") int size,
           @RequestParam(defaultValue = "fecha") @Schema(description = "Sort field", example = "fecha") String sort,
           @RequestParam(defaultValue = "DESC") @Schema(description = "Sort direction", example = "DESC", allowableValues = {"ASC", "DESC"}) String direction,
           Authentication authentication) {
        
        Page<CompraResponseDTO> pageResult = compraService.listarCompras(authentication.getName(), page, size, sort, direction);
        return PaginatedResponse.fromPage(pageResult);
    }
}
