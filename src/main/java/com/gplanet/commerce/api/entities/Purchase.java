package com.gplanet.commerce.api.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity class representing a purchase transaction in the system.
 * Contains information about the buyer, purchase date, total amount, and purchased products.
 * 
 * @author Gustavo
 * @version 1.0
 */
@Getter
@Setter
@Entity
@Table(name = "compras")
public class Purchase {
    /** 
     * Unique identifier for the purchase. 
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /** 
     * User who made the purchase. 
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User user;
    
    /** 
     * Date and time when the purchase was made. 
     */
    @Column(name = "fecha")
    private LocalDateTime date;
    
    /** 
     * Total amount of the purchase. 
     */
    @Column(nullable = false)
    private BigDecimal total;
    
    /** 
     * List of products included in this purchase. 
     */
    @JsonManagedReference
    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseProduct> products = new ArrayList<>();

    /**
     * Adds a product to the purchase and maintains the bidirectional relationship.
     * 
     * @param purchaseProduct The product entry to add to the purchase
     */
    public void addPurchaseProduct(PurchaseProduct purchaseProduct) {
        products.add(purchaseProduct);
        purchaseProduct.setPurchase(this);
    }
}
