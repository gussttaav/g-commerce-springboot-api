package com.gplanet.commerce.api.entities;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity class representing an item in a purchase.
 * Links products with purchases and stores quantity and subtotal information.
 * 
 * @author Gustavo
 * @version 1.0
 */
@Getter
@Setter
@Entity
@Table(name = "compra_productos")
public class CompraProducto {
    /** 
     * Unique identifier for the purchase item. 
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /** 
     * Reference to the parent purchase. 
     */
    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compra_id")
    private Compra compra;
    
    /** 
     * The product that was purchased. 
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    private Producto producto;
    
    /** 
     * Quantity of the product purchased. 
     */
    private Integer cantidad;
    
    /** 
     * Subtotal for this item (price * quantity). 
     */
    private BigDecimal subtotal;
}
