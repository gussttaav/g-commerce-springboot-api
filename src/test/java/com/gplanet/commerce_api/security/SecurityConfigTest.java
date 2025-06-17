package com.gplanet.commerce_api.security;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gplanet.commerce_api.configs.app.props.CorsProperties;
import com.gplanet.commerce_api.dtos.producto.ProductStatus;
import com.gplanet.commerce_api.dtos.producto.ProductoDTO;
import com.gplanet.commerce_api.dtos.producto.ProductoResponseDTO;
import com.gplanet.commerce_api.dtos.usuario.ActualizacionUsuarioDTO;
import com.gplanet.commerce_api.dtos.usuario.LoginDTO;
import com.gplanet.commerce_api.dtos.usuario.UsuarioResponseDTO;
import com.gplanet.commerce_api.entities.Usuario;
import com.gplanet.commerce_api.exceptions.InvalidPasswordException;
import com.gplanet.commerce_api.repositories.UsuarioRepository;
import com.gplanet.commerce_api.services.ProductoService;
import com.gplanet.commerce_api.services.UsuarioService;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UsuarioService usuarioService;
    
    @MockitoBean
    private ProductoService productoService;

    @Autowired
    private CorsProperties corsProperties;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        // Clear users before each test
        usuarioRepository.deleteAll();
    }

    @Test
    void whenPublicEndpoint_thenAllowsAccess() throws Exception {
        // Test registration endpoint
        mockMvc.perform(post("/api/usuarios/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"test\",\"email\":\"test@test.com\",\"password\":\"password\"}"))
                .andExpect(status().isOk());

        // Test products listing endpoint
        Page<ProductoResponseDTO> productPage = new PageImpl<>(Arrays.asList(
            ProductoResponseDTO.builder()
                .id(1L)
                .nombre("Test Product")
                .activo(true)
                .build()
        ));
        
        when(productoService.listarProductos(eq(ProductStatus.ACTIVE), eq(null), eq(0), eq(10), eq("nombre"), eq("ASC")))
            .thenReturn(productPage);

        mockMvc.perform(get("/api/productos/listar"))
                .andExpect(status().isOk());
    }

    @Test
    void whenUnauthenticatedAccessToActiveProducts_thenAllowsAccess() throws Exception {
        Page<ProductoResponseDTO> productPage = new PageImpl<>(Arrays.asList(
            ProductoResponseDTO.builder()
                .id(1L)
                .nombre("Test Product")
                .activo(true)
                .build()
        ));
        
        when(productoService.listarProductos(eq(ProductStatus.ACTIVE), eq(null), eq(0), eq(10), eq("nombre"), eq("ASC")))
            .thenReturn(productPage);

        mockMvc.perform(get("/api/productos/listar"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("WWW-Authenticate"));
    }

    @Test
    void whenUnauthenticatedAccessToInactiveProducts_thenReturns403() throws Exception {
        mockMvc.perform(get("/api/productos/listar")
                .param("status", "INACTIVE"))
                .andExpect(status().isForbidden());
    }

    @Test
    void whenBasicAuthWithValidCredentials_thenAllowsAccess() throws Exception {
        // Arrange
        setupTestUser("user@test.com", "password", Usuario.Role.USER);

        // Act & Assert
        mockMvc.perform(get("/api/compras/listar")
                .with(httpBasic("user@test.com", "password")))
                .andExpect(status().isOk());
    }

    @Test
    void whenBasicAuthWithInvalidCredentials_thenReturns401() throws Exception {
        mockMvc.perform(get("/api/compras/listar")
                .with(httpBasic("invalid@test.com", "wrongpassword")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenUserAccessesAdminEndpoint_thenReturns403() throws Exception {
        // Arrange
        setupTestUser("user@test.com", "password", Usuario.Role.USER);

        // Act & Assert
        mockMvc.perform(post("/api/productos/crear")
                .with(httpBasic("user@test.com", "password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void whenAdminAccessesAdminEndpoint_thenAllowsAccess() throws Exception {
        setupTestUser("admin@test.com", "password", Usuario.Role.ADMIN);
        
        ProductoDTO productoDTO = ProductoDTO.builder()
            .nombre("Test Product")
            .precio(new BigDecimal("10.00"))
            .descripcion("Test Description")
            .build();
        
        ProductoResponseDTO responseDTO = ProductoResponseDTO.builder()
            .id(1L)
            .nombre("Test Product")
            .build();

        when(productoService.crearProducto(any(ProductoDTO.class))).thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(post("/api/productos/crear")
                .with(httpBasic("admin@test.com", "password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productoDTO)))
                .andExpect(status().isOk());
    }

    @Test
    void whenNoAuthenticationHeader_thenReturns401() throws Exception {
        mockMvc.perform(get("/api/compras/listar"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenCsrfDisabled_thenAllowsPostWithoutToken() throws Exception {
        setupTestUser("admin@test.com", "password", Usuario.Role.ADMIN);
        
        ProductoDTO productoDTO = ProductoDTO.builder()
            .nombre("Test Product")
            .precio(new BigDecimal("10.00"))
            .descripcion("Test Description")
            .build();
        
        ProductoResponseDTO responseDTO = ProductoResponseDTO.builder()
            .id(1L)
            .nombre("Test Product")
            .build();
        when(productoService.crearProducto(any(ProductoDTO.class))).thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(post("/api/productos/crear")
                .with(httpBasic("admin@test.com", "password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productoDTO)))
                .andExpect(status().isOk());
    }

    @Test
    void whenUserAccessesOwnProfile_thenAllowsAccess() throws Exception {
         // Arrange
        setupTestUser("user@test.com", "password", Usuario.Role.USER);

        ActualizacionUsuarioDTO actualizacionDTO = new ActualizacionUsuarioDTO(
            "Updated Name", "updated@test.com");
            
        UsuarioResponseDTO responseDTO = UsuarioResponseDTO.builder()
            .id(1L)
            .nombre("Updated Name")
            .email("updated@test.com")
            .build();
            
        when(usuarioService.actualizarPerfil(anyString(), any(ActualizacionUsuarioDTO.class)))
            .thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(put("/api/usuarios/perfil")
                .with(httpBasic("user@test.com", "password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(actualizacionDTO)))
                .andExpect(status().isOk());
    }

    @Test
    void whenAccessingProtectedEndpoint_thenAuthenticationHeaderIsRequired() throws Exception {
        mockMvc.perform(get("/api/compras/listar"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("WWW-Authenticate"));
    }

    @Test
    void whenMultipleAuthenticationAttempts_thenNoSessionCreated() throws Exception {
        // Arrange
        setupTestUser("user@test.com", "password", Usuario.Role.USER);

        // Act & Assert - First request
        MvcResult result1 = mockMvc.perform(get("/api/compras/listar")
                .with(httpBasic("user@test.com", "password")))
                .andExpect(status().isOk())
                .andReturn();

        // Second request - Should require authentication again (stateless)
        mockMvc.perform(get("/api/compras/listar"))
                .andExpect(status().isUnauthorized());

        // Verify no session was created
        assertNull(result1.getRequest().getSession(false));
    }

    @Test
    void whenLoginWithValidCredentials_thenReturnsOk() throws Exception {
        // Arrange
        setupTestUser("user@test.com", "password", Usuario.Role.USER);
        
        LoginDTO loginDTO = new LoginDTO("user@test.com", "password");
        UsuarioResponseDTO responseDTO = UsuarioResponseDTO.builder()
            .id(1L)
            .nombre("Test User")
            .email("user@test.com")
            .build();
            
        when(usuarioService.login(any(LoginDTO.class))).thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(post("/api/usuarios/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isOk());
    }

    @Test
    void whenLoginWithInvalidCredentials_thenReturns401() throws Exception {
        // Arrange
        LoginDTO loginDTO = new LoginDTO("invalid@test.com", "wrongpassword");
        when(usuarioService.login(any(LoginDTO.class)))
            .thenThrow(new InvalidPasswordException("Contraseña incorrecta."));

        // Act & Assert
        mockMvc.perform(post("/api/usuarios/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenCorsRequest_thenReturnsCorrectHeaders() throws Exception {
        mockMvc.perform(options("/api/usuarios/login")
                .header("Origin", corsProperties.getTestOrigin())
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", corsProperties.getTestOrigin()))
                .andExpect(header().string("Access-Control-Allow-Methods", 
                    String.join(",", corsProperties.getAllowedMethods())))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void whenCorsRequestFromUnauthorizedOrigin_thenReturns403() throws Exception {
        mockMvc.perform(options("/api/usuarios/login")
                .header("Origin", "http://unauthorized-domain.com")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void whenCorsRequestWithInvalidMethod_thenReturns403() throws Exception {
        mockMvc.perform(options("/api/usuarios/login")
                .header("Origin", corsProperties.getTestOrigin())
                .header("Access-Control-Request-Method", "PATCH"))
                .andExpect(status().isForbidden());
    }
    
    @Test
    void whenCorsRequestWithInvalidHeader_thenReturns403() throws Exception {
        mockMvc.perform(options("/api/usuarios/login")
                .header("Origin", corsProperties.getTestOrigin())
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "InvalidHeader"))
                .andExpect(status().isForbidden());
    }

    @Test
    void whenActualRequestWithValidOrigin_thenSucceeds() throws Exception {
        mockMvc.perform(post("/api/usuarios/login")
                .header("Origin", corsProperties.getTestOrigin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(header().string("Access-Control-Allow-Origin", 
                    corsProperties.getTestOrigin()));
    }

    private void setupTestUser(String email, String password, Usuario.Role role) {
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setNombre("Test User");
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setRol(role);
        usuario.setFechaCreacion(LocalDateTime.now());
        usuarioRepository.save(usuario);
    }

    @Test
    void whenAdminUpdatesProduct_thenAllowsAccess() throws Exception {
        // Arrange
        setupTestUser("admin@test.com", "password", Usuario.Role.ADMIN);
        
        ProductoDTO productoDTO = ProductoDTO.builder()
            .nombre("Updated Product")
            .precio(new BigDecimal("20.00"))
            .descripcion("Updated Description")
            .build();
        
        ProductoResponseDTO responseDTO = ProductoResponseDTO.builder()
            .id(1L)
            .nombre("Updated Product")
            .build();

        when(productoService.actualizarProducto(eq(1L), any(ProductoDTO.class)))
            .thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(put("/api/productos/actualizar/1")
                .with(httpBasic("admin@test.com", "password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productoDTO)))
                .andExpect(status().isOk());
    }

    @Test
    void whenUserUpdatesProduct_thenReturns403() throws Exception {
        // Arrange
        setupTestUser("user@test.com", "password", Usuario.Role.USER);
        
        ProductoDTO productoDTO = ProductoDTO.builder()
            .nombre("Unauthorized Update")
            .precio(new BigDecimal("20.00"))
            .build();

        // Act & Assert
        mockMvc.perform(put("/api/productos/actualizar/1")
                .with(httpBasic("user@test.com", "password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productoDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void whenNoAuthUpdateProduct_thenReturns401() throws Exception {
        ProductoDTO productoDTO = ProductoDTO.builder()
            .nombre("Test Update")
            .precio(new BigDecimal("20.00"))
            .build();

        mockMvc.perform(put("/api/productos/actualizar/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productoDTO)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenGetUserProfile_withValidCredentials_thenAllowsAccess() throws Exception {
        // Arrange
        setupTestUser("user@test.com", "password", Usuario.Role.USER);
        
        UsuarioResponseDTO responseDTO = UsuarioResponseDTO.builder()
            .id(1L)
            .nombre("Test User")
            .email("user@test.com")
            .rol(Usuario.Role.USER)
            .build();
            
        when(usuarioService.obtenerPerfil(anyString())).thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(get("/api/usuarios/perfil")
                .with(httpBasic("user@test.com", "password")))
                .andExpect(status().isOk());
    }

    @Test
    void whenGetUserProfile_withNoAuth_thenReturns401() throws Exception {
        mockMvc.perform(get("/api/usuarios/perfil"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenListUsers_asAdmin_thenAllowsAccess() throws Exception {
        // Arrange
        setupTestUser("admin@test.com", "password", Usuario.Role.ADMIN);
        
        Page<UsuarioResponseDTO> userPage = new PageImpl<>(Arrays.asList(
            UsuarioResponseDTO.builder()
                .id(1L)
                .nombre("Test User")
                .email("user@test.com")
                .rol(Usuario.Role.USER)
                .build()
        ));
        
        when(usuarioService.listarUsuarios(0, 10, "email", "ASC")).thenReturn(userPage);

        // Act & Assert
        mockMvc.perform(get("/api/usuarios/admin/listar")
                .with(httpBasic("admin@test.com", "password")))
                .andExpect(status().isOk());
    }

    @Test
    void whenListUsers_asUser_thenReturns403() throws Exception {
        // Arrange
        setupTestUser("user@test.com", "password", Usuario.Role.USER);

        // Act & Assert
        mockMvc.perform(get("/api/usuarios/admin/listar")
                .with(httpBasic("user@test.com", "password")))
                .andExpect(status().isForbidden());
    }

    @Test
    void whenListUsers_withNoAuth_thenReturns401() throws Exception {
        mockMvc.perform(get("/api/usuarios/admin/listar"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenChangeUserRole_asAdmin_thenAllowsAccess() throws Exception {
        // Arrange
        setupTestUser("admin@test.com", "password", Usuario.Role.ADMIN);
        
        doNothing().when(usuarioService).cambiarRol(anyLong(), any(Usuario.Role.class));

        // Act & Assert
        mockMvc.perform(put("/api/usuarios/admin/change-role")
                .with(httpBasic("admin@test.com", "password"))
                .param("userId", "1")
                .param("newRole", "USER"))
                .andExpect(status().isOk());
    }

    @Test
    void whenChangeUserRole_asUser_thenReturns403() throws Exception {
        // Arrange
        setupTestUser("user@test.com", "password", Usuario.Role.USER);

        // Act & Assert
        mockMvc.perform(put("/api/usuarios/admin/change-role")
                .with(httpBasic("user@test.com", "password"))
                .param("userId", "1")
                .param("newRole", "USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void whenChangeUserRole_withNoAuth_thenReturns401() throws Exception {
        mockMvc.perform(put("/api/usuarios/admin/change-role")
                .param("userId", "1")
                .param("newRole", "USER"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenChangeUserRole_withInvalidRole_thenReturns400() throws Exception {
        // Arrange
        setupTestUser("admin@test.com", "password", Usuario.Role.ADMIN);

        // Act & Assert
        mockMvc.perform(put("/api/usuarios/admin/change-role")
                .with(httpBasic("admin@test.com", "password"))
                .param("userId", "1")
                .param("newRole", "INVALID_ROLE"))
                .andExpect(status().isBadRequest());
    }
}