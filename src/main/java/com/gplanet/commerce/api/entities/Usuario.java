package com.gplanet.commerce.api.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Entity class representing a user in the system.
 * This class stores user information including credentials and role.
 * 
 * @author Gustavo
 * @version 1.0
 */
@Data
@Entity
@Table(name = "usuario")
public class Usuario {
    /** 
     * Unique identifier for the user. 
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /** 
     * User's full name. 
     */
    private String nombre;
    
    /** 
     * User's email address (unique). 
     */
    @Column(unique = true)
    private String email;
    
    /** 
     * User's encrypted password. 
     */
    private String password;
    
    /** 
     * User's role in the system. 
     * */
    @Enumerated(EnumType.STRING)
    private Role rol;
    
    /** 
     * Timestamp when the user was created. 
     */
    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;
    
    /**
     * Enum representing possible user roles in the system.
     */
    public enum Role {
        /** 
         * Administrator role with full privileges.
        */
        ADMIN, 
        /** 
         * Regular user role with limited privileges.
         */
        USER
    }
}
