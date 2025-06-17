package com.gplanet.commerce_api.entities;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

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
public class Compra {
    /** 
     * Unique identifier for the purchase 
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /** 
     * User who made the purchase 
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
    
    /** 
     * Date and time when the purchase was made 
     */
    private LocalDateTime fecha;
    
    /** 
     * Total amount of the purchase 
     */
    private BigDecimal total;
    
    /** 
     * List of products included in this purchase 
     */
    @JsonManagedReference
    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CompraProducto> productos = new ArrayList<>();

    /**
     * Adds a product to the purchase and maintains the bidirectional relationship
     * @param compraProducto The product entry to add to the purchase
     */
    public void addCompraProducto(CompraProducto compraProducto) {
        productos.add(compraProducto);
        compraProducto.setCompra(this);
    }
}
