package com.gplanet.commerce_api.utilities;

/**
 * Represents a database error with a user-friendly title and detail message.
 * Used to encapsulate database exceptions into meaningful messages.
 * 
 * @author Gustavo
 * @version 1.0
 */
@lombok.Data
@lombok.AllArgsConstructor
class DatabaseError {
    /**
     * Brief description of the error type
     */
    private String title;
    
    /**
     * Detailed explanation of what went wrong
     */
    private String detail;
}