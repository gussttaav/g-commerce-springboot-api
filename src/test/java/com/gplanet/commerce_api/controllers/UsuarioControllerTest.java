package com.gplanet.commerce_api.controllers;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gplanet.commerce_api.dtos.usuario.ActualizacionUsuarioDTO;
import com.gplanet.commerce_api.dtos.usuario.CambioPasswdDTO;
import com.gplanet.commerce_api.dtos.usuario.LoginDTO;
import com.gplanet.commerce_api.dtos.usuario.UsuarioAdminDTO;
import com.gplanet.commerce_api.dtos.usuario.UsuarioDTO;
import com.gplanet.commerce_api.dtos.usuario.UsuarioResponseDTO;
import com.gplanet.commerce_api.entities.Usuario;
import com.gplanet.commerce_api.exceptions.InvalidPasswordException;
import com.gplanet.commerce_api.exceptions.PasswordMismatchException;
import com.gplanet.commerce_api.exceptions.ResourceNotFoundException;
import com.gplanet.commerce_api.services.UsuarioDetallesService;
import com.gplanet.commerce_api.services.UsuarioService;

@WebMvcTest(UsuarioController.class)
@Import(TestSecurityConfig.class)
class UsuarioControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private UsuarioDetallesService userDetailsService;
    
    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/usuarios";
    private static final String TEST_USER_EMAIL = "test@example.com";
    private static final String TEST_ADMIN_EMAIL = "admin@example.com";

    private UsuarioDTO buildValidUsuarioDTO() {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setNombre("Test User");
        dto.setEmail(TEST_USER_EMAIL);
        dto.setPassword("Password123!");
        return dto;
    }

    private UsuarioAdminDTO buildValidUsuarioAdminDTO() {
        UsuarioAdminDTO dto = new UsuarioAdminDTO();
        dto.setNombre("Admin User");
        dto.setEmail(TEST_ADMIN_EMAIL);
        dto.setPassword("AdminPass123!");
        dto.setRol(Usuario.Role.ADMIN);
        return dto;
    }

    // /api/usuarios/registro endpoint tests
    @Nested
    @DisplayName("POST /registro")
    class RegistroUsuarioTests {
        
        @Test
        @WithAnonymousUser
        @DisplayName("Should register user successfully with valid data")
        void registrarUsuario_ValidData_Success() throws Exception {
            // Arrange
            UsuarioDTO requestDto = buildValidUsuarioDTO();
            UsuarioResponseDTO responseDto = new UsuarioResponseDTO(1L, 
                requestDto.getNombre(), 
                requestDto.getEmail(), 
                Usuario.Role.USER,
                LocalDateTime.now());
            
            when(usuarioService.registrarUsuario(any(UsuarioDTO.class)))
                .thenReturn(responseDto);

            // Act & Assert
            mockMvc.perform(post(BASE_URL + "/registro")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.nombre").value(requestDto.getNombre()))
                    .andExpect(jsonPath("$.email").value(requestDto.getEmail()))
                    .andExpect(jsonPath("$.rol").value("USER"))
                    .andExpect(jsonPath("$.fechaCreacion").exists());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Should return 400 when email is invalid")
        void registrarUsuario_InvalidEmail_BadRequest() throws Exception {
            // Arrange
            UsuarioDTO requestDto = buildValidUsuarioDTO();
            requestDto.setEmail("invalid-email");

            // Act & Assert
            mockMvc.perform(post(BASE_URL + "/registro")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Validation Error"))
                    .andExpect(jsonPath("$.details[0]").value(containsString("email")));
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Should return 400 when required fields are missing")
        void registrarUsuario_MissingRequiredFields_BadRequest() throws Exception {
            // Arrange
            UsuarioDTO requestDto = new UsuarioDTO();

            // Act & Assert
            mockMvc.perform(post(BASE_URL + "/registro")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Validation Error"))
                    .andExpect(jsonPath("$.details").isArray())
                    .andExpect(jsonPath("$.details", hasSize(greaterThan(0))));
        }
    }

    // /api/usuarios/admin/registro endpoint tests
    @Nested
    @DisplayName("POST /admin/registro")
    class RegistroAdminTests {
        
        @Test
        @DisplayName("Should register admin successfully with valid data when authenticated as ADMIN")
        @WithMockUser(roles = "ADMIN")
        void registrarAdmin_ValidData_AuthenticatedAsAdmin_Success() throws Exception {
            // Arrange
            UsuarioAdminDTO requestDto = buildValidUsuarioAdminDTO();
            UsuarioResponseDTO responseDto = new UsuarioResponseDTO(1L, 
                requestDto.getNombre(), 
                requestDto.getEmail(), 
                requestDto.getRol(),
                LocalDateTime.now());
            
            when(usuarioService.registrarUsuario(any(UsuarioAdminDTO.class)))
                .thenReturn(responseDto);

            // Act & Assert
            mockMvc.perform(post(BASE_URL + "/admin/registro")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.nombre").value(requestDto.getNombre()))
                    .andExpect(jsonPath("$.email").value(requestDto.getEmail()))
                    .andExpect(jsonPath("$.rol").value("ADMIN"));
        }

        @Test
        @DisplayName("Should return 403 when not authenticated as ADMIN")
        @WithMockUser(roles = "USER")
        void registrarAdmin_ValidData_NotAdmin_Forbidden() throws Exception {
            // Arrange
            UsuarioAdminDTO requestDto = buildValidUsuarioAdminDTO();

            // Act & Assert
            mockMvc.perform(post(BASE_URL + "/admin/registro")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isForbidden());
        }
    }

    // /api/usuarios/perfil endpoint tests
    @Nested
    @DisplayName("PUT /api/usuarios/perfil")
    class ActualizarPerfilTests {
        
        @Test
        @DisplayName("Should update profile successfully when authenticated")
        @WithMockUser(username = TEST_USER_EMAIL)
        void actualizarPerfil_ValidData_Authenticated_Success() throws Exception {
            // Arrange
            ActualizacionUsuarioDTO requestDto = new ActualizacionUsuarioDTO(
                "Updated Name", "updated@example.com");
            
            UsuarioResponseDTO responseDto = new UsuarioResponseDTO(1L, 
                requestDto.getNombre(), 
                requestDto.getNuevoEmail(), 
                Usuario.Role.USER,
                LocalDateTime.now());
            
            when(usuarioService.actualizarPerfil(anyString(), any(ActualizacionUsuarioDTO.class)))
                .thenReturn(responseDto);

            // Act & Assert
            mockMvc.perform(put(BASE_URL + "/perfil")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nombre").value(requestDto.getNombre()))
                    .andExpect(jsonPath("$.email").value(requestDto.getNuevoEmail()));
        }

        @Test
        @DisplayName("Should return 400 when update data is invalid")
        @WithMockUser
        void actualizarPerfil_InvalidData_BadRequest() throws Exception {
            // Arrange
            ActualizacionUsuarioDTO requestDto = new ActualizacionUsuarioDTO(
                "", "invalid-email");

            // Act & Assert
            mockMvc.perform(put(BASE_URL + "/perfil")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Validation Error"))
                    .andExpect(jsonPath("$.details", hasSize(2)));
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void actualizarPerfil_NotAuthenticated_Unauthorized() throws Exception {
            // Arrange
            ActualizacionUsuarioDTO requestDto = new ActualizacionUsuarioDTO(
                "New Name", "new@example.com");

            // Act & Assert
            mockMvc.perform(put(BASE_URL + "/perfil")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // /api/usuarios/password endpoint tests
    @Nested
    @DisplayName("PUT /api/usuarios/password")
    class CambiarContraseñaTests {
        
        @Test
        @DisplayName("Should change password successfully when authenticated")
        @WithMockUser(username = TEST_USER_EMAIL)
        void cambiarContraseña_ValidData_Authenticated_Success() throws Exception {
            // Arrange
            CambioPasswdDTO requestDto = new CambioPasswdDTO();
            requestDto.setCurrentPassword("CurrentPass123!");
            requestDto.setNewPassword("NewPass123!");
            requestDto.setConfirmPassword("NewPass123!");
            
            doNothing().when(usuarioService)
                .cambiarContraseña(anyString(), any(CambioPasswdDTO.class));

            // Act & Assert
            mockMvc.perform(put(BASE_URL + "/password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 400 when passwords don't match")
        @WithMockUser
        void cambiarContraseña_PasswordMismatch_BadRequest() throws Exception {
            // Arrange
            CambioPasswdDTO requestDto = new CambioPasswdDTO();
            requestDto.setCurrentPassword("CurrentPass123!");
            requestDto.setNewPassword("NewPass123!");
            requestDto.setConfirmPassword("DifferentPass123!");
            
            doThrow(new PasswordMismatchException("Las contraseñas no coinciden"))
                .when(usuarioService)
                .cambiarContraseña(anyString(), any(CambioPasswdDTO.class));

            // Act & Assert
            mockMvc.perform(put(BASE_URL + "/password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Las contraseñas no coinciden"));
        }

        @Test
        @DisplayName("Should return 400 when current password is incorrect")
        @WithMockUser
        void cambiarContraseña_IncorrectCurrentPassword_BadRequest() throws Exception {
            // Arrange
            CambioPasswdDTO requestDto = new CambioPasswdDTO();
            requestDto.setCurrentPassword("WrongPass123!");
            requestDto.setNewPassword("NewPass123!");
            requestDto.setConfirmPassword("NewPass123!");
            
            doThrow(new InvalidPasswordException("La contraseña actual es incorrecta"))
                .when(usuarioService)
                .cambiarContraseña(anyString(), any(CambioPasswdDTO.class));

            // Act & Assert
                mockMvc.perform(put(BASE_URL + "/password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("La contraseña actual es incorrecta"));
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void cambiarContraseña_NotAuthenticated_Unauthorized() throws Exception {
            // Arrange
            CambioPasswdDTO requestDto = new CambioPasswdDTO();
            requestDto.setCurrentPassword("CurrentPass123!");
            requestDto.setNewPassword("NewPass123!");
            requestDto.setConfirmPassword("NewPass123!");

            // Act & Assert
            mockMvc.perform(put(BASE_URL + "/password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // /api/usuarios/login endpoint tests
    @Nested
    @DisplayName("POST /login")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully with valid credentials")
        @WithAnonymousUser
        void login_ValidCredentials_Success() throws Exception {
            // Arrange
            LoginDTO loginDTO = new LoginDTO(TEST_USER_EMAIL, "ValidPassword123!");
            UsuarioResponseDTO responseDTO = new UsuarioResponseDTO(1L,
                "Test User",
                TEST_USER_EMAIL,
                Usuario.Role.USER,
                LocalDateTime.now());

            when(usuarioService.login(any(LoginDTO.class))).thenReturn(responseDTO);

            // Act & Assert
            mockMvc.perform(post(BASE_URL + "/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.nombre").value("Test User"))
                    .andExpect(jsonPath("$.email").value(TEST_USER_EMAIL))
                    .andExpect(jsonPath("$.rol").value("USER"))
                    .andExpect(jsonPath("$.fechaCreacion").exists());
        }

        @Test
        @DisplayName("Should return 401 when email does not exist")
        @WithAnonymousUser
        void login_EmailNotFound_Unauthorized() throws Exception {
            // Arrange
            LoginDTO loginDTO = new LoginDTO("nonexistent@example.com", "ValidPassword123!");
            when(usuarioService.login(any(LoginDTO.class)))
                .thenThrow(new ResourceNotFoundException("No existe ningún usuario con el email proporcionado"));

            // Act & Assert
            mockMvc.perform(post(BASE_URL + "/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginDTO)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("No existe ningún usuario con el email proporcionado"));
        }

        @Test
        @DisplayName("Should return 401 when password is incorrect")
        @WithAnonymousUser
        void login_InvalidPassword_Unauthorized() throws Exception {
            // Arrange
            LoginDTO loginDTO = new LoginDTO(TEST_USER_EMAIL, "WrongPassword123!");
            when(usuarioService.login(any(LoginDTO.class)))
                .thenThrow(new InvalidPasswordException("Contraseña incorrecta."));

            // Act & Assert
            mockMvc.perform(post(BASE_URL + "/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginDTO)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Contraseña incorrecta."));
        }

        @Test
        @DisplayName("Should return 400 when email is invalid")
        @WithAnonymousUser
        void login_InvalidEmail_BadRequest() throws Exception {
            // Arrange
            LoginDTO loginDTO = new LoginDTO("invalid-email", "ValidPassword123!");

            // Act & Assert
            mockMvc.perform(post(BASE_URL + "/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Validation Error"))
                    .andExpect(jsonPath("$.details[0]").value(containsString("email")));
        }

        @Test
        @DisplayName("Should return 400 when required fields are missing")
        @WithAnonymousUser
        void login_MissingFields_BadRequest() throws Exception {
            // Arrange
            LoginDTO loginDTO = new LoginDTO("", "");

            // Act & Assert
            mockMvc.perform(post(BASE_URL + "/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Validation Error"))
                    .andExpect(jsonPath("$.details", hasSize(2)));
        }
    }

    @Nested
    @DisplayName("GET /perfil")
    class ObtenerPerfilTests {
        
        @Test
        @DisplayName("Should get profile successfully when authenticated")
        @WithMockUser(username = TEST_USER_EMAIL)
        void obtenerPerfil_Authenticated_Success() throws Exception {
            // Arrange
            UsuarioResponseDTO responseDto = new UsuarioResponseDTO(1L,
                "Test User",
                TEST_USER_EMAIL,
                Usuario.Role.USER,
                LocalDateTime.now());
            
            when(usuarioService.obtenerPerfil(TEST_USER_EMAIL))
                .thenReturn(responseDto);

            // Act & Assert
            mockMvc.perform(get(BASE_URL + "/perfil"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.nombre").value("Test User"))
                    .andExpect(jsonPath("$.email").value(TEST_USER_EMAIL))
                    .andExpect(jsonPath("$.rol").value("USER"))
                    .andExpect(jsonPath("$.fechaCreacion").exists());
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void obtenerPerfil_NotAuthenticated_Unauthorized() throws Exception {
            // Act & Assert
            mockMvc.perform(get(BASE_URL + "/perfil"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /admin/listar")
    class ListarUsuariosTests {
        
        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should list users with pagination when authenticated as admin")
        void listarUsuarios_AuthenticatedAsAdmin_Success() throws Exception {
            // Arrange
            List<UsuarioResponseDTO> usuarios = Arrays.asList(
                new UsuarioResponseDTO(1L, "User 1", "user1@example.com", Usuario.Role.USER, LocalDateTime.now()),
                new UsuarioResponseDTO(2L, "User 2", "user2@example.com", Usuario.Role.ADMIN, LocalDateTime.now())
            );
            Page<UsuarioResponseDTO> page = new PageImpl<>(usuarios);

            when(usuarioService.listarUsuarios(0, 10, "email", "ASC")).thenReturn(page);

            // Act & Assert
            mockMvc.perform(get("/api/usuarios/admin/listar")
                    .param("page", "0")
                    .param("size", "10")
                    .param("sort", "email")
                    .param("direction", "ASC"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.content[0].nombre").value("User 1"))
                    .andExpect(jsonPath("$.content[0].email").value("user1@example.com"))
                    .andExpect(jsonPath("$.content[0].rol").value("USER"))
                    .andExpect(jsonPath("$.content[1].id").value(2))
                    .andExpect(jsonPath("$.content[1].nombre").value("User 2"))
                    .andExpect(jsonPath("$.content[1].email").value("user2@example.com"))
                    .andExpect(jsonPath("$.content[1].rol").value("ADMIN"))
                    .andExpect(jsonPath("$.totalElements").value(2));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should handle empty result with pagination")
        void listarUsuarios_EmptyResult_Success() throws Exception {
            // Arrange
            Page<UsuarioResponseDTO> emptyPage = new PageImpl<>(Collections.emptyList());
            when(usuarioService.listarUsuarios(0, 10, "email", "ASC")).thenReturn(emptyPage);

            // Act & Assert
            mockMvc.perform(get("/api/usuarios/admin/listar")
                    .param("page", "0")
                    .param("size", "10")
                    .param("sort", "email")
                    .param("direction", "ASC"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("Should return 403 when authenticated as regular user")
        @WithMockUser(roles = "USER")
        void listarUsuarios_AuthenticatedAsUser_Forbidden() throws Exception {
            // Act & Assert
            mockMvc.perform(get(BASE_URL + "/admin/listar"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void listarUsuarios_NotAuthenticated_Unauthorized() throws Exception {
            // Act & Assert
            mockMvc.perform(get(BASE_URL + "/admin/listar"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PUT /admin/change-role")
    class CambiarRolTests {
        
        @Test
        @DisplayName("Should change user role when authenticated as admin")
        @WithMockUser(roles = "ADMIN")
        void cambiarRol_AuthenticatedAsAdmin_Success() throws Exception {
            // Arrange
            Long userId = 1L;
            Usuario.Role newRole = Usuario.Role.ADMIN;
            
            doNothing().when(usuarioService).cambiarRol(userId, newRole);

            // Act & Assert
            mockMvc.perform(put(BASE_URL + "/admin/change-role")
                    .param("userId", userId.toString())
                    .param("newRole", newRole.toString()))
                    .andExpect(status().isOk());
            
            verify(usuarioService).cambiarRol(userId, newRole);
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        @WithMockUser(roles = "ADMIN")
        void cambiarRol_UserNotFound_NotFound() throws Exception {
            // Arrange
            Long userId = 999L;
            Usuario.Role newRole = Usuario.Role.ADMIN;
            
            doThrow(new ResourceNotFoundException("No existe ningún usuario con el ID proporcionado"))
                .when(usuarioService).cambiarRol(userId, newRole);

            // Act & Assert
            mockMvc.perform(put(BASE_URL + "/admin/change-role")
                    .param("userId", userId.toString())
                    .param("newRole", newRole.toString()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message")
                        .value("No existe ningún usuario con el ID proporcionado"));
        }

        @Test
        @DisplayName("Should return 400 when role is invalid")
        @WithMockUser(roles = "ADMIN")
        void cambiarRol_InvalidRole_BadRequest() throws Exception {
            // Arrange
            Long userId = 1L;
            
            // Act & Assert
            mockMvc.perform(put(BASE_URL + "/admin/change-role")
                    .param("userId", userId.toString())
                    .param("newRole", "INVALID_ROLE"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 403 when authenticated as regular user")
        @WithMockUser(roles = "USER")
        void cambiarRol_AuthenticatedAsUser_Forbidden() throws Exception {
            // Arrange
            Long userId = 1L;
            Usuario.Role newRole = Usuario.Role.ADMIN;

            // Act & Assert
            mockMvc.perform(put(BASE_URL + "/admin/change-role")
                    .param("userId", userId.toString())
                    .param("newRole", newRole.toString()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void cambiarRol_NotAuthenticated_Unauthorized() throws Exception {
            // Arrange
            Long userId = 1L;
            Usuario.Role newRole = Usuario.Role.ADMIN;

            // Act & Assert
            mockMvc.perform(put(BASE_URL + "/admin/change-role")
                    .param("userId", userId.toString())
                    .param("newRole", newRole.toString()))
                    .andExpect(status().isUnauthorized());
        }
    }

    private UserDetails mockUser(String username, String... roles) {
        return User.builder()
                .username(username)
                .password("password")
                .roles(roles)
                .build();
    }

    @BeforeEach
    void setup() {
        // Configure UserDetailsService to return the mock users
        when(userDetailsService.loadUserByUsername(TEST_USER_EMAIL))
            .thenReturn(mockUser(TEST_USER_EMAIL, "USER"));
        when(userDetailsService.loadUserByUsername(TEST_ADMIN_EMAIL))
            .thenReturn(mockUser(TEST_ADMIN_EMAIL, "ADMIN"));
    }
}