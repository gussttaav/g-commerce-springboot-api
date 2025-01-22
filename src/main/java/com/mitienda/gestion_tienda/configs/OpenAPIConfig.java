package com.mitienda.gestion_tienda.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@Configuration
@OpenAPIDefinition(
    security = {
        @SecurityRequirement(name = "basicAuth")
    }
)
@SecurityScheme(
    name = "basicAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "basic"
)
public class OpenAPIConfig {
    @Bean
    public OpenAPI springShopOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("E-commerce API")
                        .description("Spring Boot REST API that implements a basic e-commerce system with user authentication, product management, and purchase tracking.")
                        .version("v1.0")
                        .contact(new Contact()
                                .name("Gustavo Torres Guerrero")
                                .email("gvs7aav0@gmail.com"))
                        .license(new License().name("MIT License").url("https://opensource.org/licenses/MIT")));
    }
}