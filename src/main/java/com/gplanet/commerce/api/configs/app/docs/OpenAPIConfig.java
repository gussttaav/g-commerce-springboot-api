package com.gplanet.commerce.api.configs.app.docs;

import java.util.Map;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.gplanet.commerce.api.configs.app.props.RateLimitProperties;

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
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import lombok.RequiredArgsConstructor;

/**
 * Configuration class for OpenAPI documentation.
 * <p>This class sets up the Swagger/OpenAPI documentation for the API,
 * including JWT security schemes, API information, and common responses.</p>
 *
 * <p><b>NOTE:</b> The class name 'OpenAPIConfig' intentionally uses the 
 * abbreviation 'API' with more than two uppercase letters to follow the 
 * naming conventions of the OpenAPI/Swagger ecosystem and for clarity 
 * in documentation configuration classes.</p>
 *
 * @author Gustavo
 * @version 1.0
 */
@Configuration
@OpenAPIDefinition(
    security = {
        @SecurityRequirement(name = "bearerAuth")
    }
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    in = SecuritySchemeIn.HEADER,
    description = "JWT Bearer token authentication. Use format: Bearer {your-token}"
)
@RequiredArgsConstructor
public class OpenAPIConfig {

    private static final String RATE_LIMIT_MSG =
        "Rate limit exceeded. %s are limited to bursts of %d, refilled at %d every %d minute(s).";
    private static final String INT32 = "int32";

    private final RateLimitProperties rateLimitProperties;

    /**
     * Creates and configures the OpenAPI documentation.
     *
     * @return configured OpenAPI instance
     */
    @Bean
    public OpenAPI springShopOpenApi() {
        return new OpenAPI()
                .info(new Info().title("G-commerce API")
                        .description(
                            "Spring Boot REST API that implements a basic e-commerce system with user authentication, "
                            + "product management, and purchase tracking. Features JWT-based authentication with RSA "
                            + "signatures, role-based access control, and OpenID Connect discovery endpoints."
                        )
                        .version("v1.0")
                        .contact(new Contact()
                                .name("Gustavo Torres Guerrero")
                                .email("gvs7aav0@gmail.com"))
                        .license(new License().name("MIT License")
                            .url("https://opensource.org/licenses/MIT")))
                .components(new Components()
                        .responses(new ApiResponses()
                            // User-related errors
                            .addApiResponse("DuplicatedEmail", createErrorApiResponse("User email already exists"))
                            .addApiResponse("UserNotFound", createErrorApiResponse("User not found"))
                            .addApiResponse("InvalidPassword", createErrorApiResponse("Invalid password"))
                            
                            // General validation errors
                            .addApiResponse("InvalidInput", createErrorApiResponse("Invalid input data"))
                            
                            // Authentication and authorization errors
                            .addApiResponse("AccessDenied", createErrorApiResponse(
                                "Authentication failed or access denied"))
                            .addApiResponse("AccessDeniedUser", createErrorApiResponse(
                                "Access denied - Requires authentication with ADMIN role"))
                            .addApiResponse("AccessDeniedAdmin", createErrorApiResponse(
                                "Access denied - Requires authentication with USER role"))
                            
                            // Product-related errors
                            .addApiResponse("DuplicatedProduct", createErrorApiResponse("Product already exists"))
                            .addApiResponse("ProductNotFound", createErrorApiResponse("Product not found"))
                            
                            // Data constraint errors
                            .addApiResponse("ConstraintError", createErrorApiResponse("Data constraint error"))
                            
                            // JWT Token-related errors
                            .addApiResponse("InvalidToken", createErrorApiResponse("Invalid or malformed JWT token"))
                            .addApiResponse("TokenExpired", createErrorApiResponse("JWT token has expired"))
                            .addApiResponse("TokenBlacklisted", createErrorApiResponse("Token has been invalidated"))
                            .addApiResponse("InvalidRefreshToken", createErrorApiResponse(
                                "Invalid, expired, already used or revoked refresh token"))
                            
                            // Rate limiting errors
                            .addApiResponse("UnauthenticatedRateLimitExceeded", createRateLimitApiResponse(
                                "Anonymous requests per IP address", rateLimitProperties.getUnauthenticated()))
                            .addApiResponse("UserRateLimitExceeded", createRateLimitApiResponse(
                                "Requests per user", rateLimitProperties.getUser()))
                            .addApiResponse("AdminRateLimitExceeded", createRateLimitApiResponse(
                                "Requests per admin", rateLimitProperties.getAdmin()))
                            .addApiResponse("LoginRateLimitExceeded", createRateLimitApiResponse(
                                "Login attempts per IP address", rateLimitProperties.getLogin()))
                ));
    }

