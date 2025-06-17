package com.gplanet.commerce_api.configs.security;

import java.io.File;
import java.io.IOException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ClassPathResource;

/**
 * This class detects the presence of an SSL certificate and configures the appropriate profile.
 * Implements EnvironmentPostProcessor to process the environment before application startup.
 * 
 * The SSL certificate can be provided in the resource folder in development mode,
 * or in the /certs directory in a Docker container.
 * 
 * @author Gustavo
 * @version 1.0
 */
public class SslProfileDetector implements EnvironmentPostProcessor {

    private static final String FILESYSTEM_KEYSTORE_PATH = "/certs/keystore.p12"; // For Docker
    private static final String CLASSPATH_KEYSTORE_PATH = "keystore.p12"; // For Development

    /**
     * Processes the environment to detect the presence of an SSL certificate and
     * configures the corresponding profile (HTTP or HTTPS).
     * 
     * @param environment the Spring configurable environment
     * @param application the Spring Boot application
     */
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        boolean sslEnabled = false;

        // Check if keystore exists in filesystem (Dockerized environment)
        File keystoreFile = new File(FILESYSTEM_KEYSTORE_PATH);
        if (keystoreFile.exists()) {
            System.out.println("SSL certificate found at " + FILESYSTEM_KEYSTORE_PATH + ", enabling HTTPS profile.");
            sslEnabled = true;
        } else {
            // Check if keystore exists in classpath (Development mode)
            try {
                if (new ClassPathResource(CLASSPATH_KEYSTORE_PATH).getFile().exists()) {
                    System.out.println("SSL certificate found in classpath, enabling HTTPS profile.");
                    sslEnabled = true;
                }
            } catch (IOException e) {
                System.out.println("No SSL certificate found in classpath.");
            }
        }

        // Set the appropriate profile
        if (sslEnabled) {
            environment.setActiveProfiles("https");
        } else {
            environment.setActiveProfiles("http");
            System.out.println("No SSL certificate found, using HTTP profile.");
        }
    }
}
