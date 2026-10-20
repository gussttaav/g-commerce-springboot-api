package com.gplanet.commerce.api.configs.app.docs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import com.gplanet.commerce.api.configs.app.props.JwtProperties;
import com.gplanet.commerce.api.configs.app.props.RateLimitProperties;
import com.gplanet.commerce.api.dtos.api.ApiErrorDTO;
import com.gplanet.commerce.api.dtos.api.PaginatedResponse;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Configuration class for OpenAPI documentation.
 * <p>This class builds the OpenAPI document served at {@code /v3/api-docs} and published by the docs site:
 * the API description, servers, tags, security schemes, and the reusable error responses and schemas
 * that the controller annotations reference through {@link ApiDocs}.</p>
 *
 * <p>Numbers that depend on the configuration, such as token lifetimes and rate limits, are read from the
 * properties, so the document always describes the running configuration.</p>
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
@RequiredArgsConstructor
public class OpenAPIConfig {

    private static final String DESCRIPTION_RESOURCE = "openapi/description.md";
    private static final String JWT_FORMAT = "JWT";
    private static final String BEARER = "bearer";
    private static final String INT32 = "int32";
    private static final long MILLIS_PER_MINUTE = 60_000L;
    private static final long MILLIS_PER_HOUR = 3_600_000L;
    private static final int EXAMPLE_REMAINING_REQUESTS = 29;
    private static final int EXAMPLE_TOTAL_ELEMENTS = 24;

    private final RateLimitProperties rateLimitProperties;
    private final JwtProperties jwtProperties;

    @Value("${openapi.production-url}")
    private String productionUrl;

    static {
        // The API serializes LocalDateTime without a time zone offset, which the date-time format (RFC 3339)
        // requires, so it's documented as an ISO 8601 local date-time string instead
        SpringDocUtils.getConfig().replaceWithSchema(LocalDateTime.class, new StringSchema()
            .pattern("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?$"));
    }

    /**
     * Creates and configures the OpenAPI documentation.
     *
     * @return configured OpenAPI instance
     */
    @Bean
    public OpenAPI gcommerceOpenApi() {
        return new OpenAPI()
                .info(new Info().title("G-Commerce API")
                        .description(description())
                        .version("v1.0")
                        .contact(new Contact()
                                .name("Gustavo Torres Guerrero")
                                .email("gvs7aav0@gmail.com")
                                .url("https://github.com/gussttaav/g-commerce-springboot-api"))
                        .license(new License().name("MIT License")
                            .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                    new Server().url(productionUrl).description("Production (public demo)"),
                    new Server().url("http://localhost:8080").description("Local development")))
                .tags(tags())
                .addSecurityItem(new SecurityRequirement().addList(ApiDocs.BEARER_AUTH))
                .components(new Components()
                        .securitySchemes(securitySchemes())
                        .responses(new LinkedHashMap<>(ApiErrorResponses.create(rateLimitProperties)))
                        .addHeaders(ApiDocs.RATE_LIMIT_REMAINING_HEADER, new Header()
                            .description("Requests left in the caller's rate limit bucket")
                            .schema(new IntegerSchema().example(EXAMPLE_REMAINING_REQUESTS))));
    }

    /**
     * Completes the document generated from the controllers: registers the error and paginated schemas,
     * documents the rate limit header of the successful responses, marks the endpoints that accept an
     * optional access token, and sorts the paths and components so the output is stable.
     *
     * @return OpenApiCustomizer instance
     */
    @Bean
    public OpenApiCustomizer documentationCustomizer() {
        return openApi -> {
            addErrorSchema(openApi);
            addPaginatedSchemas(openApi);
            addRateLimitHeaders(openApi);
            allowAnonymousAccess(openApi, "/api/products", PathItem.HttpMethod.GET);
            allowAnonymousAccess(openApi, "/api/users", PathItem.HttpMethod.POST);
            sortForStableOutput(openApi);
        };
    }

