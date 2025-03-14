package com.mitienda.gestion_tienda.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

/**
 * Configuration class for OpenAPI documentation.
 * This class sets up the Swagger/OpenAPI documentation for the API,
 * including security schemes, API information, and common responses.
 *
 * @author Gustavo
 * @version 1.0
 */
@Configuration
@OpenAPIDefinition(
    security = {
        @SecurityRequirement(name = "basicAuth")
    }
)
@SecurityScheme(
    name = "basicAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "basic",
    in = SecuritySchemeIn.HEADER,
    description = "Basic authentication credentials"
)
public class OpenAPIConfig {

    @Value("${rate-limit.unauthenticated.capacity:10}")
    private int unauthenticatedCapacity;
    
    @Value("${rate-limit.user.capacity:30}")
    private int userCapacity;
    
    @Value("${rate-limit.admin.capacity:100}")
    private int adminCapacity;

    @Value("${rate-limit.window-minutes:1}")
    private int windowMinutes;

    /**
     * Creates and configures the OpenAPI documentation.
     *
     * @return configured OpenAPI instance
     */
    @Bean
    public OpenAPI springShopOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("G-commerce API")
                        .description("Spring Boot REST API that implements a basic e-commerce system with user authentication, product management, and purchase tracking.")
                        .version("v1.0")
                        .contact(new Contact()
                                .name("Gustavo Torres Guerrero")
                                .email("gvs7aav0@gmail.com"))
                        .license(new License().name("MIT License").url("https://opensource.org/licenses/MIT")))
                .components(new Components()
                        .responses(new ApiResponses()
                            .addApiResponse("DuplicatedEmail", createErrorApiResponse("User email already exists"))
                            .addApiResponse("UserNotFound", createErrorApiResponse("User not found"))
                            .addApiResponse("InvalidPassword", createErrorApiResponse("Invalid password"))
                            .addApiResponse("InvalidInput", createErrorApiResponse("Invalid input data"))
                            .addApiResponse("AccessDenied", createErrorApiResponse("Authentication failed"))
                            .addApiResponse("AccessDeniedUser", createErrorApiResponse("Access denied - Requires authentication with ADMIN role"))
                            .addApiResponse("AccessDeniedAdmin", createErrorApiResponse("Access denied - Requires authentication with USER role"))
                            .addApiResponse("DuplicatedProduct", createErrorApiResponse("Product already exists"))
                            .addApiResponse("ProductNotFound", createErrorApiResponse("Product not found"))
                            .addApiResponse("ConstraintError", createErrorApiResponse("Data constraint error"))
                            .addApiResponse("UnauthenticatedRateLimitExceeded", createErrorApiResponse(
                                String.format("Rate limit exceeded. Maximum allowed: %d requests per %d minutes.", unauthenticatedCapacity, windowMinutes)))
                            .addApiResponse("UserRateLimitExceeded", createErrorApiResponse(
                                String.format("Rate limit exceeded. Maximum allowed: %d requests per %d minutes.", userCapacity, windowMinutes)))
                            .addApiResponse("AdminRateLimitExceeded", createErrorApiResponse(
                                String.format("Rate limit exceeded. Maximum allowed: %d requests per %d minutes.", adminCapacity, windowMinutes)))
                ));
    }

    /**
     * Creates an API response object with the specified description.
     * 
     * This method was created initially with the intention of providing 
     * the same content description for each error response and avoid code repetition.
     * (see the commented function below)
     * Its functionality can be expanded to format uniformly the error responses.
     *
     * @param description the description of the error response
     * @return configured ApiResponse instance
     */
    private ApiResponse createErrorApiResponse(String description) {
        return new ApiResponse()
            .description(description);
    }

    /*
    private Content createApiErrorContent() {
        return new Content()
            .addMediaType("application/json", 
                new MediaType()
                    .schema(new Schema<>().$ref("#/components/schemas/ApiErrorDTO")));
    }*/
}