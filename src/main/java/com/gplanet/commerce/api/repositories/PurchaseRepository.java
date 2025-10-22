package com.gplanet.commerce.api.repositories;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gplanet.commerce.api.entities.Purchase;
import com.gplanet.commerce.api.entities.User;

/**
 * Repository interface for managing Purchase entities in the database.
 * Provides CRUD operations and custom queries for purchase-related operations.
 * 
 * @author Gustavo
 * @version 1.0
 */
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    
    /**
     * Finds all purchases made by a specific user with pagination support.
     * @param user the user whose purchases are to be retrieved
     * @param pageable pagination information
     * @return a page of purchases made by the user
     */
    Page<Purchase> findByUser(User user, Pageable pageable);

    /**
     * Finds all purchases made between two dates with pagination support.
     * @param yesterday start date-time (inclusive)
     * @param tomorrow end date-time (inclusive)
     * @param pageable pagination information
     * @return a page of purchases within the specified date range
     */
    Page<Purchase> findByDateBetween(LocalDateTime yesterday, LocalDateTime tomorrow, Pageable pageable);

    /**
     * Finds all purchases made by a user, including their associated products, with pagination support.
     * Uses JOIN FETCH to avoid N+1 query problems.
     * @param userId the ID of the user
     * @param pageable pagination information
     * @return a page of purchases with their products
     */
    @Query(value = "SELECT p FROM Purchase p LEFT JOIN FETCH p.products WHERE p.user.id = :userId",
           countQuery = "SELECT COUNT(p) FROM Purchase p WHERE p.user.id = :userId")
    Page<Purchase> findAllByUserIdWithProducts(@Param("userId") Long userId, Pageable pageable);
}
