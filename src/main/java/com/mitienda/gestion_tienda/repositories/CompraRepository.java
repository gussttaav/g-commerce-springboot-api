package com.mitienda.gestion_tienda.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mitienda.gestion_tienda.entities.Compra;
import com.mitienda.gestion_tienda.entities.Usuario;

/**
 * Repository interface for managing Purchase (Compra) entities in the database.
 * Provides CRUD operations and custom queries for purchase-related operations.
 * 
 * @author Gustavo
 * @version 1.0
 */
public interface CompraRepository extends JpaRepository<Compra, Long> {
    
    /**
     * Finds all purchases made by a specific user.
     * @param usuario the user whose purchases are to be retrieved
     * @return a list of purchases made by the user
     */
    List<Compra> findByUsuario(Usuario usuario);

    /**
     * Finds all purchases made between two dates.
     * @param yesterday start date-time (inclusive)
     * @param tomorrow end date-time (inclusive)
     * @return a list of purchases within the specified date range
     */
    List<Compra> findByFechaBetween(LocalDateTime yesterday, LocalDateTime tomorrow);

    /**
     * Retrieves a purchase by its ID, including its associated products.
     * Uses JOIN FETCH to avoid N+1 query problems.
     * @param id the ID of the purchase
     * @return an Optional containing the purchase with its products if found
     */
    @Query("SELECT c FROM Compra c LEFT JOIN FETCH c.productos WHERE c.id = :id")
    Optional<Compra> findByIdWithProductos(@Param("id") Long id);
    
    /**
     * Finds all purchases made by a user, including their associated products.
     * Uses JOIN FETCH to avoid N+1 query problems.
     * @param usuarioId the ID of the user
     * @return a list of purchases with their products
     */
    @Query("SELECT c FROM Compra c LEFT JOIN FETCH c.productos WHERE c.usuario.id = :usuarioId")
    List<Compra> findAllByUsuarioIdWithProductos(@Param("usuarioId") Long usuarioId);
}
