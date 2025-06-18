package com.gplanet.commerce_api.integration;

import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

import com.gplanet.commerce_api.dtos.usuario.ActualizacionUsuarioDTO;
import com.gplanet.commerce_api.dtos.usuario.CambioPasswdDTO;
import com.gplanet.commerce_api.dtos.usuario.LoginDTO;
import com.gplanet.commerce_api.dtos.usuario.UsuarioAdminDTO;
import com.gplanet.commerce_api.dtos.usuario.UsuarioDTO;
import com.gplanet.commerce_api.entities.Usuario;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UsuarioIntegrationTest extends BaseIntegrationTest {
    private static final String BASE_URL = "/api/usuarios";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_ADMIN_EMAIL = "admin@example.com";
    private static final String TEST_ADMIN_PASSWORD = "admin123";

    @Test
    void registroUsuario_DatosValidos_RetornaUsuarioCreado() throws Exception {
        // Arrange
        UsuarioDTO usuarioDTO = new UsuarioDTO("Test User", TEST_EMAIL, TEST_PASSWORD);
        
        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(usuarioDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                .andExpect(jsonPath("$.rol").value("USER"))
                .andExpect(jsonPath("$.nombre").value("Test User"));
        
        // Verify database
        Usuario usuarioGuardado = usuarioRepository.findByEmail(TEST_EMAIL)
            .orElseThrow(() -> new AssertionError("Usuario no encontrado en la base de datos"));
        assertEquals(Usuario.Role.USER, usuarioGuardado.getRol());
        assertTrue(passwordEncoder.matches(TEST_PASSWORD, usuarioGuardado.getPassword()));
    }
    
    @Test
    void registroAdmin_DatosValidos_RetornaAdminCreado() throws Exception {
        // Arrange
        UsuarioAdminDTO adminDTO = new UsuarioAdminDTO("New Admin", "new.admin@example.com", "admin123", Usuario.Role.ADMIN);
        crearUsuarioAdmin();

        String authHeader = obtenerBasicAuthHeader(TEST_ADMIN_EMAIL, TEST_ADMIN_PASSWORD);
        
        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/admin/registro")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adminDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new.admin@example.com"))
                .andExpect(jsonPath("$.rol").value("ADMIN"));
                
        // Verify database
        Usuario adminGuardado = usuarioRepository.findByEmail("new.admin@example.com")
            .orElseThrow(() -> new AssertionError("Admin no encontrado en la base de datos"));
        assertEquals(Usuario.Role.ADMIN, adminGuardado.getRol());
    }
    
    @Test
    void registroUsuario_EmailDuplicado_RetornaError() throws Exception {
        // Arrange - Create first user
        UsuarioDTO primerUsuario = new UsuarioDTO("First User", TEST_EMAIL, TEST_PASSWORD);
        
        mockMvc.perform(post(BASE_URL + "/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(primerUsuario)))
                .andExpect(status().isOk());

        // Arrange - Create duplicate user
        UsuarioDTO usuarioDuplicado = new UsuarioDTO("Duplicate User", TEST_EMAIL, TEST_PASSWORD);
        
        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(usuarioDuplicado)))
                .andExpect(status().isConflict());
    }
    
    @Test
    void actualizarPerfil_UsuarioAutenticado_ActualizaCorrectamente() throws Exception {
        // Arrange - Create user first
        UsuarioDTO usuarioDTO = new UsuarioDTO("Test User", TEST_EMAIL, TEST_PASSWORD);
        
        mockMvc.perform(post(BASE_URL + "/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(usuarioDTO)))
                .andExpect(status().isOk());

        // Arrange - Prepare update data
        ActualizacionUsuarioDTO actualizacionDTO = new ActualizacionUsuarioDTO(
            "Updated Name", "updated@example.com");
        
        String authHeader = obtenerBasicAuthHeader(TEST_EMAIL, TEST_PASSWORD);
        
        // Act & Assert
        mockMvc.perform(put(BASE_URL + "/perfil")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(actualizacionDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Updated Name"))
                .andExpect(jsonPath("$.email").value("updated@example.com"));
                
        // Verify database update
        assertFalse(usuarioRepository.findByEmail(TEST_EMAIL).isPresent(), 
            "Old email should not exist");
        assertTrue(usuarioRepository.findByEmail("updated@example.com").isPresent(), 
            "New email should exist");
    }
    
    @Test
    void cambiarContraseña_DatosValidos_CambiaCorrectamente() throws Exception {
        // Arrange - Create user first
        UsuarioDTO usuarioDTO = new UsuarioDTO("Test User", TEST_EMAIL, TEST_PASSWORD);
        
        mockMvc.perform(post(BASE_URL + "/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(usuarioDTO)))
                .andExpect(status().isOk());

        // Arrange - Prepare password change data
        CambioPasswdDTO cambioDTO = new CambioPasswdDTO(TEST_PASSWORD, "newPassword123", "newPassword123");
        
        String authHeader = obtenerBasicAuthHeader(TEST_EMAIL, TEST_PASSWORD);
        
        // Act & Assert
        mockMvc.perform(put(BASE_URL + "/password")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cambioDTO)))
                .andExpect(status().isOk());
                
        // Verify password change
        Usuario usuario = usuarioRepository.findByEmail(TEST_EMAIL)
            .orElseThrow(() -> new AssertionError("Usuario no encontrado"));
        assertTrue(passwordEncoder.matches("newPassword123", usuario.getPassword()), 
            "Password should be updated in database");
    }
    
    @Test
    void cambiarContraseña_ContraseñaActualIncorrecta_RetornaError() throws Exception {
        // Arrange - Create user first
        UsuarioDTO usuarioDTO = new UsuarioDTO("Test User", TEST_EMAIL, TEST_PASSWORD);
        
        mockMvc.perform(post(BASE_URL + "/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(usuarioDTO)))
                .andExpect(status().isOk());

        // Arrange - Prepare invalid password change data
        CambioPasswdDTO cambioDTO = new CambioPasswdDTO("wrongPassword", "newPassword123", "newPassword123");
        
        String authHeader = obtenerBasicAuthHeader(TEST_EMAIL, TEST_PASSWORD);
        
        // Act & Assert
        mockMvc.perform(put(BASE_URL + "/password")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cambioDTO)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("La contraseña actual es incorrecta"));
    }

    @Test
    void login_CredencialesCorrectas_RetornaUsuario() throws Exception {
        // Arrange - Create user first
        UsuarioDTO usuarioDTO = new UsuarioDTO("Test User", TEST_EMAIL, TEST_PASSWORD);
        
        // Register user first
        mockMvc.perform(post(BASE_URL + "/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(usuarioDTO)))
                .andExpect(status().isOk());

        // Arrange login request
        LoginDTO loginDTO = new LoginDTO(TEST_EMAIL, TEST_PASSWORD);

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                .andExpect(jsonPath("$.nombre").value("Test User"))
                .andExpect(jsonPath("$.rol").value("USER"));
    }

    @Test
    void login_ContraseñaIncorrecta_RetornaBadRequest() throws Exception {
        // Arrange - Create user first
        UsuarioDTO usuarioDTO = new UsuarioDTO("Test User", TEST_EMAIL, TEST_PASSWORD);
        
        // Register user first
        mockMvc.perform(post(BASE_URL + "/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(usuarioDTO)))
                .andExpect(status().isOk());

        // Arrange login request with wrong password
        LoginDTO loginDTO = new LoginDTO(TEST_EMAIL, "wrongpassword");

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Contraseña incorrecta."));
    }

    @Test
    void obtenerPerfil_UsuarioAutenticado_RetornaDatosCorrectos() throws Exception {
        // Arrange - Create user first
        UsuarioDTO usuarioDTO = new UsuarioDTO("Test User", TEST_EMAIL, TEST_PASSWORD);
        
        mockMvc.perform(post(BASE_URL + "/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(usuarioDTO)))
                .andExpect(status().isOk());

        String authHeader = obtenerBasicAuthHeader(TEST_EMAIL, TEST_PASSWORD);
        
        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/perfil")
                .header(HttpHeaders.AUTHORIZATION, authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Test User"))
                .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                .andExpect(jsonPath("$.rol").value("USER"));
    }

    @Test
    void obtenerPerfil_SinAutenticacion_RetornaUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/perfil"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listarUsuarios_UsuarioAdmin_RetornaListaCompleta() throws Exception {
        // Arrange - Create regular user
        UsuarioDTO usuarioDTO = new UsuarioDTO("Test User", TEST_EMAIL, TEST_PASSWORD);
        
        mockMvc.perform(post(BASE_URL + "/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(usuarioDTO)))
                .andExpect(status().isOk());

        // Create admin user
        crearUsuarioAdmin();
        String authHeader = obtenerBasicAuthHeader(TEST_ADMIN_EMAIL, TEST_ADMIN_PASSWORD);
        
        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/admin/listar")
                .header(HttpHeaders.AUTHORIZATION, authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[*].email", hasItems(TEST_EMAIL, TEST_ADMIN_EMAIL)))
                .andExpect(jsonPath("$.content[*].rol", hasItems("USER", "ADMIN")))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.lastPage").value(true));
    }

    @Test
    void listarUsuarios_UsuarioRegular_RetornaForbidden() throws Exception {
        // Arrange - Create regular user
        UsuarioDTO usuarioDTO = new UsuarioDTO("Test User", TEST_EMAIL, TEST_PASSWORD);
        
        mockMvc.perform(post(BASE_URL + "/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(usuarioDTO)))
                .andExpect(status().isOk());

        String authHeader = obtenerBasicAuthHeader(TEST_EMAIL, TEST_PASSWORD);
        
        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/admin/listar")
                .header(HttpHeaders.AUTHORIZATION, authHeader))
                .andExpect(status().isForbidden());
    }

    @Test
    void cambiarRol_UsuarioAdmin_CambiaRolCorrectamente() throws Exception {
        // Arrange - Create regular user
        UsuarioDTO usuarioDTO = new UsuarioDTO("Test User", TEST_EMAIL, TEST_PASSWORD);
        
        MvcResult result = mockMvc.perform(post(BASE_URL + "/registro")
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
        String authHeader = obtenerBasicAuthHeader(TEST_ADMIN_EMAIL, TEST_ADMIN_PASSWORD);
        
        // Act & Assert - Change role to ADMIN
        mockMvc.perform(put(BASE_URL + "/admin/change-role")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .param("userId", String.valueOf(userId))
                .param("newRole", "ADMIN"))
                .andExpect(status().isOk());
                
        // Verify role change in database
        Usuario usuario = usuarioRepository.findByEmail(TEST_EMAIL)
                .orElseThrow(() -> new AssertionError("Usuario no encontrado"));
        assertEquals(Usuario.Role.ADMIN, usuario.getRol());
    }

    @Test
    void cambiarRol_UsuarioRegular_RetornaForbidden() throws Exception {
        // Arrange - Create first regular user
        UsuarioDTO usuarioDTO = new UsuarioDTO("Test User", TEST_EMAIL, TEST_PASSWORD);
        
        MvcResult result = mockMvc.perform(post(BASE_URL + "/registro")
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
        
        mockMvc.perform(post(BASE_URL + "/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(usuario2DTO)))
                .andExpect(status().isOk());

        String authHeader = obtenerBasicAuthHeader("test2@example.com", TEST_PASSWORD);
        
        // Act & Assert - Try to change role
        mockMvc.perform(put(BASE_URL + "/admin/change-role")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .param("userId", String.valueOf(userId))
                .param("newRole", "ADMIN"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cambiarRol_UsuarioInexistente_RetornaNotFound() throws Exception {
        // Arrange - Create admin user
        crearUsuarioAdmin();
        String authHeader = obtenerBasicAuthHeader(TEST_ADMIN_EMAIL, TEST_ADMIN_PASSWORD);
        
        // Act & Assert
        mockMvc.perform(put(BASE_URL + "/admin/change-role")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .param("userId", "999999")
                .param("newRole", "ADMIN"))
                .andExpect(status().isNotFound());
    }
}