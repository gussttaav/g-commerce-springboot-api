package com.gplanet.commerce.api.configs.app.docs;

/**
 * Names shared by the OpenAPI configuration and the controller annotations: tags, security schemes
 * and references to the reusable responses and schemas defined in {@link OpenAPIConfig}.
 *
 * <p>Annotations need compile-time constants, so a typo in a reference fails to compile
 * instead of producing a broken link in the published document.</p>
 *
 * @author Gustavo
 * @version 1.0
 */
public final class ApiDocs {

    /** Tag of the authentication endpoints. */
    public static final String TAG_AUTHENTICATION = "Authentication";
    /** Tag of the user endpoints. */
    public static final String TAG_USERS = "Users";
    /** Tag of the product endpoints. */
    public static final String TAG_PRODUCTS = "Products";
    /** Tag of the purchase endpoints. */
    public static final String TAG_PURCHASES = "Purchases";
    /** Tag of the JWKS and OpenID discovery endpoints. */
    public static final String TAG_DISCOVERY = "Discovery";

    /** Security scheme of the access tokens. */
    public static final String BEARER_AUTH = "bearerAuth";
    /** Security scheme of the refresh tokens, only accepted by the refresh endpoint. */
    public static final String REFRESH_TOKEN_AUTH = "refreshToken";

    /** Name of the reusable error body schema. */
    public static final String ERROR_SCHEMA = "Error";
    /** Name of the reusable header with the requests left in the rate limit. */
    public static final String RATE_LIMIT_REMAINING_HEADER = "X-Rate-Limit-Remaining";

    /** 400: a request body field, query parameter or path variable is invalid. */
    public static final String INVALID_INPUT = "#/components/responses/InvalidInput";
    /** 400: the profile update is invalid, including the password change checks. */
    public static final String INVALID_PROFILE_UPDATE = "#/components/responses/InvalidProfileUpdate";
    /** 401: no access token, or an invalid, expired or revoked one. */
    public static final String AUTHENTICATION_REQUIRED = "#/components/responses/AuthenticationRequired";
    /** 401: wrong email or password at login. */
    public static final String INVALID_CREDENTIALS = "#/components/responses/InvalidCredentials";
    /** 401: missing, invalid, expired, used or revoked refresh token. */
    public static final String INVALID_REFRESH_TOKEN = "#/components/responses/InvalidRefreshToken";
    /** 401: the token to validate is missing from the Authorization header. */
    public static final String BEARER_TOKEN_REQUIRED = "#/components/responses/BearerTokenRequired";
    /** 403: the endpoint requires the ADMIN role. */
    public static final String ADMIN_ROLE_REQUIRED = "#/components/responses/AdminRoleRequired";
    /** 403: the endpoint requires the USER role. */
    public static final String USER_ROLE_REQUIRED = "#/components/responses/UserRoleRequired";
    /** 403: only administrators can register administrators. */
    public static final String ADMIN_REGISTRATION_FORBIDDEN = "#/components/responses/AdminRegistrationForbidden";
    /** 403: only administrators can list products that are not active. */
    public static final String PRODUCT_STATUS_FORBIDDEN = "#/components/responses/ProductStatusForbidden";
    /** 404: the user doesn't exist. */
    public static final String USER_NOT_FOUND = "#/components/responses/UserNotFound";
    /** 404: the product doesn't exist. */
    public static final String PRODUCT_NOT_FOUND = "#/components/responses/ProductNotFound";
    /** 404: a product of the purchase doesn't exist or is not active. */
    public static final String PURCHASE_PRODUCT_NOT_FOUND = "#/components/responses/PurchaseProductNotFound";
    /** 409: another user has the email. */
    public static final String DUPLICATED_EMAIL = "#/components/responses/DuplicatedEmail";
    /** 409: another product has the name. */
    public static final String DUPLICATED_PRODUCT = "#/components/responses/DuplicatedProduct";
    /** 429: the rate limit of the caller is exhausted. */
    public static final String RATE_LIMIT_EXCEEDED = "#/components/responses/RateLimitExceeded";
    /** 429: too many login attempts from the client's IP address. */
    public static final String LOGIN_RATE_LIMIT_EXCEEDED = "#/components/responses/LoginRateLimitExceeded";
    /** 500: an unexpected server error. */
    public static final String INTERNAL_ERROR = "#/components/responses/InternalError";

    /** Paginated list of products. */
    public static final String PAGINATED_PRODUCTS = "#/components/schemas/PaginatedProducts";
    /** Paginated list of purchases. */
    public static final String PAGINATED_PURCHASES = "#/components/schemas/PaginatedPurchases";
    /** Paginated list of users. */
    public static final String PAGINATED_USERS = "#/components/schemas/PaginatedUsers";

    private ApiDocs() {
        throw new UnsupportedOperationException("Utility class");
    }
}
