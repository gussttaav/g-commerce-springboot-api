package com.gplanet.commerce.api.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gplanet.commerce.api.entities.User;
import com.gplanet.commerce.api.entities.User.Role;

/**
 * Repository interface for managing User entities in the database.
 * Provides CRUD operations and custom queries for user-related operations.
 * 
 * @author Gustavo
 * @version 1.0
 */
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Finds a user by the email address.
     * @param email the email address to search for
     * @return an Optional containing the user if found
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Checks if a user with the given email exists.
     * @param email the email address to check
     * @return true if a user with the email exists, false otherwise
     */
    boolean existsByEmail(String email);
    
    /**
     * Finds all users with a specific role.
     * @param role the role to search for
     * @return a list of users with the specified role
     */
    List<User> findByRole(Role role);
}
