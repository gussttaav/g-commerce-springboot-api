package com.gplanet.commerce.api.controllers;

import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gplanet.commerce.api.configs.security.AuthenticatedUser;
import com.gplanet.commerce.api.dtos.api.PaginatedResponse;
import com.gplanet.commerce.api.dtos.purchase.PurchaseDTO;
import com.gplanet.commerce.api.dtos.purchase.PurchaseResponseDTO;
import com.gplanet.commerce.api.exceptions.ApiException;
import com.gplanet.commerce.api.exceptions.ResourceNotFoundException;
import com.gplanet.commerce.api.services.PurchaseService;

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
 * REST controller for managing purchase operations in the store.
 * 
 *<p>This controller handles all purchase-related operations including:
 * <ul>
 *   <li>Creating new purchases</li>
 *   <li>Retrieving purchase history for users</li>
 * </ul>
 * </p>
 * 
 *<p>All endpoints in this controller require authentication except where noted.
 * Purchase operations are restricted to regular users only.</p>
 *
 * @author Gustavo
 * @version 1.0
 */
@Tag(name = "Purchases", description = "API endpoints to manage user purchases")
@RestController
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
@Validated
public class PurchaseController {

    private static final String SORT_FIELDS = "date|total";

    private final PurchaseService purchaseService;

    /**
     * Creates a new purchase for the authenticated user.
     * Validates product availability and user's privileges before processing the purchase.
     *
     * @param authentication the current user's authentication object
     * @param purchaseDTO the purchase details including products and quantities
     * @return CompraResponseDTO containing the created purchase information
     * @throws UsernameNotFoundException if the user is not found
     * @throws ResourceNotFoundException if any of the products don't exist or are not active
     * @throws ApiException if the purchase could not be added due to a constraint violation
     */
    @Operation(summary = "Create a new purchase",
               description = "Creates a new purchase for the authenticated user with the provided information. "
                           + "Products that are not active can't be bought and are reported as not found.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Purchase created successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = PurchaseResponseDTO.class))),
        @ApiResponse(responseCode = "400", ref = "#/components/responses/InvalidInput"),
        @ApiResponse(responseCode = "401", ref = "#/components/responses/AccessDenied"),
        @ApiResponse(responseCode = "403", ref = "#/components/responses/AccessDeniedAdmin"),
        @ApiResponse(responseCode = "404", ref = "#/components/responses/ProductNotFound"),
        @ApiResponse(responseCode = "409", ref = "#/components/responses/ConstraintError"),
        @ApiResponse(responseCode = "429", ref = "#/components/responses/UserRateLimitExceeded")
    })
    @PostMapping
    public PurchaseResponseDTO createPurchase(
            Authentication authentication,
            @Valid @RequestBody @Parameter(description = "New purchase details", required = true)
            PurchaseDTO purchaseDTO) {
        return purchaseService.createPurchase(AuthenticatedUser.idOf(authentication), purchaseDTO);
    }


    /**
     * Retrieves the paginated purchase history for the authenticated user.
     * Regular users can only see their own purchases.
     * Administrators can see all purchases.
     *
     * @param page The page number (zero-based)
     * @param size The page size, at most {@value PaginatedResponse#MAX_PAGE_SIZE}
     * @param sort The field to sort by: date or total
     * @param direction The sort direction (ASC or DESC)
     * @param authentication the current user's authentication object
     * @return {@code PaginatedResponse<CompraResponseDTO>} containing paginated purchases
     * @throws UsernameNotFoundException if the user is not found
     */
    @Operation(summary = "Lists user purchases with pagination",
               description = "Returns a paginated list of purchases made by the authenticated user. "
                             + "Administrators can see all purchases.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Purchases found successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/PaginatedPurchases"))),
        @ApiResponse(responseCode = "400", ref = "#/components/responses/InvalidInput"),
        @ApiResponse(responseCode = "401", ref = "#/components/responses/AccessDenied"),
        @ApiResponse(responseCode = "429", ref = "#/components/responses/UserRateLimitExceeded")
    })
    @GetMapping
    public PaginatedResponse<PurchaseResponseDTO> listPurchases(
           @RequestParam(defaultValue = "0") @Min(0)
           @Schema(description = "Page number (zero-based)", example = "0") int page,
           @RequestParam(defaultValue = "10") @Min(1) @Max(PaginatedResponse.MAX_PAGE_SIZE)
           @Schema(description = "Page size", example = "10") int size,
           @RequestParam(defaultValue = "date")
           @Pattern(regexp = SORT_FIELDS, message = "must be one of " + SORT_FIELDS)
           @Schema(description = "Sort field", example = "date", allowableValues = {"date", "total"}) String sort,
           @RequestParam(defaultValue = "DESC")
           @Schema(description = "Sort direction", example = "DESC", allowableValues = {"ASC", "DESC"}) 
           String direction,
           Authentication authentication) {
        Page<PurchaseResponseDTO> pageResult = purchaseService.listPurchases(
            AuthenticatedUser.idOf(authentication), page, size, sort, direction);
        return PaginatedResponse.fromPage(pageResult);
    }
}
