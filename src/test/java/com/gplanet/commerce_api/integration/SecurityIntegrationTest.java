package com.gplanet.commerce_api.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import com.gplanet.commerce_api.dtos.usuario.UsuarioDTO;
import com.gplanet.commerce_api.entities.Usuario;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest extends BaseIntegrationTest {
    
    @Test
    void accederEndpointPublico_SinAutenticacion_Permitido() throws Exception {
        UsuarioDTO usuarioDTO = new UsuarioDTO();
        usuarioDTO.setNombre("New User");
        usuarioDTO.setEmail("new@example.com");
        usuarioDTO.setPassword("password123");
        
        mockMvc.perform(post("/api/usuarios/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(usuarioDTO)))
                .andExpect(status().isOk());
    }
    
    @Test
    void accederEndpointProtegido_SinAutenticacion_Denegado() throws Exception {
        mockMvc.perform(get("/api/compras/listar"))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void accederEndpointAdmin_UsuarioNormal_Forbidden() throws Exception {
        // Create regular user
        Usuario usuario = new Usuario();
        usuario.setEmail("user@example.com");
        usuario.setPassword(passwordEncoder.encode("password123"));
        usuario.setRol(Usuario.Role.USER);
        usuarioRepository.save(usuario);
        
        mockMvc.perform(post("/api/productos/crear")
                .header(HttpHeaders.AUTHORIZATION, 
                        obtenerBasicAuthHeader("user@example.com", "password123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());
    }
}
