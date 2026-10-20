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

import com.gplanet.commerce.api.configs.app.docs.ApiDocs;
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
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
@Tag(name = ApiDocs.TAG_PURCHASES)
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
     * @return PurchaseResponseDTO containing the created purchase information
     * @throws UsernameNotFoundException if the user is not found
     * @throws ResourceNotFoundException if any of the products don't exist or are not active
     * @throws ApiException if the purchase could not be added due to a constraint violation
     */
    @Operation(operationId = "createPurchase", summary = "Create a purchase",
               description = "Buys the given products for the signed-in user at their current prices. Requires "
                           + "the `USER` role: administrators can't make purchases. Inactive products can't be "
                           + "bought and are reported as not found.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "The new purchase"),
        @ApiResponse(responseCode = "400", ref = ApiDocs.INVALID_INPUT),
        @ApiResponse(responseCode = "401", ref = ApiDocs.AUTHENTICATION_REQUIRED),
        @ApiResponse(responseCode = "403", ref = ApiDocs.USER_ROLE_REQUIRED),
        @ApiResponse(responseCode = "404", ref = ApiDocs.PURCHASE_PRODUCT_NOT_FOUND),
        @ApiResponse(responseCode = "429", ref = ApiDocs.RATE_LIMIT_EXCEEDED)
    })
    @PostMapping
    public PurchaseResponseDTO createPurchase(
            @Parameter(hidden = true) Authentication authentication,
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                schema = @Schema(implementation = PurchaseDTO.class),
                examples = @ExampleObject(name = "twoProducts", summary = "Two products",
                    value = "{\"products\": [{\"productId\": 3, \"quantity\": 1}, "
                        + "{\"productId\": 2, \"quantity\": 2}]}")))
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
     * @return {@code PaginatedResponse<PurchaseResponseDTO>} containing paginated purchases
     * @throws UsernameNotFoundException if the user is not found
     */
    @Operation(operationId = "listPurchases", summary = "List purchases",
               description = "Returns a page of the signed-in user's purchases, newest first by default. "
                           + "Administrators get the purchases of all the users.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "A page of purchases",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = ApiDocs.PAGINATED_PURCHASES))),
        @ApiResponse(responseCode = "400", ref = ApiDocs.INVALID_INPUT),
        @ApiResponse(responseCode = "401", ref = ApiDocs.AUTHENTICATION_REQUIRED),
        @ApiResponse(responseCode = "429", ref = ApiDocs.RATE_LIMIT_EXCEEDED)
    })
    @GetMapping
    public PaginatedResponse<PurchaseResponseDTO> listPurchases(
           @Parameter(description = "Page number, starting at 0")
           @RequestParam(defaultValue = "0") @Min(0) int page,
           @Parameter(description = "Purchases per page")
           @RequestParam(defaultValue = "10") @Min(1) @Max(PaginatedResponse.MAX_PAGE_SIZE) int size,
           @Parameter(description = "Field to sort by")
           @Schema(allowableValues = {"date", "total"})
           @RequestParam(defaultValue = "date")
           @Pattern(regexp = SORT_FIELDS, message = "must be one of " + SORT_FIELDS) String sort,
           @Parameter(description = "Sort direction")
           @Schema(allowableValues = {"ASC", "DESC"})
           @RequestParam(defaultValue = "DESC") String direction,
           @Parameter(hidden = true) Authentication authentication) {
        Page<PurchaseResponseDTO> pageResult = purchaseService.listPurchases(
            AuthenticatedUser.idOf(authentication), page, size, sort, direction);
        return PaginatedResponse.fromPage(pageResult);
    }
}
