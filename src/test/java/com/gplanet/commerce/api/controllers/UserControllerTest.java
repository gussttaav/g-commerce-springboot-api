package com.gplanet.commerce.api.controllers;

import static com.gplanet.commerce.api.testutils.ApiErrorResultMatchers.errorDetailContains;
import static com.gplanet.commerce.api.testutils.ApiErrorResultMatchers.errorDetailsAsArray;
import static com.gplanet.commerce.api.testutils.ApiErrorResultMatchers.errorDetailsIsNotEmpty;
import static com.gplanet.commerce.api.testutils.ApiErrorResultMatchers.errorMessage;
import static com.gplanet.commerce.api.testutils.ApiErrorResultMatchers.validationError;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_USUARIOS;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gplanet.commerce.api.configs.TestSecurityConfig;
import com.gplanet.commerce.api.dtos.user.UserDTO;
import com.gplanet.commerce.api.dtos.user.UserResponseDTO;
import com.gplanet.commerce.api.dtos.user.UserRoleUpdateDTO;
import com.gplanet.commerce.api.dtos.user.UserUpdateDTO;
import com.gplanet.commerce.api.entities.User;
import com.gplanet.commerce.api.exceptions.InvalidPasswordException;
import com.gplanet.commerce.api.exceptions.PasswordMismatchException;
import com.gplanet.commerce.api.exceptions.ResourceNotFoundException;
import com.gplanet.commerce.api.services.CustomUserDetailsService;
import com.gplanet.commerce.api.services.UserService;

