package com.gplanet.commerce.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Spring Boot Commerce API application.
 */
@SpringBootApplication
public class CommerceApiApplication {

    /**
     * Starts the Spring Boot application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(CommerceApiApplication.class, args);
    }

}
