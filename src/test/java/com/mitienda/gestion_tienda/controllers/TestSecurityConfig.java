package com.mitienda.gestion_tienda.controllers;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import com.mitienda.gestion_tienda.configs.CorsProperties;
import com.mitienda.gestion_tienda.configs.SecurityConfig;

@Configuration
@Import(SecurityConfig.class)
class TestSecurityConfig {
    
    @Bean
    @Primary
    public CorsProperties corsProperties() {
        CorsProperties properties = new CorsProperties();
        properties.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
        properties.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        properties.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        properties.setTestOrigin("http://localhost:3000");
        return properties;
    }
}
