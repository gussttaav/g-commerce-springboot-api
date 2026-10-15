package com.gplanet.commerce.api.repositories;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.gplanet.commerce.api.entities.Product;

/**
 * Repository interface for managing Product entities in the database.
 * Provides CRUD operations and custom queries for product-related operations.
 * 
 * @author Gustavo
 * @version 1.0
 */
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    /**
     * Finds a product by ID if it is active.
     * @param id the ID of the product
     * @return an Optional containing the product if it exists and its 'activo' flag is true
     */
    Optional<Product> findByIdAndActiveTrue(Long id);

    /**
     * Finds all active products in the system with pagination support.
     * @param pageable pagination information
     * @return a page of products where the 'activo' flag is true
     */
    Page<Product> findByActiveTrue(Pageable pageable);

    /**
     * Finds all non-active products in the system with pagination support.
     * @param pageable pagination information
     * @return a page of products where the 'activo' flag is false
     */
    Page<Product> findByActiveFalse(Pageable pageable);

    /**
     * Finds active products containing the search term in either name or description.
     * 
     * @param searchTerm The search term to match against name or description (should include % wildcards)
     * @param pageable Pagination information
     * @return Page of Producto entities matching the criteria
     */
    @Query("SELECT p FROM Product p"
        + " WHERE p.active = true AND (LOWER(p.name) LIKE :searchTerm OR LOWER(p.description) LIKE :searchTerm)")
    Page<Product> findByActiveTrueAndSearch(String searchTerm, Pageable pageable);

    /**
     * Finds inactive products containing the search term in either name or description.
     * 
     * @param searchTerm The search term to match against name or description (should include % wildcards)
     * @param pageable Pagination information
     * @return Page of Producto entities matching the criteria
     */
    @Query("SELECT p FROM Product p"
        + " WHERE p.active = false AND (LOWER(p.name) LIKE :searchTerm OR LOWER(p.description) LIKE :searchTerm)")
    Page<Product> findByActiveFalseAndSearch(String searchTerm, Pageable pageable);

    /**
     * Finds all products (active and inactive) containing the search term in either name or description.
     * 
     * @param searchTerm The search term to match against name or description (should include % wildcards)
     * @param pageable Pagination information
     * @return Page of Producto entities matching the criteria
     */
    @Query("SELECT p FROM Product p"
        + " WHERE LOWER(p.name) LIKE :searchTerm OR LOWER(p.description) LIKE :searchTerm")
    Page<Product> findBySearch(String searchTerm, Pageable pageable);
}
