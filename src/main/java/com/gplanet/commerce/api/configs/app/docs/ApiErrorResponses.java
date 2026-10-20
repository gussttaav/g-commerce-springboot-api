package com.gplanet.commerce.api.configs.app.docs;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import com.gplanet.commerce.api.configs.app.props.RateLimitProperties;

import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;

/**
 * Builds the reusable error responses of the OpenAPI document.
 *
 * <p>Each response documents the {@code Error} body with examples copied from what the API actually
 * returns: the messages of the exceptions, the error names set by {@code GlobalExceptionHandler} and the
 * bodies written by the security entry point and access denied handler. Keep them in sync when a message
 * changes.</p>
 *
 * @author Gustavo
 * @version 1.0
 */
final class ApiErrorResponses {

    private static final String APPLICATION_JSON = "application/json";
    private static final String TIMESTAMP = "2026-10-20T09:30:00";
    private static final String USERS_PATH = "/api/users";
    private static final String USERS_ME_PATH = "/api/users/me";
    private static final String PRODUCTS_PATH = "/api/products";
    private static final String PURCHASES_PATH = "/api/purchases";
    private static final String REFRESH_PATH = "/api/auth/refresh";
    private static final String VALIDATE_PATH = "/api/auth/validate";
    private static final String VALIDATION_ERROR = "Validation Error";
    private static final String VALIDATION_FAILED = "Validation failed";
    private static final String ACCESS_DENIED = "Access Denied";
    private static final String INVALID_TOKEN = "Invalid Token";
    private static final String MISSING_HEADER = "Missing Authorization Header";
    private static final String HEADER_REQUIRED = "Authorization header is required";
    private static final String HEADER_MISSING_DETAIL = "The Authorization header with a Bearer token is missing";
    private static final String NOT_BEARER = "Missing or invalid Bearer token";
    private static final String UNAUTHORIZED_OPERATION = "Unauthorized operation";
    private static final String RATE_LIMIT_MESSAGE = "Rate limit exceeded";
    private static final int EXAMPLE_RETRY_AFTER_SECONDS = 30;

    private ApiErrorResponses() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Creates the error responses, keyed by the names referenced from {@link ApiDocs}.
     *
     * @param rateLimits the rate limits, described in the 429 responses
     * @return the error responses in a stable order
     */
    static Map<String, ApiResponse> create(RateLimitProperties rateLimits) {
        Map<String, ApiResponse> responses = new LinkedHashMap<>();
        addClientErrors(responses);
        addAuthenticationErrors(responses);
        addAuthorizationErrors(responses);
        addNotFoundAndConflictErrors(responses);
        addServerErrors(responses, rateLimits);
        return responses;
    }

    private static void addClientErrors(Map<String, ApiResponse> responses) {
        responses.put("InvalidInput", error(
            "The request is invalid: a body field or a query parameter failed validation, or the body is not "
                + "valid JSON. `details` lists each problem.",
            example("invalidBody", "Invalid body field", HttpStatus.BAD_REQUEST, VALIDATION_ERROR,
                VALIDATION_FAILED, USERS_PATH, "email: Email must be valid", "password: Password is required"),
            example("invalidQueryParameter", "Invalid query parameter", HttpStatus.BAD_REQUEST,
                "Constraint Violation", VALIDATION_FAILED, PRODUCTS_PATH,
                "size: must be less than or equal to 100")));
        responses.put("InvalidProfileUpdate", error(
            "The update is invalid: a field failed validation, or the password change was rejected.",
            example("invalidField", "Invalid field", HttpStatus.BAD_REQUEST, VALIDATION_ERROR, VALIDATION_FAILED,
                USERS_ME_PATH, "newPassword: New password must be at least 8 characters long"),
            example("wrongCurrentPassword", "Wrong current password", HttpStatus.BAD_REQUEST,
                apiError(HttpStatus.BAD_REQUEST), "The current password is incorrect", USERS_ME_PATH),
            example("confirmationMismatch", "New password not confirmed", HttpStatus.BAD_REQUEST,
                apiError(HttpStatus.BAD_REQUEST), "The new password and its confirmation don't match",
                USERS_ME_PATH),
            example("samePassword", "New password equals the current one", HttpStatus.BAD_REQUEST,
                VALIDATION_ERROR, "The new password must be different from the current password", USERS_ME_PATH,
                "The new password must be different from the current password")));
    }

