package com.mitienda.gestion_tienda.configs;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.Setter;

/**
 * Configuration properties for CORS (Cross-Origin Resource Sharing) settings.
 * This class manages the CORS configuration for the application.
 * 
 * @author Gustavo
 * @version 1.0
 */
@ConfigurationProperties(prefix = "cors")
@Component
@Getter
@Setter
public class CorsProperties {
    /** 
     * List of allowed origins for CORS requests 
    */
    private List<String> allowedOrigins;
    
    /** 
     * List of allowed HTTP methods 
     */
    private List<String> allowedMethods;
    
    /** 
     * List of allowed HTTP headers 
     */
    private List<String> allowedHeaders;
    
    /** 
     * Origin used for testing purposes 
     */
    private String testOrigin;
}
