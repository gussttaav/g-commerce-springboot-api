package com.mitienda.gestion_tienda.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mitienda.gestion_tienda.entities.Producto;

/**
 * Repository interface for managing Product (Producto) entities in the database.
 * Provides CRUD operations and custom queries for product-related operations.
 * 
 * @author Gustavo
 * @version 1.0
 */
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    
    /**
     * Finds all active products in the system.
     * @return a list of products where the 'activo' flag is true
     */
    List<Producto> findByActivoTrue();
}
