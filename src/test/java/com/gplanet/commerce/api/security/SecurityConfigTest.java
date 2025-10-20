package com.gplanet.commerce.api.security;

import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_ACTUATOR_HEALTH;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_ACTUATOR_INFO;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_AUTH_LOGIN;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_COMPRAS;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_JWKS;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_OPENID_CONFIG;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_PRODUCTOS;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_SWAGGER_UI;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_SWAGGER_DOCS;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_USUARIOS_ME;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_USUARIOS;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gplanet.commerce.api.configs.TestRedisConfiguration;
import com.gplanet.commerce.api.configs.app.health.ApiHealthIndicator;
import com.gplanet.commerce.api.configs.app.health.DatabaseHealthIndicator;
import com.gplanet.commerce.api.configs.app.props.CorsProperties;
import com.gplanet.commerce.api.dtos.auth.LoginDTO;
import com.gplanet.commerce.api.dtos.product.ProductDTO;
import com.gplanet.commerce.api.dtos.product.ProductResponseDTO;
import com.gplanet.commerce.api.dtos.product.ProductStatus;
import com.gplanet.commerce.api.dtos.usuario.UsuarioDTO;
import com.gplanet.commerce.api.dtos.usuario.UsuarioResponseDTO;
import com.gplanet.commerce.api.dtos.usuario.UsuarioUpdateDTO;
import com.gplanet.commerce.api.entities.Usuario;
import com.gplanet.commerce.api.repositories.UsuarioRepository;
import com.gplanet.commerce.api.services.ProductService;
import com.gplanet.commerce.api.services.UsuarioService;

