package com.gplanet.commerce.api.testutils;

/**
 * <p>Utility class that defines constants used across test classes in the G-Commerce API.
 * This class contains various endpoint URLs, role definitions, and test user credentials
 * used for testing purposes.
 * </p>
 * The constants are organized into several categories:
 * <ul>
 *   <li>User Management Endpoints</li>
 *   <li>Product Management Endpoints</li>
 *   <li>Purchase Management Endpoints</li>
 *   <li>Authentication Endpoints</li>
 *   <li>JWKS and OpenID Connect configuration</li>
 *   <li>Actuator Endpoints</li>
 *   <li>Swagger Documentation</li>
 * </ul>
 * 
 * @author Gustavo
 * @version 1.0
 */
public final class TestConstants {

    public static final String BASE_URL = "/api";

    // ========================
    // User Management Endpoints
    // ========================
    public static final String ENDPOINT_USUARIOS = BASE_URL + "/usuarios";
    public static final String ENDPOINT_USUARIOS_ME = ENDPOINT_USUARIOS + "/me";

    // Admin-only (User Management)
    public static final String ENDPOINT_USUARIOS_ADMIN = ENDPOINT_USUARIOS + "/admin";
    public static final String ENDPOINT_USUARIOS_ADMIN_CHANGE_ROLE = ENDPOINT_USUARIOS_ADMIN + "/change-role";

    // ========================
    // Product Management Endpoints
    // ========================
    public static final String ENDPOINT_PRODUCTOS = BASE_URL + "/productos";

    // ========================
    // Purchase Management Endpoints
    // ========================
    public static final String ENDPOINT_COMPRAS = BASE_URL + "/compras";


    // ========================
    // Authentication Endpoints
    // ========================
    public static final String ENDPOINT_AUTH = BASE_URL + "/auth";

    public static final String ENDPOINT_AUTH_LOGIN = ENDPOINT_AUTH + "/login";
    public static final String ENDPOINT_AUTH_LOGOUT = ENDPOINT_AUTH + "/logout";
    public static final String ENDPOINT_AUTH_REFRESH = ENDPOINT_AUTH + "/refresh";
    public static final String ENDPOINT_AUTH_VALIDATE = ENDPOINT_AUTH + "/validate";


    // ========================
    // JSON Web Key Set (JWKS) and OpenID Connect configuration
    // ========================

    public static final String ENDPOINT_JWKS = "/.well-known/jwks.json";
    public static final String ENDPOINT_OPENID_CONFIG = "/.well-known/openid-configuration";

    // ========================
    // ACTUATORS
    // ========================

    public static final String ENDPOINT_ACTUATOR_HEALTH = "/actuator/health";
    public static final String ENDPOINT_ACTUATOR_INFO = "/actuator/info";

    // ========================
    // SWAGGER DOCUMENTATION
    // ========================

    public static final String ENDPOINT_SWAGGER_UI = "/swagger-ui/index.html";
    public static final String ENDPOINT_SWAGGER_DOCS = "/v3/api-docs";

    private TestConstants() {
    }
}
