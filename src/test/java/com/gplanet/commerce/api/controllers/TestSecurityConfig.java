package com.gplanet.commerce.api.controllers;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import com.gplanet.commerce.api.configs.app.props.CorsProperties;
import com.gplanet.commerce.api.configs.security.SecurityConfig;

@Configuration
@Import(SecurityConfig.class)
public class TestSecurityConfig {
    
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