    private static void addAuthenticationErrors(Map<String, ApiResponse> responses) {
        responses.put("AuthenticationRequired", error(
            "The request has no access token, or the token is invalid, expired or revoked.",
            example("missingToken", "No valid access token", HttpStatus.UNAUTHORIZED,
                apiError(HttpStatus.UNAUTHORIZED), "Access token required", USERS_ME_PATH)));
        responses.put("InvalidCredentials", error(
            "The email is not registered or the password is wrong. Both cases get the same answer, so it doesn't "
                + "reveal which emails are registered.",
            example("invalidCredentials", "Wrong email or password", HttpStatus.UNAUTHORIZED,
                apiError(HttpStatus.UNAUTHORIZED), "Invalid email or password", "/api/auth/login")));
        responses.put("InvalidRefreshToken", error(
            "The refresh token is missing, invalid, expired, already used or revoked by a logout.",
            example("usedToken", "Already used or revoked", HttpStatus.UNAUTHORIZED, INVALID_TOKEN,
                "Refresh token has already been used or revoked", REFRESH_PATH),
            example("invalidToken", "Invalid or expired", HttpStatus.UNAUTHORIZED, INVALID_TOKEN,
                "Invalid refresh token", REFRESH_PATH),
            example("missingHeader", "No Authorization header", HttpStatus.UNAUTHORIZED, MISSING_HEADER,
                HEADER_REQUIRED, REFRESH_PATH, HEADER_MISSING_DETAIL)));
        responses.put("BearerTokenRequired", error(
            "The `Authorization` header is missing or doesn't contain a Bearer token. An invalid token is not an "
                + "error: it is reported in a 200 response with `valid: false`.",
            example("missingHeader", "No Authorization header", HttpStatus.UNAUTHORIZED, MISSING_HEADER,
                HEADER_REQUIRED, VALIDATE_PATH, HEADER_MISSING_DETAIL),
            example("notBearer", "Not a Bearer token", HttpStatus.UNAUTHORIZED, INVALID_TOKEN, NOT_BEARER,
                VALIDATE_PATH)));
    }

    private static void addAuthorizationErrors(Map<String, ApiResponse> responses) {
        responses.put("AdminRoleRequired", error(
            "The endpoint requires the `ADMIN` role.",
            example("adminRoleRequired", "Not an administrator", HttpStatus.FORBIDDEN,
                apiError(HttpStatus.FORBIDDEN), UNAUTHORIZED_OPERATION, PRODUCTS_PATH)));
        responses.put("UserRoleRequired", error(
            "The endpoint requires the `USER` role: administrators can't make purchases.",
            example("userRoleRequired", "Not a regular user", HttpStatus.FORBIDDEN,
                apiError(HttpStatus.FORBIDDEN), UNAUTHORIZED_OPERATION, PURCHASES_PATH)));
        responses.put("AdminRegistrationForbidden", error(
            "Only an administrator can register a user with the `ADMIN` role.",
            example("adminRegistrationForbidden", "Not an administrator", HttpStatus.FORBIDDEN, ACCESS_DENIED,
                "Only administrators can create admin users", USERS_PATH)));
        responses.put("ProductStatusForbidden", error(
            "Only administrators can list products that are not active (`status` other than `ACTIVE`).",
            example("anonymous", "Anonymous request", HttpStatus.FORBIDDEN, ACCESS_DENIED,
                "Unauthenticated users can only access active products", PRODUCTS_PATH),
            example("regularUser", "Regular user", HttpStatus.FORBIDDEN, apiError(HttpStatus.FORBIDDEN),
                "Only administrators can access non-active products", PRODUCTS_PATH)));
    }

