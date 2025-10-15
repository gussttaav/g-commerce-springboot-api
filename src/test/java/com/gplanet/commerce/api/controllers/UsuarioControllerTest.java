package com.gplanet.commerce.api.controllers;

import static com.gplanet.commerce.api.testutils.ApiErrorResultMatchers.errorDetailContains;
import static com.gplanet.commerce.api.testutils.ApiErrorResultMatchers.errorDetailsAsArray;
import static com.gplanet.commerce.api.testutils.ApiErrorResultMatchers.errorDetailsIsNotEmpty;
import static com.gplanet.commerce.api.testutils.ApiErrorResultMatchers.errorMessage;
import static com.gplanet.commerce.api.testutils.ApiErrorResultMatchers.validationError;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_USUARIOS;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_USUARIOS_ADMIN_CHANGE_ROLE;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_USUARIOS_ME;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gplanet.commerce.api.configs.TestSecurityConfig;
import com.gplanet.commerce.api.dtos.usuario.ActualizacionUsuarioDTO;
import com.gplanet.commerce.api.dtos.usuario.UsuarioDTO;
import com.gplanet.commerce.api.dtos.usuario.UsuarioResponseDTO;
import com.gplanet.commerce.api.entities.Usuario;
import com.gplanet.commerce.api.exceptions.InvalidPasswordException;
import com.gplanet.commerce.api.exceptions.PasswordMismatchException;
import com.gplanet.commerce.api.exceptions.ResourceNotFoundException;
import com.gplanet.commerce.api.services.UsuarioDetallesService;
import com.gplanet.commerce.api.services.UsuarioService;

@WebMvcTest(UsuarioController.class)
@Import(TestSecurityConfig.class)
class UsuarioControllerTest {

    private static final String USER_ROLE = "USER";
    private static final String TEST_USER_NAME = "Test User";
    private static final String TEST_USER_EMAIL = "test@example.com";
    private static final String TEST_ADMIN_EMAIL = "admin@example.com";
    private static final String SORT_FIELD_EMAIL = "email";
    private static final String SORT_DIRECTION_ASC = "ASC";
    private static final String USER_ID_PARAM = "userId";
    private static final String NEW_ROLE_PARAM = "newRole";
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private UsuarioDetallesService usuarioDetallesService;

    private UsuarioDTO buildValidUsuarioDTO() {
        return new UsuarioDTO(TEST_USER_NAME, TEST_USER_EMAIL, "Password123!");
    }

    private UsuarioDTO buildValidUsuarioAdminDTO() {
        return new UsuarioDTO("Admin User", TEST_ADMIN_EMAIL, "AdminPass123!", Usuario.Role.ADMIN);
    }

    public static ResultMatcher userId(int id) throws Exception {
        return jsonPath("$.id").value(id);
    }

    public static ResultMatcher userName(String name) throws Exception {
        return jsonPath("$.nombre").value(name);
    }

    public static ResultMatcher userEmail(String email) throws Exception {
        return jsonPath("$.email").value(email);
    }

    public static ResultMatcher userRol(String rol) throws Exception {
        return jsonPath("$.rol").value(rol);
    }

