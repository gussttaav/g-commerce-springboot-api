package com.mitienda.gestion_tienda.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import com.mitienda.gestion_tienda.dtos.compra.*;
import com.mitienda.gestion_tienda.exceptions.ApiException;
import com.mitienda.gestion_tienda.exceptions.ResourceNotFoundException;
import com.mitienda.gestion_tienda.services.CompraService;

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
@Tag(name = "Compras", description = "API para gestionar las compras de la tienda")
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
        @ApiResponse(responseCode = "409", ref = "#/components/responses/ConstraintError")
    })
    @PostMapping("/nueva")
    public CompraResponseDTO realizarCompra(
            Authentication authentication,
            @Valid @RequestBody @Parameter(description = "New purchase details", required = true) 
            CompraDTO compraDTO) {
        return compraService.realizarCompra(authentication.getName(), compraDTO);
    }


    /**
     * Retrieves the purchase history for the authenticated user.
     * Regular users can only see their own purchases.
     * 
     * @param authentication the current user's authentication object
     * @return List<CompraResponseDTO> containing all purchases made by the user
     * @throws UsernameNotFoundException if the user is not found
     */
    @Operation(summary = "Lists all user purchases", 
               description = "Returns a list of all purchases made by the authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Purchases found successfully",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = CompraResponseDTO.class)))),
        @ApiResponse(responseCode = "401", ref = "#/components/responses/AccessDenied"),
    })
    @GetMapping("/listar")
    public List<CompraResponseDTO> listarCompras(
           Authentication authentication) {
        return compraService.listarCompras(authentication.getName());
    }
}
