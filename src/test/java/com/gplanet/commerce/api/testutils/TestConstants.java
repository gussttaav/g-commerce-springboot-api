package com.gplanet.commerce.api.testutils;

public final class TestConstants {

    public static final String BASE_URL = "/api";

    // ========================
    // User Management Endpoints
    // ========================
    public static final String ENDPOINT_USUARIOS = BASE_URL + "/usuarios";

    public static final String ENDPOINT_USUARIOS_REGISTRO = ENDPOINT_USUARIOS + "/registro";
    public static final String ENDPOINT_USUARIOS_PERFIL = ENDPOINT_USUARIOS + "/perfil";
    public static final String ENDPOINT_USUARIOS_PASSWORD = ENDPOINT_USUARIOS + "/password";

    // Admin-only (User Management)
    public static final String ENDPOINT_USUARIOS_ADMIN = ENDPOINT_USUARIOS + "/admin";
    public static final String ENDPOINT_USUARIOS_ADMIN_REGISTRO = ENDPOINT_USUARIOS_ADMIN + "/registro";
    public static final String ENDPOINT_USUARIOS_ADMIN_LISTAR = ENDPOINT_USUARIOS_ADMIN + "/listar";
    public static final String ENDPOINT_USUARIOS_ADMIN_CHANGE_ROLE = ENDPOINT_USUARIOS_ADMIN + "/change-role";

    // ========================
    // Product Management Endpoints
    // ========================
    public static final String ENDPOINT_PRODUCTOS = BASE_URL + "/productos";

    public static final String ENDPOINT_PRODUCTOS_LISTAR = ENDPOINT_PRODUCTOS + "/listar";
    public static final String ENDPOINT_PRODUCTOS_CREAR = ENDPOINT_PRODUCTOS + "/crear";
    public static final String ENDPOINT_PRODUCTOS_ELIMINAR = ENDPOINT_PRODUCTOS + "/eliminar";
    public static final String ENDPOINT_PRODUCTOS_ACTUALIZAR = ENDPOINT_PRODUCTOS + "/actualizar";

    // ========================
    // Purchase Management Endpoints
    // ========================
    public static final String ENDPOINT_COMPRAS = BASE_URL + "/compras";

    public static final String ENDPOINT_COMPRAS_NUEVA = ENDPOINT_COMPRAS + "/nueva";
    public static final String ENDPOINT_COMPRAS_LISTAR = ENDPOINT_COMPRAS + "/listar";


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
    // Roles
    // ========================
    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";

    // ========================
    // Tests users
    // ========================

    public static final String TEST_NAME = "Test User";
    public static final String TEST_USERNAME = "test@example.com";
    public static final String TEST_PASSWD = "testPasswd123!";
    public static final String TEST_ADMIN_USERNAME = "admin@example.com";
    public static final String TEST_ADMIN_PASSWD = "adminPasswd123!";

    private TestConstants() {}
}