    /**
     * Creates the API response of a request rejected by a rate limit.
     *
     * @param limitedRequests the requests the limit applies to
     * @param limit the rate limit
     * @return configured ApiResponse instance
     */
    private ApiResponse createRateLimitApiResponse(String limitedRequests, RateLimitProperties.Limit limit) {
        return createErrorApiResponse(String.format(RATE_LIMIT_MSG, limitedRequests, limit.getCapacity(),
            limit.getRefill(), rateLimitProperties.getWindowMinutes()));
    }

    /**
     * Creates an API response object with the specified description.
     *
     * <p>This method was created initially with the intention of providing the same 
     * content description for each error response and avoid code repetition.
     * (see the commented function below)
     * Its functionality can be expanded to format uniformly the error responses.</p>
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

    /**
     * Customizes paginated response schemas for OpenAPI documentation.
     *
     * @return OpenApiCustomizer instance
     */
    @Bean
    public OpenApiCustomizer paginatedResponseCustomizer() {
        return openApi -> customizePaginatedResponses(openApi);
    }

    private void customizePaginatedResponses(OpenAPI openApi) {
        Components components = openApi.getComponents();
        if (components == null) {
            return;
        }
        var schemas = components.getSchemas();
        if (schemas == null) {
            return;
        }
        var typeMap = Map.of(
            "Purchase", "PaginatedPurchases",
            "Product", "PaginatedProducts",
            "User", "PaginatedUsers"
        );
        typeMap.forEach((dtoSchemaName, paginatedSchemaName) -> {
            Schema<?> dtoSchema = schemas.get(dtoSchemaName);
            if (dtoSchema != null) {
                addPaginatedSchema(components, dtoSchema, paginatedSchemaName, dtoSchemaName);
            }
        });
    }

    private void addPaginatedSchema(
        Components components,
        Schema<?> dtoSchema,
        String paginatedSchemaName,
        String dtoSchemaName
    ) {
        ObjectSchema paginatedSchema = new ObjectSchema();
        paginatedSchema.setName(paginatedSchemaName);
        paginatedSchema.setDescription("Paginated response of " + dtoSchemaName.toLowerCase() + "s");

        ArraySchema contentSchema = new ArraySchema();
        contentSchema.setItems(dtoSchema);
        paginatedSchema.addProperty("content", contentSchema);

        paginatedSchema.addProperty("pageNumber", new IntegerSchema()
            .format(INT32)
            .description("Current page number (0-based)"));

        paginatedSchema.addProperty("pageSize", new IntegerSchema()
            .format(INT32)
            .description("Number of items per page"));

        paginatedSchema.addProperty("totalElements", new IntegerSchema()
            .format("int64")
            .description("Total number of elements"));

        paginatedSchema.addProperty("totalPages", new IntegerSchema()
            .format(INT32)
            .description("Total number of pages")
        );

        paginatedSchema.addProperty("isLastPage", new BooleanSchema()
            .description("Is this the last page?")
        );

        components.addSchemas(paginatedSchemaName, paginatedSchema);
    }
}
