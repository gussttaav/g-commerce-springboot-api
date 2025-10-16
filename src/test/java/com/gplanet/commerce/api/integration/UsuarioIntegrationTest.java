package com.gplanet.commerce.api.integration;

import static com.gplanet.commerce.api.testutils.ApiErrorResultMatchers.errorMessage;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_USUARIOS;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_USUARIOS_ME;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import com.gplanet.commerce.api.dtos.usuario.UsuarioDTO;
import com.gplanet.commerce.api.dtos.usuario.UsuarioRoleUpdateDTO;
import com.gplanet.commerce.api.dtos.usuario.UsuarioUpdateDTO;
import com.gplanet.commerce.api.entities.Usuario;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UsuarioIntegrationTest extends AbstractIntegrationTest {
    private static final String TEST_USER_NAME = "Test User";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_ADMIN_EMAIL = "admin@example.com";
    private static final String TEST_ADMIN_PASSWORD = "admin123";
    private static final String USER_ROLE = "USER";
    private static final String ADMIN_ROLE = "ADMIN";

    private String createChangeRoleRequest(Long userId) {
        return String.format("%s/%d/role", ENDPOINT_USUARIOS, userId);
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

    @Test
    void registroUsuarioDatosValidosRetornaUsuarioCreado() throws Exception {
        // Arrange
        UsuarioDTO usuarioDTO = new UsuarioDTO(TEST_USER_NAME, TEST_EMAIL, TEST_PASSWORD);
        
        // Act & Assert
        mockMvc.perform(post(ENDPOINT_USUARIOS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(usuarioDTO)))
                .andExpect(status().isOk())
                .andExpect(userEmail(TEST_EMAIL))
                .andExpect(userRol(USER_ROLE))
                .andExpect(userName(TEST_USER_NAME));
        
        // Verify database
        Usuario usuarioGuardado = usuarioRepository.findByEmail(TEST_EMAIL)
            .orElseThrow(() -> new AssertionError("Usuario no encontrado en la base de datos"));
        assertEquals(Usuario.Role.USER, usuarioGuardado.getRol());
        assertTrue(passwordEncoder.matches(TEST_PASSWORD, usuarioGuardado.getPassword()));
    }
    
    @Test
    void registroAdminDatosValidosRetornaAdminCreado() throws Exception {
        // Arrange
        final String newEmail = "new.admin@example.com";
        UsuarioDTO adminDTO = new UsuarioDTO(
                "New Admin", 
                newEmail, 
                "admin123", Usuario.Role.ADMIN);
        crearUsuarioAdmin();

        String authHeader = obtenerJwtToken(TEST_ADMIN_EMAIL, TEST_ADMIN_PASSWORD);
        
        // Act & Assert
        mockMvc.perform(post(ENDPOINT_USUARIOS)
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adminDTO)))
                .andExpect(status().isOk())
                .andExpect(userEmail(newEmail))
                .andExpect(userRol(ADMIN_ROLE));
                
        // Verify database
        Usuario adminGuardado = usuarioRepository.findByEmail(newEmail)
            .orElseThrow(() -> new AssertionError("Admin no encontrado en la base de datos"));
        assertEquals(Usuario.Role.ADMIN, adminGuardado.getRol());
    }
    
    @Test
    void registroUsuarioEmailDuplicadoRetornaError() throws Exception {
        // Arrange - Create first user
        UsuarioDTO primerUsuario = new UsuarioDTO("First User", TEST_EMAIL, TEST_PASSWORD);
        
        mockMvc.perform(post(ENDPOINT_USUARIOS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(primerUsuario)))
                .andExpect(status().isOk());

        // Arrange - Create duplicate user
        UsuarioDTO usuarioDuplicado = new UsuarioDTO("Duplicate User", TEST_EMAIL, TEST_PASSWORD);
        
        // Act & Assert
        mockMvc.perform(post(ENDPOINT_USUARIOS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(usuarioDuplicado)))
                .andExpect(status().isConflict());
    }
    
    @Test
    void actualizarPerfilUsuarioAutenticadoActualizaCorrectamente() throws Exception {
        // Arrange - Create user first
        final String updatedEmail = "updated@example.com";
        UsuarioDTO usuarioDTO = new UsuarioDTO(TEST_USER_NAME, TEST_EMAIL, TEST_PASSWORD);
        
        mockMvc.perform(post(ENDPOINT_USUARIOS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(usuarioDTO)))
                .andExpect(status().isOk());

        // Arrange - Prepare update data
        UsuarioUpdateDTO actualizacionDTO = new UsuarioUpdateDTO(
            "Updated Name", updatedEmail);
        
        String authHeader = obtenerJwtToken(TEST_EMAIL, TEST_PASSWORD);
        
        // Act & Assert
        mockMvc.perform(patch(ENDPOINT_USUARIOS_ME)
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(actualizacionDTO)))
                .andExpect(status().isOk())
                .andExpect(userName("Updated Name"))
                .andExpect(userEmail(updatedEmail));
                
        // Verify database update
        assertFalse(usuarioRepository.findByEmail(TEST_EMAIL).isPresent(), 
            "Old email should not exist");
        assertTrue(usuarioRepository.findByEmail(updatedEmail).isPresent(), 
            "New email should exist");
    }
    
    @Test
    void cambiarContrasenaValidDataChangesSuccessfully() throws Exception {
        // Arrange - Create user first
        final String newPassword = "newPassword123";
        UsuarioDTO usuarioDTO = new UsuarioDTO(TEST_USER_NAME, TEST_EMAIL, TEST_PASSWORD);
        
        mockMvc.perform(post(ENDPOINT_USUARIOS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(usuarioDTO)))
                .andExpect(status().isOk());

        // Arrange - Prepare password change data
        UsuarioUpdateDTO cambioDTO = new UsuarioUpdateDTO(TEST_PASSWORD, newPassword, newPassword);
        
        String authHeader = obtenerJwtToken(TEST_EMAIL, TEST_PASSWORD);
        
        // Act & Assert
        mockMvc.perform(patch(ENDPOINT_USUARIOS_ME)
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cambioDTO)))
                .andExpect(status().isOk());
                
        // Verify password change
        Usuario usuario = usuarioRepository.findByEmail(TEST_EMAIL)
            .orElseThrow(() -> new AssertionError("Usuario no encontrado"));
        assertTrue(passwordEncoder.matches(newPassword, usuario.getPassword()), 
            "Password should be updated in database");
    }
    
    @Test
    void cambiarContrasenaIncorrectCurrentPasswordReturnsError() throws Exception {
        // Arrange - Create user first
        UsuarioDTO usuarioDTO = new UsuarioDTO(TEST_USER_NAME, TEST_EMAIL, TEST_PASSWORD);
        
        mockMvc.perform(post(ENDPOINT_USUARIOS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(usuarioDTO)))
                .andExpect(status().isOk());

        // Arrange - Prepare invalid password change data
        UsuarioUpdateDTO cambioDTO = new UsuarioUpdateDTO("wrongPassword", "newPassword", "newPassword");
        
        String authHeader = obtenerJwtToken(TEST_EMAIL, TEST_PASSWORD);
        
        // Act & Assert
        mockMvc.perform(patch(ENDPOINT_USUARIOS_ME)
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cambioDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(errorMessage("La contraseña actual es incorrecta"));
    }

    @Test
    void obtenerPerfilUsuarioAutenticadoRetornaDatosCorrectos() throws Exception {
        // Arrange - Create user first
        UsuarioDTO usuarioDTO = new UsuarioDTO(TEST_USER_NAME, TEST_EMAIL, TEST_PASSWORD);
        
        mockMvc.perform(post(ENDPOINT_USUARIOS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(usuarioDTO)))
                .andExpect(status().isOk());

        String authHeader = obtenerJwtToken(TEST_EMAIL, TEST_PASSWORD);
        
        // Act & Assert
        mockMvc.perform(get(ENDPOINT_USUARIOS_ME)
                .header(HttpHeaders.AUTHORIZATION, authHeader))
                .andExpect(status().isOk())
                .andExpect(userName(TEST_USER_NAME))
                .andExpect(userEmail(TEST_EMAIL))
                .andExpect(userRol(USER_ROLE));
    }

    @Test
    void obtenerPerfilSinAutenticacionRetornaUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(get(ENDPOINT_USUARIOS_ME))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listarUsuariosUsuarioAdminRetornaListaCompleta() throws Exception {
        // Arrange - Create regular user
        UsuarioDTO usuarioDTO = new UsuarioDTO(TEST_USER_NAME, TEST_EMAIL, TEST_PASSWORD);
        
        mockMvc.perform(post(ENDPOINT_USUARIOS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(usuarioDTO)))
                .andExpect(status().isOk());

        // Create admin user
        crearUsuarioAdmin();
        String authHeader = obtenerJwtToken(TEST_ADMIN_EMAIL, TEST_ADMIN_PASSWORD);
        
        // Act & Assert
        mockMvc.perform(get(ENDPOINT_USUARIOS)
                .header(HttpHeaders.AUTHORIZATION, authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[*].email", hasItems(TEST_EMAIL, TEST_ADMIN_EMAIL)))
                .andExpect(jsonPath("$.content[*].rol", hasItems(USER_ROLE, ADMIN_ROLE)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.isLastPage").value(true));
    }

    @Test
    void listarUsuariosUsuarioRegularRetornaForbidden() throws Exception {
        // Arrange - Create regular user
        UsuarioDTO usuarioDTO = new UsuarioDTO(TEST_USER_NAME, TEST_EMAIL, TEST_PASSWORD);
        
        mockMvc.perform(post(ENDPOINT_USUARIOS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(usuarioDTO)))
                .andExpect(status().isOk());

        String authHeader = obtenerJwtToken(TEST_EMAIL, TEST_PASSWORD);
        
        // Act & Assert
        mockMvc.perform(get(ENDPOINT_USUARIOS)
                .header(HttpHeaders.AUTHORIZATION, authHeader))
                .andExpect(status().isForbidden());
    }

    @Test
    void cambiarRolUsuarioAdminCambiaRolCorrectamente() throws Exception {
        // Arrange - Create regular user
        UsuarioDTO usuarioDTO = new UsuarioDTO(TEST_USER_NAME, TEST_EMAIL, TEST_PASSWORD);
        
        MvcResult result = mockMvc.perform(post(ENDPOINT_USUARIOS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(usuarioDTO)))
                .andExpect(status().isOk())
                .andReturn();

        // Get user ID from response
        String responseJson = result.getResponse().getContentAsString();
        Number userIdNumber = JsonPath.read(responseJson, "$.id");
        Long userId = userIdNumber.longValue();

        // Create admin user
        crearUsuarioAdmin();
        String authHeader = obtenerJwtToken(TEST_ADMIN_EMAIL, TEST_ADMIN_PASSWORD);
        UsuarioRoleUpdateDTO newRoleDTO = new UsuarioRoleUpdateDTO(Usuario.Role.ADMIN);
        
        // Act & Assert - Change role to ADMIN
        mockMvc.perform(patch(createChangeRoleRequest(userId))
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newRoleDTO)))
                .andExpect(status().isOk());
                
        // Verify role change in database
        Usuario usuario = usuarioRepository.findByEmail(TEST_EMAIL)
                .orElseThrow(() -> new AssertionError("Usuario no encontrado"));
        assertEquals(Usuario.Role.ADMIN, usuario.getRol());
    }

    @Test
    void cambiarRolUsuarioRegularRetornaForbidden() throws Exception {
        // Arrange - Create first regular user
        UsuarioDTO usuarioDTO = new UsuarioDTO(TEST_USER_NAME, TEST_EMAIL, TEST_PASSWORD);
        
        MvcResult result = mockMvc.perform(post(ENDPOINT_USUARIOS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(usuarioDTO)))
                .andExpect(status().isOk())
                .andReturn();

        // Get user ID from response
        String responseJson = result.getResponse().getContentAsString();
        Number userIdNumber = JsonPath.read(responseJson, "$.id");
        Long userId = userIdNumber.longValue();

        // Create second regular user
        UsuarioDTO usuario2DTO = new UsuarioDTO("Test User 2", "test2@example.com", TEST_PASSWORD);
        
        mockMvc.perform(post(ENDPOINT_USUARIOS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(usuario2DTO)))
                .andExpect(status().isOk());

        String authHeader = obtenerJwtToken("test2@example.com", TEST_PASSWORD);
        UsuarioRoleUpdateDTO newRoleDTO = new UsuarioRoleUpdateDTO(Usuario.Role.ADMIN);
        
        // Act & Assert - Try to change role
        mockMvc.perform(patch(createChangeRoleRequest(userId))
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newRoleDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void cambiarRolUsuarioInexistenteRetornaNotFound() throws Exception {
        // Arrange - Create admin user
        crearUsuarioAdmin();
        String authHeader = obtenerJwtToken(TEST_ADMIN_EMAIL, TEST_ADMIN_PASSWORD);
        UsuarioRoleUpdateDTO newRoleDTO = new UsuarioRoleUpdateDTO(Usuario.Role.ADMIN);
        final Long userId = 999999L;
        
        // Act & Assert
        mockMvc.perform(patch(createChangeRoleRequest(userId))
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newRoleDTO)))
                .andExpect(status().isNotFound());
    }
}