    private String description() {
        String template;
        try {
            template = new ClassPathResource(DESCRIPTION_RESOURCE).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + DESCRIPTION_RESOURCE, e);
        }
        Map<String, Object> values = Map.ofEntries(
            Map.entry("accessTokenMinutes", jwtProperties.getAccessExpirationMs() / MILLIS_PER_MINUTE),
            Map.entry("refreshTokenHours", jwtProperties.getRefreshExpirationMs() / MILLIS_PER_HOUR),
            Map.entry("maxPageSize", PaginatedResponse.MAX_PAGE_SIZE),
            Map.entry("rateLimitWindow", describeWindow(rateLimitProperties.getWindowMinutes())),
            Map.entry("anonymousCapacity", rateLimitProperties.getUnauthenticated().getCapacity()),
            Map.entry("anonymousRefill", rateLimitProperties.getUnauthenticated().getRefill()),
            Map.entry("userCapacity", rateLimitProperties.getUser().getCapacity()),
            Map.entry("userRefill", rateLimitProperties.getUser().getRefill()),
            Map.entry("adminCapacity", rateLimitProperties.getAdmin().getCapacity()),
            Map.entry("adminRefill", rateLimitProperties.getAdmin().getRefill()),
            Map.entry("loginCapacity", rateLimitProperties.getLogin().getCapacity()),
            Map.entry("loginRefill", rateLimitProperties.getLogin().getRefill()));
        for (Map.Entry<String, Object> value : values.entrySet()) {
            template = template.replace("{{" + value.getKey() + "}}", String.valueOf(value.getValue()));
        }
        if (template.contains("{{")) {
            throw new IllegalStateException("Unresolved placeholder in " + DESCRIPTION_RESOURCE);
        }
        return template;
    }

    private static String describeWindow(int minutes) {
        if (minutes == 1) {
            return "minute";
        }
        return minutes + " minutes";
    }

    private List<Tag> tags() {
        return List.of(
            new Tag().name(ApiDocs.TAG_AUTHENTICATION).description(
                "Sign in, refresh and revoke tokens, and check whether a token is still valid."),
            new Tag().name(ApiDocs.TAG_USERS).description(
                "Register accounts and manage the signed-in user's profile. Administrators can also list "
                    + "the users and change their roles."),
            new Tag().name(ApiDocs.TAG_PRODUCTS).description(
                "Browse the catalog, which is public, and manage the products as an administrator. Removing "
                    + "a product deactivates it, so it stays in the purchase history."),
            new Tag().name(ApiDocs.TAG_PURCHASES).description(
                "Buy active products and read the purchase history. Users see their own purchases and "
                    + "administrators see all of them."),
            new Tag().name(ApiDocs.TAG_DISCOVERY).description(
                "Public signing keys and OpenID provider metadata, so other services can verify the tokens "
                    + "this API issues."));
    }

    private Map<String, SecurityScheme> securitySchemes() {
        Map<String, SecurityScheme> schemes = new LinkedHashMap<>();
        schemes.put(ApiDocs.BEARER_AUTH, new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme(BEARER)
            .bearerFormat(JWT_FORMAT)
            .description(String.format("An access token, the `token` field returned by `POST /api/auth/login` "
                    + "and `POST /api/auth/refresh`. It expires after %d minutes.",
                jwtProperties.getAccessExpirationMs() / MILLIS_PER_MINUTE)));
        schemes.put(ApiDocs.REFRESH_TOKEN_AUTH, new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme(BEARER)
            .bearerFormat(JWT_FORMAT)
            .description(String.format("A refresh token, the `refreshToken` field returned by "
                    + "`POST /api/auth/login` and `POST /api/auth/refresh`. Each one works once and expires "
                    + "after %d hours.",
                jwtProperties.getRefreshExpirationMs() / MILLIS_PER_HOUR)));
        return schemes;
    }

    // The Error schema isn't the type of any controller return value, so springdoc doesn't register it.
    // It's resolved with the OpenAPI 3.1 converters, the ones springdoc uses for the rest of the document.
    private static void addErrorSchema(OpenAPI openApi) {
        Components components = openApi.getComponents();
        ModelConverters.getInstance(true).readAll(ApiErrorDTO.class).forEach(components::addSchemas);
    }

    private static void addPaginatedSchemas(OpenAPI openApi) {
        Map<String, String> typeMap = Map.of(
            "Purchase", "PaginatedPurchases",
            "Product", "PaginatedProducts",
            "User", "PaginatedUsers"
        );
        Components components = openApi.getComponents();
        typeMap.forEach((dtoSchemaName, paginatedSchemaName) ->
            components.addSchemas(paginatedSchemaName, paginatedSchema(dtoSchemaName)));
    }

    private static Schema<?> paginatedSchema(String dtoSchemaName) {
        ObjectSchema paginatedSchema = new ObjectSchema();
        paginatedSchema.setDescription("A page of " + dtoSchemaName.toLowerCase() + "s");

        ArraySchema contentSchema = new ArraySchema();
        contentSchema.setItems(new Schema<>().$ref("#/components/schemas/" + dtoSchemaName));
        contentSchema.setDescription("The items of the page");
        paginatedSchema.addProperty("content", contentSchema);

        paginatedSchema.addProperty("pageNumber", new IntegerSchema()
            .format(INT32)
            .description("Current page number (0-based)")
            .example(0));
        paginatedSchema.addProperty("pageSize", new IntegerSchema()
            .format(INT32)
            .description("Maximum number of items per page")
            .example(10));
        paginatedSchema.addProperty("totalElements", new IntegerSchema()
            .format("int64")
            .description("Total number of items across all pages")
            .example(EXAMPLE_TOTAL_ELEMENTS));
        paginatedSchema.addProperty("totalPages", new IntegerSchema()
            .format(INT32)
            .description("Total number of pages")
            .example(3));
        paginatedSchema.addProperty("isLastPage", new BooleanSchema()
            .description("Whether this is the last page")
            .example(false));
        paginatedSchema.setRequired(List.of("content", "pageNumber", "pageSize", "totalElements",
            "totalPages", "isLastPage"));
        return paginatedSchema;
    }

    // RateLimitInterceptor adds the header to every request it lets through under /api/**
    private static void addRateLimitHeaders(OpenAPI openApi) {
        Header header = new Header().$ref("#/components/headers/" + ApiDocs.RATE_LIMIT_REMAINING_HEADER);
        openApi.getPaths().forEach((path, pathItem) -> {
            if (path.startsWith("/api/")) {
                pathItem.readOperations().forEach(operation -> operation.getResponses().forEach((code, response) -> {
                    if (code.startsWith("2")) {
                        response.addHeaderObject(ApiDocs.RATE_LIMIT_REMAINING_HEADER, header);
                    }
                }));
            }
        });
    }

    // An empty requirement means the endpoint also accepts requests without credentials
    private static void allowAnonymousAccess(OpenAPI openApi, String path, PathItem.HttpMethod method) {
        Operation operation = openApi.getPaths().get(path).readOperationsMap().get(method);
        operation.setSecurity(List.of(
            new SecurityRequirement(),
            new SecurityRequirement().addList(ApiDocs.BEARER_AUTH)));
    }

    // springdoc doesn't guarantee the order of the paths and responses, and the published copy must not change
    // between builds of the same code
    private static void sortForStableOutput(OpenAPI openApi) {
        Paths sortedPaths = new Paths();
        new TreeMap<>(openApi.getPaths()).forEach(sortedPaths::addPathItem);
        openApi.setPaths(sortedPaths);
        sortedPaths.values().forEach(pathItem -> pathItem.readOperations().forEach(operation -> {
            ApiResponses sortedResponses = new ApiResponses();
            new TreeMap<>(operation.getResponses()).forEach(sortedResponses::addApiResponse);
            operation.setResponses(sortedResponses);
        }));

        Components components = openApi.getComponents();
        components.setSchemas(new TreeMap<>(components.getSchemas()));
        components.setResponses(new TreeMap<>(components.getResponses()));
    }
}
