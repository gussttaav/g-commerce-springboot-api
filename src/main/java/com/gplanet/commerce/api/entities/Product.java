package com.gplanet.commerce.api.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

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
public class Product {
    /** 
     * Unique identifier for the product. 
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /** 
     * Product name (unique). 
     */
    @Column(name = "nombre", unique = true, nullable = false)
    private String name;

    /** 
     * Product description. 
     */
    @Column(name = "descripcion")
    private String description;
    
    /** 
     * Product price. 
     */
    @Column(name = "precio", nullable = false)
    private BigDecimal price;
    
    /** 
     * Date when the product was created. 
     */
    @Column(name = "fecha_creacion")
    private LocalDateTime creationDate;
    
    /** 
     * Indicates if the product is currently active. 
     */
    @Column(name = "activo")
    private boolean active;
}