    private static void addNotFoundAndConflictErrors(Map<String, ApiResponse> responses) {
        responses.put("UserNotFound", error(
            "No user has the given ID.",
            example("userNotFound", "Unknown user", HttpStatus.NOT_FOUND, apiError(HttpStatus.NOT_FOUND),
                "No user exists with the provided ID", USERS_PATH + "/42/role")));
        responses.put("ProductNotFound", error(
            "No product has the given ID.",
            example("productNotFound", "Unknown product", HttpStatus.NOT_FOUND, apiError(HttpStatus.NOT_FOUND),
                "Product not found", PRODUCTS_PATH + "/42")));
        responses.put("PurchaseProductNotFound", error(
            "A product of the purchase doesn't exist or is not active; the message names its ID.",
            example("productNotFound", "Unknown or inactive product", HttpStatus.NOT_FOUND,
                apiError(HttpStatus.NOT_FOUND), "Product 42 not found", PURCHASES_PATH)));
        responses.put("DuplicatedEmail", error(
            "Another user already has this email.",
            example("duplicatedEmail", "Email taken", HttpStatus.CONFLICT, apiError(HttpStatus.CONFLICT),
                "A user with this email already exists", USERS_PATH)));
        responses.put("DuplicatedProduct", error(
            "Another product, active or not, already has this name.",
            example("duplicatedProduct", "Name taken", HttpStatus.CONFLICT, apiError(HttpStatus.CONFLICT),
                "A product with this name already exists", PRODUCTS_PATH)));
    }

    private static void addServerErrors(Map<String, ApiResponse> responses, RateLimitProperties rateLimits) {
        responses.put("RateLimitExceeded", rateLimited(
            String.format("The caller's rate limit is exhausted. Anonymous requests are limited per IP address "
                    + "(bursts of %d), and signed-in requests per user: %d for users, %d for administrators. "
                    + "Wait the seconds of the `Retry-After` header.",
                rateLimits.getUnauthenticated().getCapacity(), rateLimits.getUser().getCapacity(),
                rateLimits.getAdmin().getCapacity()),
            PRODUCTS_PATH));
        responses.put("LoginRateLimitExceeded", rateLimited(
            String.format("Too many login attempts from this IP address: bursts of %d, refilled at %d every %d "
                    + "minute(s). Wait the seconds of the `Retry-After` header.",
                rateLimits.getLogin().getCapacity(), rateLimits.getLogin().getRefill(),
                rateLimits.getWindowMinutes()),
            "/api/auth/login"));
        responses.put("InternalError", error(
            "An unexpected error on the server.",
            example("internalError", "Server error", HttpStatus.INTERNAL_SERVER_ERROR,
                apiError(HttpStatus.INTERNAL_SERVER_ERROR), "An unexpected error occurred",
                "/.well-known/jwks.json")));
    }

    private static ApiResponse rateLimited(String description, String path) {
        Schema<?> seconds = new IntegerSchema().example(EXAMPLE_RETRY_AFTER_SECONDS);
        return error(description,
                example("rateLimitExceeded", "Rate limit exceeded", HttpStatus.TOO_MANY_REQUESTS,
                    apiError(HttpStatus.TOO_MANY_REQUESTS), RATE_LIMIT_MESSAGE, path))
            .addHeaderObject(HttpHeaders.RETRY_AFTER, new Header()
                .description("Seconds to wait before the next request is accepted")
                .schema(seconds))
            .addHeaderObject("X-Rate-Limit-Retry-After-Seconds", new Header()
                .description("Same as `Retry-After`")
                .schema(seconds));
    }

    private static ApiResponse error(String description, NamedExample... examples) {
        MediaType mediaType = new MediaType()
            .schema(new Schema<>().$ref("#/components/schemas/" + ApiDocs.ERROR_SCHEMA));
        for (NamedExample namedExample : examples) {
            mediaType.addExamples(namedExample.name(), namedExample.example());
        }
        return new ApiResponse()
            .description(description)
            .content(new Content().addMediaType(APPLICATION_JSON, mediaType));
    }

    private static NamedExample example(String name, String summary, HttpStatus status, String error,
            String message, String path, String... details) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", TIMESTAMP);
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        body.put("path", path);
        body.put("details", List.of(details));
        return new NamedExample(name, new Example().summary(summary).value(body));
    }

    // The error name of the exceptions handled as ApiException is the reason phrase of their status
    private static String apiError(HttpStatus status) {
        return status.getReasonPhrase();
    }

    private record NamedExample(String name, Example example) {
    }
}