/**
 * Security configuration tests for JWT/OAuth2 authentication.
 * Tests authentication, authorization, and CORS configuration.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfiguration.class)
class SecurityConfigTest {

    private static final String USER_NAME = "Test User";
    private static final String USER_EMAIL = "user@test.com";
    private static final String USER_PASSWORD = "password";
    private static final String ADMIN_EMAIL = "admin@test.com";
    private static final String ADMIN_PASSWORD = "password";
    private static final String TEST_PRODUCT_NAME = "Test Product";
    private static final String TEST_PRODUCT_DESC = "Test Product Description";
    private static final String RESPONSE_TOKEN_FIELD = "token";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String CORS_ORIGIN_HEADER = "Origin";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final BigDecimal TEST_PRODUCT_PRICE = new BigDecimal("10.00");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UsuarioService usuarioService;
    
    @MockitoBean
    private ProductService productService;

    @MockitoBean(name = "commerceApi")
    private ApiHealthIndicator apiHealthIndicator;

    @MockitoBean(name = "customDatabase")
    private DatabaseHealthIndicator databaseHealthIndicator;

    @Autowired
    private CorsProperties corsProperties;


    @BeforeEach
    void setup() {
        usuarioRepository.deleteAll();

        // Create real test user
        Usuario user = new Usuario();
        user.setNombre(USER_NAME);
        user.setEmail(USER_EMAIL);
        user.setPassword(passwordEncoder.encode(USER_PASSWORD));
        user.setRol(Usuario.Role.USER);
        user.setFechaCreacion(LocalDateTime.now());
        usuarioRepository.save(user);

        // Create real test admin
        Usuario admin = new Usuario();
        admin.setNombre(USER_NAME);
        admin.setEmail(ADMIN_EMAIL);
        admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
        admin.setRol(Usuario.Role.ADMIN);
        admin.setFechaCreacion(LocalDateTime.now());
        usuarioRepository.save(admin);
    }

    @Test
    void whenPublicEndpointThenAllowsAccess() throws Exception {
        // Test registration endpoint
        UsuarioDTO usuarioDto = new UsuarioDTO("test", "test@test.com", "Password123!");
        UsuarioResponseDTO responseDto = new UsuarioResponseDTO(1L, "test", "test@test.com", 
            Usuario.Role.USER, LocalDateTime.now());
        
        when(usuarioService.registrarUsuario(any(UsuarioDTO.class))).thenReturn(responseDto);

        mockMvc.perform(post(ENDPOINT_USUARIOS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(usuarioDto)))
                .andExpect(status().isOk());

        // Test products listing endpoint
        Page<ProductResponseDTO> productPage = new PageImpl<>(Arrays.asList(
            new ProductResponseDTO(1L, TEST_PRODUCT_NAME, TEST_PRODUCT_DESC, 
                TEST_PRODUCT_PRICE, LocalDateTime.now(), true)
        ));
        
        when(productService.listarProductos(eq(ProductStatus.ACTIVE), eq(null), 
            eq(0), eq(10), eq("name"), eq("ASC")))
            .thenReturn(productPage);

        mockMvc.perform(get(ENDPOINT_PRODUCTOS))
                .andExpect(status().isOk());
    }

    @Test
    void whenUnauthenticatedAccessToProtectedEndpointThenReturns401() throws Exception {
        mockMvc.perform(get(ENDPOINT_COMPRAS))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenJwtAuthWithValidTokenThenAllowsAccess() throws Exception {
        // Arrange
        LoginDTO loginDto = new LoginDTO(USER_EMAIL, USER_PASSWORD);
        
        // Get real JWT token
        String tokenResponse = mockMvc.perform(post(ENDPOINT_AUTH_LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        String jwtToken = OBJECT_MAPPER.readTree(tokenResponse).get(RESPONSE_TOKEN_FIELD).asText();

        // Use token to access protected endpoint
        Page<ProductResponseDTO> productPage = new PageImpl<>(Arrays.asList(
            new ProductResponseDTO(1L, TEST_PRODUCT_NAME, TEST_PRODUCT_DESC, 
                TEST_PRODUCT_PRICE, LocalDateTime.now(), true)
        ));
        
        when(productService.listarProductos(eq(ProductStatus.ACTIVE), eq(null), 
            eq(0), eq(10), eq("nombre"), eq("ASC")))
            .thenReturn(productPage);

        mockMvc.perform(get(ENDPOINT_COMPRAS)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + jwtToken))
                .andExpect(status().isOk());
    }

    @Test
    void whenJwtAuthWithInvalidTokenThenReturns401() throws Exception {
        mockMvc.perform(get(ENDPOINT_COMPRAS)
                .header(AUTHORIZATION_HEADER, "Bearer invalid.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenUserAccessesAdminEndpointThenReturns403() throws Exception {
        // Arrange
        LoginDTO loginDto = new LoginDTO(USER_EMAIL, USER_PASSWORD);

        // Get real user JWT token
        String tokenResponse = mockMvc.perform(post(ENDPOINT_AUTH_LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        String jwtToken = OBJECT_MAPPER.readTree(tokenResponse).get(RESPONSE_TOKEN_FIELD).asText();

        // Act & Assert - User tries to access admin endpoint
        mockMvc.perform(post(ENDPOINT_PRODUCTOS)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void whenAdminAccessesAdminEndpointThenAllowsAccess() throws Exception {
        // Arrange
        LoginDTO loginDto = new LoginDTO(ADMIN_EMAIL, ADMIN_PASSWORD);

        ProductDTO productDto = new ProductDTO(TEST_PRODUCT_NAME, TEST_PRODUCT_DESC, 
            TEST_PRODUCT_PRICE, true);
        ProductResponseDTO responseDto = new ProductResponseDTO(1L, TEST_PRODUCT_NAME, 
            TEST_PRODUCT_DESC, TEST_PRODUCT_PRICE, LocalDateTime.now(), true);

        when(productService.crearProducto(any(ProductDTO.class))).thenReturn(responseDto);

        // Get admin JWT token
        String tokenResponse = mockMvc.perform(post(ENDPOINT_AUTH_LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        String jwtToken = OBJECT_MAPPER.readTree(tokenResponse).get(RESPONSE_TOKEN_FIELD).asText();

        // Act & Assert
        mockMvc.perform(post(ENDPOINT_PRODUCTOS)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(productDto)))
                .andExpect(status().isOk());
    }

    @Test
    void whenNoAuthenticationHeaderThenReturns401() throws Exception {
        mockMvc.perform(get(ENDPOINT_COMPRAS))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenCsrfDisabledThenAllowsPostWithoutToken() throws Exception {
        // Arrange
        LoginDTO loginDto = new LoginDTO(ADMIN_EMAIL, ADMIN_PASSWORD);

        ProductDTO productDto = new ProductDTO(TEST_PRODUCT_NAME, TEST_PRODUCT_DESC, 
            TEST_PRODUCT_PRICE, true);
        ProductResponseDTO responseDto = new ProductResponseDTO(1L, TEST_PRODUCT_NAME, 
            TEST_PRODUCT_DESC, TEST_PRODUCT_PRICE, LocalDateTime.now(), true);

        when(productService.crearProducto(any(ProductDTO.class))).thenReturn(responseDto);

        // Get admin JWT token
        String tokenResponse = mockMvc.perform(post(ENDPOINT_AUTH_LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        String jwtToken = OBJECT_MAPPER.readTree(tokenResponse).get(RESPONSE_TOKEN_FIELD).asText();

        // Act & Assert - No CSRF token required
        mockMvc.perform(post(ENDPOINT_PRODUCTOS)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(productDto)))
                .andExpect(status().isOk());
    }

    @Test
    void whenUserAccessesOwnProfileThenAllowsAccess() throws Exception {
        // Arrange
        LoginDTO loginDto = new LoginDTO(USER_EMAIL, USER_PASSWORD);
        
        UsuarioUpdateDTO actualizacionDto = new UsuarioUpdateDTO(
            "Updated Name", "updated@test.com");
        UsuarioResponseDTO responseDto = new UsuarioResponseDTO(1L, "Updated Name", 
            "updated@test.com", Usuario.Role.USER, LocalDateTime.now());
            
        when(usuarioService.actualizarPerfil(anyString(), any(UsuarioUpdateDTO.class)))
            .thenReturn(responseDto);

        // Get user JWT token
        String tokenResponse = mockMvc.perform(post(ENDPOINT_AUTH_LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        String jwtToken = OBJECT_MAPPER.readTree(tokenResponse).get(RESPONSE_TOKEN_FIELD).asText();

        // Act & Assert
        mockMvc.perform(patch(ENDPOINT_USUARIOS_ME)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(actualizacionDto)))
                .andExpect(status().isOk());
    }

    @Test
    void whenAccessingProtectedEndpointThenAuthenticationHeaderIsRequired() throws Exception {
        mockMvc.perform(get(ENDPOINT_COMPRAS))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenMultipleAuthenticationAttemptsThenNoSessionCreated() throws Exception {
        // Arrange
        LoginDTO loginDto = new LoginDTO(USER_EMAIL, USER_PASSWORD);

        // Get JWT token
        String tokenResponse = mockMvc.perform(post(ENDPOINT_AUTH_LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        String jwtToken = OBJECT_MAPPER.readTree(tokenResponse).get(RESPONSE_TOKEN_FIELD).asText();

        // Act & Assert - First request
        MvcResult result1 = mockMvc.perform(get(ENDPOINT_COMPRAS)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + jwtToken))
                .andExpect(status().isOk())
                .andReturn();

        // Second request - Should work with same token (stateless)
        mockMvc.perform(get(ENDPOINT_COMPRAS)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + jwtToken))
                .andExpect(status().isOk());

        // Verify no session was created
        assertNull(result1.getRequest().getSession(false));
    }

    @Test
    void whenCorsRequestThenReturnsCorrectHeaders() throws Exception {
        mockMvc.perform(options(ENDPOINT_AUTH_LOGIN)
                .header(CORS_ORIGIN_HEADER, corsProperties.getTestOrigin())
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", corsProperties.getTestOrigin()))
                .andExpect(header().string("Access-Control-Allow-Methods", 
                    String.join(",", corsProperties.getAllowedMethods())))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void whenCorsRequestFromUnauthorizedOriginThenReturns403() throws Exception {
        mockMvc.perform(options(ENDPOINT_AUTH_LOGIN)
                .header(CORS_ORIGIN_HEADER, "http://unauthorized-domain.com")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void whenActualRequestWithValidOriginThenSucceeds() throws Exception {
        mockMvc.perform(post(ENDPOINT_AUTH_LOGIN)
                .header(CORS_ORIGIN_HEADER, corsProperties.getTestOrigin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(header().string("Access-Control-Allow-Origin", 
                    corsProperties.getTestOrigin()));
    }

    @Test
    void whenAccessingActuatorEndpointsThenAllowsAccess() throws Exception {
        when(apiHealthIndicator.health()).thenReturn(Health.up().build());
        when(databaseHealthIndicator.health()).thenReturn(Health.up().build());

        mockMvc.perform(get(ENDPOINT_ACTUATOR_HEALTH))
                .andExpect(status().isOk());

        mockMvc.perform(get(ENDPOINT_ACTUATOR_INFO))
                .andExpect(status().isOk());
    }

    @Test
    void whenAccessingSwaggerEndpointsThenAllowsAccess() throws Exception {
        mockMvc.perform(get(ENDPOINT_SWAGGER_UI))
                .andExpect(status().isOk());

        mockMvc.perform(get(ENDPOINT_SWAGGER_DOCS))
                .andExpect(status().isOk());
    }

    @Test
    void whenAccessingWellKnownEndpointsThenAllowsAccess() throws Exception {
        mockMvc.perform(get(ENDPOINT_JWKS))
                .andExpect(status().isOk());

        mockMvc.perform(get(ENDPOINT_OPENID_CONFIG))
                .andExpect(status().isOk());
    }
}