@WebMvcTest(UserController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class UserControllerTest {

    private static final String USER_ROLE = "USER";
    private static final String TEST_USER_NAME = "Test User";
    private static final String TEST_USER_EMAIL = "test@example.com";
    private static final Long TEST_USER_ID = 1L;
    // Access tokens identify the user by ID, which is the name of the authentication
    private static final String TEST_USER_AUTH_NAME = "1";
    private static final String TEST_ADMIN_EMAIL = "admin@example.com";
    private static final String SORT_FIELD_EMAIL = "email";
    private static final String SORT_DIRECTION_ASC = "ASC";
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private String createChangeRoleRequest(Long userId) {
        return String.format("%s/%d/role", ENDPOINT_USUARIOS, userId);
    }

    private UserDTO buildValidUsuarioDTO() {
        return new UserDTO(TEST_USER_NAME, TEST_USER_EMAIL, "Password123!");
    }

    private UserDTO buildValidUsuarioAdminDTO() {
        return new UserDTO("Admin User", TEST_ADMIN_EMAIL, "AdminPass123!", User.Role.ADMIN);
    }

    public static ResultMatcher userId(int id) throws Exception {
        return jsonPath("$.id").value(id);
    }

    public static ResultMatcher userName(String name) throws Exception {
        return jsonPath("$.name").value(name);
    }

    public static ResultMatcher userEmail(String email) throws Exception {
        return jsonPath("$.email").value(email);
    }

    public static ResultMatcher userRol(String role) throws Exception {
        return jsonPath("$.role").value(role);
    }

    @BeforeEach
    void setUp() {
        // Configure ObjectMapper for consistent decimal formatting
        objectMapper.configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Nested
    @DisplayName("POST /api/users/")
    class RegistroUsuarioTests {
        
        @Test
        @WithAnonymousUser
        @DisplayName("Should register user successfully with valid data")
        void registerUserValidDataSuccess() throws Exception {
            // Arrange
            UserDTO requestDto = buildValidUsuarioDTO();
            UserResponseDTO responseDto = new UserResponseDTO(1L, 
                requestDto.name(), 
                requestDto.email(), 
                User.Role.USER,
                LocalDateTime.now());
            
            when(userService.registerUser(any(UserDTO.class)))
                .thenReturn(responseDto);

            // Act & Assert
            mockMvc.perform(post(ENDPOINT_USUARIOS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(userId(1))
                .andExpect(userName(requestDto.name()))
                .andExpect(userEmail(requestDto.email()))
                .andExpect(userRol(USER_ROLE))
                .andExpect(jsonPath("$.creationDate").exists());

            verify(userService).registerUser(any(UserDTO.class));
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Should return 400 when email is invalid")
        void registerUserInvalidEmailBadRequest() throws Exception {
            // Arrange
            UserDTO requestDto = new UserDTO(TEST_USER_NAME, "invalid-email", "Password123!");

            // Act & Assert
            mockMvc.perform(post(ENDPOINT_USUARIOS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(validationError())
                .andExpect(errorDetailContains("email"));
            
            verifyNoInteractions(userService);
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Should return 400 when required fields are missing")
        void registerUserMissingRequiredFieldsBadRequest() throws Exception {
            // Arrange
            UserDTO requestDto = new UserDTO("", "", "");

            // Act & Assert
            mockMvc.perform(post(ENDPOINT_USUARIOS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(validationError())
                .andExpect(errorDetailsAsArray())
                .andExpect(errorDetailsIsNotEmpty());
            
            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("Should register admin successfully with valid data when authenticated as ADMIN")
        @WithMockUser(username = TEST_ADMIN_EMAIL, roles = "ADMIN")
        void registrarAdminValidDataAuthenticatedAsAdminSuccess() throws Exception {
            // Arrange
            UserDTO requestDto = buildValidUsuarioAdminDTO();
            UserResponseDTO responseDto = new UserResponseDTO(1L, 
                requestDto.name(), 
                requestDto.email(), 
                requestDto.role(),
                LocalDateTime.now());
            
            when(userService.registerUser(any(UserDTO.class)))
                .thenReturn(responseDto);

            // Act & Assert
            mockMvc.perform(post(ENDPOINT_USUARIOS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(userId(1))
                .andExpect(userName(requestDto.name()))
                .andExpect(userEmail(requestDto.email()))
                .andExpect(userRol("ADMIN"));

            verify(userService).registerUser(any(UserDTO.class));
        }

        @Test
        @DisplayName("Should return 403 when not authenticated as ADMIN")
        @WithMockUser(username = TEST_USER_EMAIL, roles = USER_ROLE)
        void registrarAdminValidDataNotAdminForbidden() throws Exception {
            // Arrange
            UserDTO requestDto = buildValidUsuarioAdminDTO();

            // Act & Assert
            mockMvc.perform(post(ENDPOINT_USUARIOS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isForbidden());
            
            verifyNoInteractions(userService);
        }
    }

    // /api/users/me endpoint tests
    @Nested
    @DisplayName("PATCH /me")
    class ActualizarPerfilTests {
        
        @Test
        @DisplayName("Should update profile successfully when authenticated")
        @WithMockUser(username = TEST_USER_AUTH_NAME)
        void updateProfileValidDataAuthenticatedSuccess() throws Exception {
            // Arrange
            UserUpdateDTO requestDto = new UserUpdateDTO(
                "Updated Name", "updated@example.com");
            
            UserResponseDTO responseDto = new UserResponseDTO(1L, 
                requestDto.name(), 
                requestDto.newEmail(), 
                User.Role.USER,
                LocalDateTime.now());
            
            when(userService.updateProfile(eq(TEST_USER_ID), any(UserUpdateDTO.class)))
                .thenReturn(responseDto);

            // Act & Assert
            mockMvc.perform(patch(ENDPOINT_USUARIOS_ME)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(userName(requestDto.name()))
                .andExpect(userEmail(requestDto.newEmail()));

            verify(userService).updateProfile(eq(TEST_USER_ID), any(UserUpdateDTO.class));
        }

        @Test
        @DisplayName("Should return 400 when update data is invalid")
        @WithMockUser(username = TEST_USER_AUTH_NAME)
        void updateProfileInvalidDataBadRequest() throws Exception {
            // Arrange
            UserUpdateDTO requestDto = new UserUpdateDTO(
                "", "invalid-email");

            // Act & Assert
            mockMvc.perform(patch(ENDPOINT_USUARIOS_ME)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(validationError())
                .andExpect(errorDetailsIsNotEmpty());
            
            verifyNoInteractions(userService);
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Should return 401 when not authenticated")
        void updateProfileNotAuthenticatedUnauthorized() throws Exception {
            // Arrange
            UserUpdateDTO requestDto = new UserUpdateDTO(
                "New Name", "new@example.com");

            // Act & Assert
            mockMvc.perform(patch(ENDPOINT_USUARIOS_ME)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isUnauthorized());
            
            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("Should change password successfully when authenticated")
        @WithMockUser(username = TEST_USER_AUTH_NAME)
        void cambiarContrasenaValidDataAuthenticatedSuccess() throws Exception {
            // Arrange
            UserUpdateDTO requestDto = new UserUpdateDTO(
                TEST_USER_NAME, "NewPass123!", "NewPass123!");

            UserResponseDTO responseDto = new UserResponseDTO(1L, 
                TEST_USER_NAME, 
                TEST_USER_EMAIL, 
                User.Role.USER,
                LocalDateTime.now());
            
            when(userService.updateProfile(eq(TEST_USER_ID), any(UserUpdateDTO.class)))
                .thenReturn(responseDto);

            // Act & Assert
            mockMvc.perform(patch(ENDPOINT_USUARIOS_ME)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk());

            verify(userService).updateProfile(eq(TEST_USER_ID), any(UserUpdateDTO.class));
        }

        @Test
        @DisplayName("Should return 400 when passwords don't match")
        @WithMockUser(username = TEST_USER_AUTH_NAME)
        void cambiarContrasenaPasswordMismatchBadRequest() throws Exception {
            // Arrange
            UserUpdateDTO requestDto = new UserUpdateDTO(
                TEST_USER_NAME, "Pass123!", "DifferentPass123!");
            
            doThrow(new PasswordMismatchException("Las contraseñas no coinciden"))
                .when(userService)
                .updateProfile(eq(TEST_USER_ID), any(UserUpdateDTO.class));

            // Act & Assert
            mockMvc.perform(patch(ENDPOINT_USUARIOS_ME)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(errorMessage("Las contraseñas no coinciden"));

            verify(userService).updateProfile(eq(TEST_USER_ID), any(UserUpdateDTO.class));
        }

        @Test
        @DisplayName("Should return 401 when current password is incorrect")
        @WithMockUser(username = TEST_USER_AUTH_NAME)
        void cambiarContrasenaIncorrectCurrentPasswordUnauthorized() throws Exception {
            // Arrange
            UserUpdateDTO requestDto = new UserUpdateDTO(
                "WrongPass123!", "NewPass234!", "NewPass23!4");
            
            doThrow(new InvalidPasswordException("La contraseña actual es incorrecta"))
                .when(userService)
                .updateProfile(eq(TEST_USER_ID), any(UserUpdateDTO.class));

            // Act & Assert
            mockMvc.perform(patch(ENDPOINT_USUARIOS_ME)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isUnauthorized())
                .andExpect(errorMessage("La contraseña actual es incorrecta"));

            verify(userService).updateProfile(eq(TEST_USER_ID), any(UserUpdateDTO.class));
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Should return 401 when not authenticated")
        void cambiarContrasenaNotAuthenticatedUnauthorized() throws Exception {
            // Arrange
            UserUpdateDTO requestDto = new UserUpdateDTO(
                "CurrentPass123!", "Psswd123!", "Psswd123!");

            // Act & Assert
            mockMvc.perform(patch(ENDPOINT_USUARIOS_ME)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isUnauthorized());
            
            verifyNoInteractions(userService);
        }
    }


    @Nested
    @DisplayName("GET /perfil")
    class ObtenerPerfilTests {
        
        @Test
        @DisplayName("Should get profile successfully when authenticated")
        @WithMockUser(username = TEST_USER_AUTH_NAME)
        void getProfileAuthenticatedSuccess() throws Exception {
            // Arrange
            UserResponseDTO responseDto = new UserResponseDTO(1L,
                TEST_USER_NAME,
                TEST_USER_EMAIL,
                User.Role.USER,
                LocalDateTime.now());
            
            when(userService.getProfile(TEST_USER_ID))
                .thenReturn(responseDto);

            // Act & Assert
            mockMvc.perform(get(ENDPOINT_USUARIOS_ME))
                .andExpect(status().isOk())
                .andExpect(userId(1))
                .andExpect(userName(TEST_USER_NAME))
                .andExpect(userEmail(TEST_USER_EMAIL))
                .andExpect(userRol(USER_ROLE))
                .andExpect(jsonPath("$.creationDate").exists());

            verify(userService).getProfile(TEST_USER_ID);
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Should return 401 when not authenticated")
        void getProfileNotAuthenticatedUnauthorized() throws Exception {
            // Act & Assert
            mockMvc.perform(get(ENDPOINT_USUARIOS_ME))
                .andExpect(status().isUnauthorized());
            
            verifyNoInteractions(userService);
        }
    }

    @Nested
    @DisplayName("GET /api/users/")
    class ListarUsuariosTests {
        
        @Test
        @WithMockUser(username = TEST_ADMIN_EMAIL, roles = "ADMIN")
        @DisplayName("Should list users with pagination when authenticated as admin")
        void listUsersAuthenticatedAsAdminSuccess() throws Exception {
            // Arrange
            List<UserResponseDTO> users = Arrays.asList(
                new UserResponseDTO(1L, "User 1", "user1@example.com", User.Role.USER, LocalDateTime.now()),
                new UserResponseDTO(2L, "User 2", "user2@example.com", User.Role.ADMIN, LocalDateTime.now())
            );
            Page<UserResponseDTO> page = new PageImpl<>(users);

            when(userService.listUsers(0, 10, SORT_FIELD_EMAIL, SORT_DIRECTION_ASC)).thenReturn(page);

            // Act & Assert
            mockMvc.perform(get(ENDPOINT_USUARIOS)
                    .param("page", "0")
                    .param("size", "10")
                    .param("sort", SORT_FIELD_EMAIL)
                    .param("direction", SORT_DIRECTION_ASC))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("User 1"))
                .andExpect(jsonPath("$.content[0].email").value("user1@example.com"))
                .andExpect(jsonPath("$.content[0].role").value(USER_ROLE))
                .andExpect(jsonPath("$.content[1].id").value(2))
                .andExpect(jsonPath("$.content[1].name").value("User 2"))
                .andExpect(jsonPath("$.content[1].email").value("user2@example.com"))
                .andExpect(jsonPath("$.content[1].role").value("ADMIN"))
                .andExpect(jsonPath("$.totalElements").value(2));

            verify(userService).listUsers(0, 10, SORT_FIELD_EMAIL, SORT_DIRECTION_ASC);
        }

        @Test
        @WithMockUser(username = TEST_ADMIN_EMAIL, roles = "ADMIN")
        @DisplayName("Should handle empty result with pagination")
        void listUsersEmptyResultSuccess() throws Exception {
            // Arrange
            Page<UserResponseDTO> emptyPage = new PageImpl<>(Collections.emptyList());
            when(userService.listUsers(0, 10, SORT_FIELD_EMAIL, SORT_DIRECTION_ASC)).thenReturn(emptyPage);

            // Act & Assert
            mockMvc.perform(get(ENDPOINT_USUARIOS)
                    .param("page", "0")
                    .param("size", "10")
                    .param("sort", SORT_FIELD_EMAIL)
                    .param("direction", SORT_DIRECTION_ASC))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));

            verify(userService).listUsers(0, 10, SORT_FIELD_EMAIL, SORT_DIRECTION_ASC);
        }

        @ParameterizedTest
        @WithMockUser(username = TEST_ADMIN_EMAIL, roles = "ADMIN")
        @CsvSource({"page, -1", "size, 0", "size, 101", "sort, password", "sort, unknown"})
        @DisplayName("Should return 400 when a paging parameter is invalid")
        void listUsersInvalidPagingParameterBadRequest(String parameter, String value) throws Exception {
            // Act & Assert
            mockMvc.perform(get(ENDPOINT_USUARIOS)
                    .param(parameter, value))
                .andExpect(status().isBadRequest())
                .andExpect(errorDetailContains(parameter));

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("Should return 403 when authenticated as regular user")
        @WithMockUser(username = TEST_USER_EMAIL, roles = USER_ROLE)
        void listUsersAuthenticatedAsUserForbidden() throws Exception {
            // Act & Assert
            mockMvc.perform(get(ENDPOINT_USUARIOS))
                .andExpect(status().isForbidden());
            
            verifyNoInteractions(userService);
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Should return 401 when not authenticated")
        void listUsersNotAuthenticatedUnauthorized() throws Exception {
            // Act & Assert
            mockMvc.perform(get(ENDPOINT_USUARIOS))
                .andExpect(status().isUnauthorized());
            
            verifyNoInteractions(userService);
        }
    }

    @Nested
    @DisplayName("PATCH /{id}/role")
    class CambiarRolTests {
        
        @Test
        @DisplayName("Should change user role when authenticated as admin")
        @WithMockUser(username = TEST_ADMIN_EMAIL, roles = "ADMIN")
        void changeRoleAuthenticatedAsAdminSuccess() throws Exception {
            // Arrange
            Long userId = 1L;
            User.Role newRole = User.Role.ADMIN;
            UserRoleUpdateDTO newRoleDTO = new UserRoleUpdateDTO(newRole);
            
            doNothing().when(userService).changeRole(userId, newRole);

            // Act & Assert
            mockMvc.perform(patch(createChangeRoleRequest(userId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newRoleDTO)))
                .andExpect(status().isOk());
            
            verify(userService).changeRole(userId, newRole);
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        @WithMockUser(username = TEST_ADMIN_EMAIL, roles = "ADMIN")
        void changeRoleUserNotFoundNotFound() throws Exception {
            // Arrange
            final Long userId = 999L;
            User.Role newRole = User.Role.ADMIN;
            UserRoleUpdateDTO newRoleDTO = new UserRoleUpdateDTO(newRole);
            
            doThrow(new ResourceNotFoundException("No existe ningún usuario con el ID proporcionado"))
                .when(userService).changeRole(userId, newRole);

            // Act & Assert
            mockMvc.perform(patch(createChangeRoleRequest(userId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newRoleDTO)))
                .andExpect(status().isNotFound())
                .andExpect(errorMessage("No existe ningún usuario con el ID proporcionado"));

            verify(userService).changeRole(userId, newRole);
        }

        @Test
        @DisplayName("Should return 403 when authenticated as regular user")
        @WithMockUser(username = TEST_USER_EMAIL, roles = USER_ROLE)
        void changeRoleAuthenticatedAsUserForbidden() throws Exception {
            // Arrange
            Long userId = 1L;
            User.Role newRole = User.Role.ADMIN;
            UserRoleUpdateDTO newRoleDTO = new UserRoleUpdateDTO(newRole);

            // Act & Assert
            mockMvc.perform(patch(createChangeRoleRequest(userId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newRoleDTO)))
                .andExpect(status().isForbidden());
            
            verifyNoInteractions(userService);
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Should return 401 when not authenticated")
        void changeRoleNotAuthenticatedUnauthorized() throws Exception {
            // Arrange
            Long userId = 1L;
            User.Role newRole = User.Role.ADMIN;
            UserRoleUpdateDTO newRoleDTO = new UserRoleUpdateDTO(newRole);

            // Act & Assert
            mockMvc.perform(patch(createChangeRoleRequest(userId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newRoleDTO)))
                .andExpect(status().isUnauthorized());
            
            verifyNoInteractions(userService);
        }
    }
}
