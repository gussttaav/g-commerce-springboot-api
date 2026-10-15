package com.gplanet.commerce.api.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gplanet.commerce.api.dtos.purchase.PurchaseDTO;
import com.gplanet.commerce.api.dtos.purchase.PurchaseMapper;
import com.gplanet.commerce.api.dtos.purchase.PurchaseProductDTO;
import com.gplanet.commerce.api.dtos.purchase.PurchaseResponseDTO;
import com.gplanet.commerce.api.entities.Product;
import com.gplanet.commerce.api.entities.Purchase;
import com.gplanet.commerce.api.entities.PurchaseProduct;
import com.gplanet.commerce.api.entities.User;
import com.gplanet.commerce.api.exceptions.ResourceNotFoundException;
import com.gplanet.commerce.api.repositories.ProductRepository;
import com.gplanet.commerce.api.repositories.PurchaseRepository;
import com.gplanet.commerce.api.repositories.UserRepository;
import com.gplanet.commerce.api.utilities.DatabaseOperationHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service class that handles purchase-related operations including
 * creating new purchases and listing purchase history.
 * 
 * @author Gustavo
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseService {
    private final PurchaseMapper purchaseMapper;
    private final PurchaseRepository purchaseRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /**
     * Lists purchases based on user role with pagination support.
     * Admins see all purchases, regular users see only their own purchases.
     * 
     * @param email Email of the requesting user
     * @param page The page number (zero-based)
     * @param size The page size
     * @param sort The field to sort by
     * @param direction The sort direction (ASC or DESC)
     * @return Page of PurchaseResponseDTO containing paginated purchase information
     * @throws UsernameNotFoundException if user is not found
     */
    @Transactional(readOnly = true)
    public Page<PurchaseResponseDTO> listPurchases(String email, int page, int size, String sort, String direction) {
        log.debug("Listing purchases for user: {} with pagination - page: {}, size: {}, sort: {}, direction: {}", 
                email, page, size, sort, direction);
        
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            
        // Create Pageable object with sort direction
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        
        // Get paginated result based on user role
        Page<Purchase> purchasesPage;
        if (user.getRole() == User.Role.ADMIN) {
            purchasesPage = purchaseRepository.findAll(pageable);
        } else {
            purchasesPage = purchaseRepository.findByUser(user, pageable);
        }
        
        // Map to DTOs
        Page<PurchaseResponseDTO> result = purchasesPage.map(purchaseMapper::toPurchaseResponseDTO);
        
        log.debug("Found {} purchases on page {} of {}", 
                result.getNumberOfElements(), 
                result.getNumber() + 1,  // +1 for human-readable page number
                result.getTotalPages());
                
        return result;
    }

    /**
     * Processes a new purchase for a user, calculating totals and
     * creating all necessary purchase records.
     * 
     * @param email Email of the user making the purchase
     * @param purchaseDTO Data transfer object containing purchase information
     * @return PurchaseResponseDTO containing the created purchase information
     * @throws UsernameNotFoundException if user is not found
     * @throws ResourceNotFoundException if any product in the purchase is not found or is not active
     */
    @Transactional
    public PurchaseResponseDTO createPurchase(String email, PurchaseDTO purchaseDTO) {
        log.info("Starting new purchase for user: {}", email);
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        Purchase purchase = new Purchase();
        purchase.setUser(user);
        purchase.setDate(LocalDateTime.now());
        purchase.setTotal(BigDecimal.ZERO);

        BigDecimal total = BigDecimal.ZERO;

        for (PurchaseProductDTO item : purchaseDTO.products()) {
            // Removed products are only marked as inactive, and can no longer be bought
            Product product = productRepository.findByIdAndActiveTrue(item.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product " + item.productId() + " not found"));

            PurchaseProduct purchaseProduct = new PurchaseProduct();
            purchaseProduct.setProduct(product);
            purchaseProduct.setQuantity(item.quantity());
            purchaseProduct.setSubtotal(
                product.getPrice().multiply(BigDecimal.valueOf(item.quantity()))
            );
            
            purchase.addPurchaseProduct(purchaseProduct);
            total = total.add(purchaseProduct.getSubtotal());
        }

        purchase.setTotal(total);        
        Purchase savedPurchase = DatabaseOperationHandler.executeOperation(() -> 
            purchaseRepository.save(purchase)
        );
        log.info("Purchase completed - ID: {}, Total: {}", savedPurchase.getId(), savedPurchase.getTotal());
        return purchaseMapper.toPurchaseResponseDTO(savedPurchase);
    }
}