    @BeforeEach
    void setUp() {
        // Configure ObjectMapper for consistent decimal formatting
        objectMapper.configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Nested
    @DisplayName("POST /api/usuarios/")
    class RegistroUsuarioTests {
        
        @Test
        @WithAnonymousUser
        @DisplayName("Should register user successfully with valid data")
        void registrarUsuarioValidDataSuccess() throws Exception {
            // Arrange
            UsuarioDTO requestDto = buildValidUsuarioDTO();
            UsuarioResponseDTO responseDto = new UsuarioResponseDTO(1L, 
                requestDto.nombre(), 
                requestDto.email(), 
                Usuario.Role.USER,
                LocalDateTime.now());
            
            when(usuarioService.registrarUsuario(any(UsuarioDTO.class)))
                .thenReturn(responseDto);

            // Act & Assert
            mockMvc.perform(post(ENDPOINT_USUARIOS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(userId(1))
                .andExpect(userName(requestDto.nombre()))
                .andExpect(userEmail(requestDto.email()))
                .andExpect(userRol(USER_ROLE))
                .andExpect(jsonPath("$.fechaCreacion").exists());

            verify(usuarioService).registrarUsuario(any(UsuarioDTO.class));
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Should return 400 when email is invalid")
        void registrarUsuarioInvalidEmailBadRequest() throws Exception {
            // Arrange
            UsuarioDTO requestDto = new UsuarioDTO(TEST_USER_NAME, "invalid-email", "Password123!");

            // Act & Assert
            mockMvc.perform(post(ENDPOINT_USUARIOS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(validationError())
                .andExpect(errorDetailContains("email"));
            
            verifyNoInteractions(usuarioService);
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Should return 400 when required fields are missing")
        void registrarUsuarioMissingRequiredFieldsBadRequest() throws Exception {
            // Arrange
            UsuarioDTO requestDto = new UsuarioDTO("", "", "");

            // Act & Assert
            mockMvc.perform(post(ENDPOINT_USUARIOS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(validationError())
                .andExpect(errorDetailsAsArray())
                .andExpect(errorDetailsIsNotEmpty());
            
            verifyNoInteractions(usuarioService);
        }

        @Test
        @DisplayName("Should register admin successfully with valid data when authenticated as ADMIN")
        @WithMockUser(username = TEST_ADMIN_EMAIL, roles = "ADMIN")
        void registrarAdminValidDataAuthenticatedAsAdminSuccess() throws Exception {
            // Arrange
            UsuarioDTO requestDto = buildValidUsuarioAdminDTO();
            UsuarioResponseDTO responseDto = new UsuarioResponseDTO(1L, 
                requestDto.nombre(), 
                requestDto.email(), 
                requestDto.rol(),
                LocalDateTime.now());
            
            when(usuarioService.registrarUsuario(any(UsuarioDTO.class)))
                .thenReturn(responseDto);

            // Act & Assert
            mockMvc.perform(post(ENDPOINT_USUARIOS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(userId(1))
                .andExpect(userName(requestDto.nombre()))
                .andExpect(userEmail(requestDto.email()))
                .andExpect(userRol("ADMIN"));

            verify(usuarioService).registrarUsuario(any(UsuarioDTO.class));
        }

        @Test
        @DisplayName("Should return 403 when not authenticated as ADMIN")
        @WithMockUser(username = TEST_USER_EMAIL, roles = USER_ROLE)
        void registrarAdminValidDataNotAdminForbidden() throws Exception {
            // Arrange
            UsuarioDTO requestDto = buildValidUsuarioAdminDTO();

            // Act & Assert
            mockMvc.perform(post(ENDPOINT_USUARIOS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isForbidden());
            
            verifyNoInteractions(usuarioService);
        }
    }

    // /api/usuarios/me endpoint tests
    @Nested
    @DisplayName("PATCH /me")
    class ActualizarPerfilTests {
        
        @Test
        @DisplayName("Should update profile successfully when authenticated")
        @WithMockUser(username = TEST_USER_EMAIL)
        void actualizarPerfilValidDataAuthenticatedSuccess() throws Exception {
            // Arrange
            ActualizacionUsuarioDTO requestDto = new ActualizacionUsuarioDTO(
                "Updated Name", "updated@example.com");
            
            UsuarioResponseDTO responseDto = new UsuarioResponseDTO(1L, 
                requestDto.nombre(), 
                requestDto.nuevoEmail(), 
                Usuario.Role.USER,
                LocalDateTime.now());
            
            when(usuarioService.actualizarPerfil(eq(TEST_USER_EMAIL), any(ActualizacionUsuarioDTO.class)))
                .thenReturn(responseDto);

            // Act & Assert
            mockMvc.perform(patch(ENDPOINT_USUARIOS_ME)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(userName(requestDto.nombre()))
                .andExpect(userEmail(requestDto.nuevoEmail()));

            verify(usuarioService).actualizarPerfil(eq(TEST_USER_EMAIL), any(ActualizacionUsuarioDTO.class));
        }

        @Test
        @DisplayName("Should return 400 when update data is invalid")
        @WithMockUser(username = TEST_USER_EMAIL)
        void actualizarPerfilInvalidDataBadRequest() throws Exception {
            // Arrange
            ActualizacionUsuarioDTO requestDto = new ActualizacionUsuarioDTO(
                "", "invalid-email");

            // Act & Assert
            mockMvc.perform(patch(ENDPOINT_USUARIOS_ME)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(validationError())
                .andExpect(errorDetailsIsNotEmpty());
            
            verifyNoInteractions(usuarioService);
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Should return 401 when not authenticated")
        void actualizarPerfilNotAuthenticatedUnauthorized() throws Exception {
            // Arrange
            ActualizacionUsuarioDTO requestDto = new ActualizacionUsuarioDTO(
                "New Name", "new@example.com");

            // Act & Assert
            mockMvc.perform(patch(ENDPOINT_USUARIOS_ME)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isUnauthorized());
            
            verifyNoInteractions(usuarioService);
        }

        @Test
        @DisplayName("Should change password successfully when authenticated")
        @WithMockUser(username = TEST_USER_EMAIL)
        void cambiarContrasenaValidDataAuthenticatedSuccess() throws Exception {
            // Arrange
            ActualizacionUsuarioDTO requestDto = new ActualizacionUsuarioDTO(
                TEST_USER_NAME, "NewPass123!", "NewPass123!");

            UsuarioResponseDTO responseDto = new UsuarioResponseDTO(1L, 
                TEST_USER_NAME, 
                TEST_USER_EMAIL, 
                Usuario.Role.USER,
                LocalDateTime.now());
            
            when(usuarioService.actualizarPerfil(eq(TEST_USER_EMAIL), any(ActualizacionUsuarioDTO.class)))
                .thenReturn(responseDto);

            // Act & Assert
            mockMvc.perform(patch(ENDPOINT_USUARIOS_ME)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk());

            verify(usuarioService).actualizarPerfil(eq(TEST_USER_EMAIL), any(ActualizacionUsuarioDTO.class));
        }

        @Test
        @DisplayName("Should return 400 when passwords don't match")
        @WithMockUser(username = TEST_USER_EMAIL)
        void cambiarContrasenaPasswordMismatchBadRequest() throws Exception {
            // Arrange
            ActualizacionUsuarioDTO requestDto = new ActualizacionUsuarioDTO(
                TEST_USER_NAME, "Pass123!", "DifferentPass123!");
            
            doThrow(new PasswordMismatchException("Las contraseñas no coinciden"))
                .when(usuarioService)
                .actualizarPerfil(eq(TEST_USER_EMAIL), any(ActualizacionUsuarioDTO.class));

            // Act & Assert
            mockMvc.perform(patch(ENDPOINT_USUARIOS_ME)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(errorMessage("Las contraseñas no coinciden"));

            verify(usuarioService).actualizarPerfil(eq(TEST_USER_EMAIL), any(ActualizacionUsuarioDTO.class));
        }

        @Test
        @DisplayName("Should return 401 when current password is incorrect")
        @WithMockUser(username = TEST_USER_EMAIL)
        void cambiarContrasenaIncorrectCurrentPasswordUnauthorized() throws Exception {
            // Arrange
            ActualizacionUsuarioDTO requestDto = new ActualizacionUsuarioDTO(
                "WrongPass123!", "NewPass234!", "NewPass23!4");
            
            doThrow(new InvalidPasswordException("La contraseña actual es incorrecta"))
                .when(usuarioService)
                .actualizarPerfil(eq(TEST_USER_EMAIL), any(ActualizacionUsuarioDTO.class));

            // Act & Assert
            mockMvc.perform(patch(ENDPOINT_USUARIOS_ME)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isUnauthorized())
                .andExpect(errorMessage("La contraseña actual es incorrecta"));

            verify(usuarioService).actualizarPerfil(eq(TEST_USER_EMAIL), any(ActualizacionUsuarioDTO.class));
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Should return 401 when not authenticated")
        void cambiarContrasenaNotAuthenticatedUnauthorized() throws Exception {
            // Arrange
            ActualizacionUsuarioDTO requestDto = new ActualizacionUsuarioDTO(
                "CurrentPass123!", "Psswd123!", "Psswd123!");

            // Act & Assert
            mockMvc.perform(patch(ENDPOINT_USUARIOS_ME)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isUnauthorized());
            
            verifyNoInteractions(usuarioService);
        }
    }


    @Nested
    @DisplayName("GET /perfil")
    class ObtenerPerfilTests {
        
        @Test
        @DisplayName("Should get profile successfully when authenticated")
        @WithMockUser(username = TEST_USER_EMAIL)
        void obtenerPerfilAuthenticatedSuccess() throws Exception {
            // Arrange
            UsuarioResponseDTO responseDto = new UsuarioResponseDTO(1L,
                TEST_USER_NAME,
                TEST_USER_EMAIL,
                Usuario.Role.USER,
                LocalDateTime.now());
            
            when(usuarioService.obtenerPerfil(TEST_USER_EMAIL))
                .thenReturn(responseDto);

            // Act & Assert
            mockMvc.perform(get(ENDPOINT_USUARIOS_ME))
                .andExpect(status().isOk())
                .andExpect(userId(1))
                .andExpect(userName(TEST_USER_NAME))
                .andExpect(userEmail(TEST_USER_EMAIL))
                .andExpect(userRol(USER_ROLE))
                .andExpect(jsonPath("$.fechaCreacion").exists());

            verify(usuarioService).obtenerPerfil(TEST_USER_EMAIL);
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Should return 401 when not authenticated")
        void obtenerPerfilNotAuthenticatedUnauthorized() throws Exception {
            // Act & Assert
            mockMvc.perform(get(ENDPOINT_USUARIOS_ME))
                .andExpect(status().isUnauthorized());
            
            verifyNoInteractions(usuarioService);
        }
    }

    @Nested
    @DisplayName("GET /api/usuarios/")
    class ListarUsuariosTests {
        
        @Test
        @WithMockUser(username = TEST_ADMIN_EMAIL, roles = "ADMIN")
        @DisplayName("Should list users with pagination when authenticated as admin")
        void listarUsuariosAuthenticatedAsAdminSuccess() throws Exception {
            // Arrange
            List<UsuarioResponseDTO> usuarios = Arrays.asList(
                new UsuarioResponseDTO(1L, "User 1", "user1@example.com", Usuario.Role.USER, LocalDateTime.now()),
                new UsuarioResponseDTO(2L, "User 2", "user2@example.com", Usuario.Role.ADMIN, LocalDateTime.now())
            );
            Page<UsuarioResponseDTO> page = new PageImpl<>(usuarios);

            when(usuarioService.listarUsuarios(0, 10, SORT_FIELD_EMAIL, SORT_DIRECTION_ASC)).thenReturn(page);

            // Act & Assert
            mockMvc.perform(get(ENDPOINT_USUARIOS)
                    .param("page", "0")
                    .param("size", "10")
                    .param("sort", SORT_FIELD_EMAIL)
                    .param("direction", SORT_DIRECTION_ASC))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].nombre").value("User 1"))
                .andExpect(jsonPath("$.content[0].email").value("user1@example.com"))
                .andExpect(jsonPath("$.content[0].rol").value(USER_ROLE))
                .andExpect(jsonPath("$.content[1].id").value(2))
                .andExpect(jsonPath("$.content[1].nombre").value("User 2"))
                .andExpect(jsonPath("$.content[1].email").value("user2@example.com"))
                .andExpect(jsonPath("$.content[1].rol").value("ADMIN"))
                .andExpect(jsonPath("$.totalElements").value(2));

            verify(usuarioService).listarUsuarios(0, 10, SORT_FIELD_EMAIL, SORT_DIRECTION_ASC);
        }

        @Test
        @WithMockUser(username = TEST_ADMIN_EMAIL, roles = "ADMIN")
        @DisplayName("Should handle empty result with pagination")
        void listarUsuariosEmptyResultSuccess() throws Exception {
            // Arrange
            Page<UsuarioResponseDTO> emptyPage = new PageImpl<>(Collections.emptyList());
            when(usuarioService.listarUsuarios(0, 10, SORT_FIELD_EMAIL, SORT_DIRECTION_ASC)).thenReturn(emptyPage);

            // Act & Assert
            mockMvc.perform(get(ENDPOINT_USUARIOS)
                    .param("page", "0")
                    .param("size", "10")
                    .param("sort", SORT_FIELD_EMAIL)
                    .param("direction", SORT_DIRECTION_ASC))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));

            verify(usuarioService).listarUsuarios(0, 10, SORT_FIELD_EMAIL, SORT_DIRECTION_ASC);
        }

        @Test
        @DisplayName("Should return 403 when authenticated as regular user")
        @WithMockUser(username = TEST_USER_EMAIL, roles = USER_ROLE)
        void listarUsuariosAuthenticatedAsUserForbidden() throws Exception {
            // Act & Assert
            mockMvc.perform(get(ENDPOINT_USUARIOS))
                .andExpect(status().isForbidden());
            
            verifyNoInteractions(usuarioService);
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Should return 401 when not authenticated")
        void listarUsuariosNotAuthenticatedUnauthorized() throws Exception {
            // Act & Assert
            mockMvc.perform(get(ENDPOINT_USUARIOS))
                .andExpect(status().isUnauthorized());
            
            verifyNoInteractions(usuarioService);
        }
    }

    @Nested
    @DisplayName("PUT /admin/change-role")
    class CambiarRolTests {
        
        @Test
        @DisplayName("Should change user role when authenticated as admin")
        @WithMockUser(username = TEST_ADMIN_EMAIL, roles = "ADMIN")
        void cambiarRolAuthenticatedAsAdminSuccess() throws Exception {
            // Arrange
            Long userId = 1L;
            Usuario.Role newRole = Usuario.Role.ADMIN;
            
            doNothing().when(usuarioService).cambiarRol(userId, newRole);

            // Act & Assert
            mockMvc.perform(put(ENDPOINT_USUARIOS_ADMIN_CHANGE_ROLE)
                    .param(USER_ID_PARAM, userId.toString())
                    .param(NEW_ROLE_PARAM, newRole.toString()))
                .andExpect(status().isOk());
            
            verify(usuarioService).cambiarRol(userId, newRole);
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        @WithMockUser(username = TEST_ADMIN_EMAIL, roles = "ADMIN")
        void cambiarRolUserNotFoundNotFound() throws Exception {
            // Arrange
            final Long userId = 999L;
            Usuario.Role newRole = Usuario.Role.ADMIN;
            
            doThrow(new ResourceNotFoundException("No existe ningún usuario con el ID proporcionado"))
                .when(usuarioService).cambiarRol(userId, newRole);

            // Act & Assert
            mockMvc.perform(put(ENDPOINT_USUARIOS_ADMIN_CHANGE_ROLE)
                    .param(USER_ID_PARAM, userId.toString())
                    .param(NEW_ROLE_PARAM, newRole.toString()))
                .andExpect(status().isNotFound())
                .andExpect(errorMessage("No existe ningún usuario con el ID proporcionado"));

            verify(usuarioService).cambiarRol(userId, newRole);
        }

        @Test
        @DisplayName("Should return 400 when role is invalid")
        @WithMockUser(username = TEST_ADMIN_EMAIL, roles = "ADMIN")
        void cambiarRolInvalidRoleBadRequest() throws Exception {
            // Arrange
            Long userId = 1L;
            
            // Act & Assert
            mockMvc.perform(put(ENDPOINT_USUARIOS_ADMIN_CHANGE_ROLE)
                    .param(USER_ID_PARAM, userId.toString())
                    .param(NEW_ROLE_PARAM, "INVALID_ROLE"))
                .andExpect(status().isBadRequest());
            
            verifyNoInteractions(usuarioService);
        }

        @Test
        @DisplayName("Should return 403 when authenticated as regular user")
        @WithMockUser(username = TEST_USER_EMAIL, roles = USER_ROLE)
        void cambiarRolAuthenticatedAsUserForbidden() throws Exception {
            // Arrange
            Long userId = 1L;
            Usuario.Role newRole = Usuario.Role.ADMIN;

            // Act & Assert
            mockMvc.perform(put(ENDPOINT_USUARIOS_ADMIN_CHANGE_ROLE)
                    .param(USER_ID_PARAM, userId.toString())
                    .param(NEW_ROLE_PARAM, newRole.toString()))
                .andExpect(status().isForbidden());
            
            verifyNoInteractions(usuarioService);
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Should return 401 when not authenticated")
        void cambiarRolNotAuthenticatedUnauthorized() throws Exception {
            // Arrange
            Long userId = 1L;
            Usuario.Role newRole = Usuario.Role.ADMIN;

            // Act & Assert
            mockMvc.perform(put(ENDPOINT_USUARIOS_ADMIN_CHANGE_ROLE)
                    .param(USER_ID_PARAM, userId.toString())
                    .param(NEW_ROLE_PARAM, newRole.toString()))
                .andExpect(status().isUnauthorized());
            
            verifyNoInteractions(usuarioService);
        }
    }
}
