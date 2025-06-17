package com.gplanet.commerce_api.entities;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entity class representing a product in the system.
 * Contains product information such as name, description, price, and status.
 * 
 * @author Gustavo
 * @version 1.0
 */
@Data
@Entity
@Table(name = "productos")
public class Producto {
    /** 
     * Unique identifier for the product 
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /** 
     * Product name (unique) 
     */
    @Column(unique = true)
    private String nombre;

    /** 
     * Product description 
     */
    private String descripcion;
    
    /** 
     * Product price 
     */
    private BigDecimal precio;
    
    /** 
     * Date when the product was created 
     */
    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;
    
    /** 
     * Indicates if the product is currently active 
     */
    private boolean activo;
}
