package com.mitienda.gestion_tienda.security;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mitienda.gestion_tienda.configs.CorsProperties;
import com.mitienda.gestion_tienda.dtos.producto.ProductoDTO;
import com.mitienda.gestion_tienda.dtos.producto.ProductoResponseDTO;
import com.mitienda.gestion_tienda.dtos.usuario.ActualizacionUsuarioDTO;
import com.mitienda.gestion_tienda.dtos.usuario.LoginDTO;
import com.mitienda.gestion_tienda.dtos.usuario.UsuarioResponseDTO;
import com.mitienda.gestion_tienda.entities.Usuario;
import com.mitienda.gestion_tienda.exceptions.InvalidPasswordException;
import com.mitienda.gestion_tienda.repositories.UsuarioRepository;
import com.mitienda.gestion_tienda.services.ProductoService;
import com.mitienda.gestion_tienda.services.UsuarioService;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
        mockMvc.perform(post("/api/usuarios/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"test\",\"email\":\"test@test.com\",\"password\":\"password\"}"))
                .andExpect(status().isOk());
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
}