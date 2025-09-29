package com.gplanet.commerce.api.integration;

import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_COMPRAS_LISTAR;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_USUARIOS_ADMIN_CHANGE_ROLE;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_USUARIOS_REGISTRO;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import com.gplanet.commerce.api.dtos.usuario.UsuarioDTO;
import com.gplanet.commerce.api.entities.Usuario;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest extends AbstractIntegrationTest {
    
    @Test
    void accederEndpointPublicoSinAutenticacionPermitido() throws Exception {
        UsuarioDTO usuarioDTO = new UsuarioDTO("New User", "new@example.com", "password123");
        
        mockMvc.perform(post(ENDPOINT_USUARIOS_REGISTRO)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(usuarioDTO)))
                .andExpect(status().isOk());
    }
    
    @Test
    void accederEndpointProtegidoSinAutenticacionDenegado() throws Exception {
        mockMvc.perform(get(ENDPOINT_COMPRAS_LISTAR))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void accederEndpointAdminUsuarioNormalForbidden() throws Exception {
        // Create regular user using the helper method which properly handles password encoding
        crearUsuario("Regular User", "user@example.com", "Password123!", Usuario.Role.USER);
        
        // Use the JWT token obtained through proper authentication flow
        String jwtToken = obtenerJwtToken("user@example.com", "Password123!");
        
        mockMvc.perform(post(ENDPOINT_USUARIOS_ADMIN_CHANGE_ROLE)
                .header(HttpHeaders.AUTHORIZATION, jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());
    }
}
