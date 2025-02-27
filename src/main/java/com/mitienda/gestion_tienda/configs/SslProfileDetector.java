package com.mitienda.gestion_tienda.configs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class SslProfileDetector implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Resource keystoreResource = new ClassPathResource("keystore.p12");

        if (keystoreResource.exists()) {
            System.out.println("SSL certificate found, enabling HTTPS profile");
            environment.setActiveProfiles("https");
        } else {
            System.out.println("No SSL certificate found, using HTTP profile");
            environment.setActiveProfiles("http");
        }
    }
}
