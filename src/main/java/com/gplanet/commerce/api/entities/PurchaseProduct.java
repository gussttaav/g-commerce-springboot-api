package com.gplanet.commerce.api.entities;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
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
public class PurchaseProduct {
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
    @JoinColumn(name = "compra_id", nullable = false)
    private Purchase purchase;
    
    /** 
     * The product that was purchased. 
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Product product;
    
    /** 
     * Quantity of the product purchased. 
     */
    @Column(name = "cantidad", nullable = false)
    private Integer quantity;
    
    /** 
     * Subtotal for this item (price * quantity). 
     */
    @Column(nullable = false)
    private BigDecimal subtotal;
}
