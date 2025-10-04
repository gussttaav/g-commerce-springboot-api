package com.gplanet.commerce.api.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.gplanet.commerce.api.dtos.usuario.ActualizacionUsuarioDTO;
import com.gplanet.commerce.api.dtos.usuario.CambioPasswdDTO;
import com.gplanet.commerce.api.dtos.usuario.UsuarioDTO;
import com.gplanet.commerce.api.dtos.usuario.UsuarioMapper;
import com.gplanet.commerce.api.dtos.usuario.UsuarioResponseDTO;
import com.gplanet.commerce.api.entities.Usuario;
import com.gplanet.commerce.api.exceptions.ApiException;
import com.gplanet.commerce.api.exceptions.ResourceNotFoundException;
import com.gplanet.commerce.api.repositories.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {
    public static final String TEST_NAME = "Test User";
    public static final String TEST_USERNAME = "test@example.com";
    public static final String TEST_PASSWD = "testPasswd123!";
    private static final Long NON_EXISTENT_USER_ID = 999L;

    @Mock
    private UsuarioRepository usuarioRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UsuarioMapper usuarioMapper;
    
    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario createTestUsuario() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre(TEST_NAME);
        usuario.setEmail(TEST_USERNAME);
        usuario.setPassword(TEST_PASSWD);
        usuario.setRol(Usuario.Role.USER);
        usuario.setFechaCreacion(LocalDateTime.now());
        return usuario;
    }
    
    private UsuarioDTO createTestUsuarioDTO() {
        return new UsuarioDTO(TEST_NAME, TEST_USERNAME, "password123");
    }
    
    @Test
    void registrarUsuarioValidDataSuccess() {
        // Arrange
        UsuarioDTO usuarioDTO = createTestUsuarioDTO();
        Usuario savedUsuario = createTestUsuario();
        UsuarioResponseDTO expectedResponse = new UsuarioResponseDTO(
            1L, usuarioDTO.nombre(), usuarioDTO.email(), 
            Usuario.Role.USER, savedUsuario.getFechaCreacion());
        
        when(passwordEncoder.encode(usuarioDTO.password())).thenReturn(TEST_PASSWD);
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(savedUsuario);
        when(usuarioMapper.toUsuarioResponseDTO(savedUsuario)).thenReturn(expectedResponse);
        
        // Act
        UsuarioResponseDTO result = usuarioService.registrarUsuario(usuarioDTO);
        
        // Assert
        assertNotNull(result);
        assertEquals(savedUsuario.getId(), result.id());
        assertEquals(savedUsuario.getNombre(), result.nombre());
        assertEquals(savedUsuario.getEmail(), result.email());
        assertEquals(Usuario.Role.USER, result.rol());
        
        verify(passwordEncoder).encode(usuarioDTO.password());
        verify(usuarioRepository).save(any(Usuario.class));
    }
    
    @Test
    void registrarUsuarioExistingEmailThrowsException() {
        // Arrange
        UsuarioDTO usuarioDTO = new UsuarioDTO(TEST_NAME, "existing@example.com", "password123");
        
        when(passwordEncoder.encode(usuarioDTO.password())).thenReturn(TEST_PASSWD);
        when(usuarioRepository.save(any(Usuario.class)))
            .thenThrow(new DataIntegrityViolationException("Duplicate email"));
        
        assertThrows(ApiException.class, () -> 
            usuarioService.registrarUsuario(usuarioDTO));
        
        verify(passwordEncoder).encode(usuarioDTO.password());
        verify(usuarioRepository).save(any(Usuario.class));
    }
    
    @Test
    void actualizarPerfilValidDataSuccess() {
        // Arrange
        Usuario existingUser = createTestUsuario();
        String userEmail = existingUser.getEmail();
        ActualizacionUsuarioDTO perfilDTO = new ActualizacionUsuarioDTO(
            "Updated Name", "new@example.com");
        Usuario updatedUser = createTestUsuario();
        updatedUser.setNombre(perfilDTO.nombre());
        updatedUser.setEmail(perfilDTO.nuevoEmail());
        UsuarioResponseDTO expectedResponse = new UsuarioResponseDTO(
            1L, perfilDTO.nombre(), perfilDTO.nuevoEmail(), 
            Usuario.Role.USER, LocalDateTime.now());
        
        when(usuarioRepository.findByEmail(userEmail)).thenReturn(Optional.of(existingUser));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(updatedUser);
        when(usuarioMapper.toUsuarioResponseDTO(updatedUser)).thenReturn(expectedResponse);
        
        // Act
        UsuarioResponseDTO result = usuarioService.actualizarPerfil(userEmail, perfilDTO);
        
        // Assert
        assertNotNull(result);
        assertEquals(perfilDTO.nombre(), result.nombre());
        assertEquals(perfilDTO.nuevoEmail(), result.email());
    }
    
    @Test
    void cambiarContrasenaValidDataSuccess() {
        // Arrange
        String userEmail = TEST_USERNAME;
        CambioPasswdDTO contrasenaDTO = new CambioPasswdDTO("oldPassword", "newPassword", "newPassword");
        
        Usuario usuario = new Usuario();
        usuario.setPassword("encodedOldPassword");
        
        when(usuarioRepository.findByEmail(userEmail)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(contrasenaDTO.currentPassword(), usuario.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(contrasenaDTO.newPassword())).thenReturn("encodedNewPassword");
        
        // Act & Assert
        assertDoesNotThrow(() -> 
            usuarioService.changePassword(userEmail, contrasenaDTO));
        
        verify(usuarioRepository).save(usuario);
        assertEquals("encodedNewPassword", usuario.getPassword());
    }


    @Test
    void obtenerPerfilExistingUserSuccess() {
        // Arrange
        Usuario usuario = createTestUsuario();
        UsuarioResponseDTO expectedResponse = new UsuarioResponseDTO(
            usuario.getId(), usuario.getNombre(), usuario.getEmail(), 
            usuario.getRol(), usuario.getFechaCreacion()
        );

        when(usuarioRepository.findByEmail(TEST_USERNAME)).thenReturn(Optional.of(usuario));
        when(usuarioMapper.toUsuarioResponseDTO(usuario)).thenReturn(expectedResponse);

        // Act
        UsuarioResponseDTO result = usuarioService.obtenerPerfil(TEST_USERNAME);

        // Assert
        assertNotNull(result);
        assertEquals(expectedResponse.id(), result.id());
        assertEquals(expectedResponse.nombre(), result.nombre());
        assertEquals(expectedResponse.email(), result.email());
        assertEquals(expectedResponse.rol(), result.rol());
        
        verify(usuarioRepository).findByEmail(TEST_USERNAME);
        verify(usuarioMapper).toUsuarioResponseDTO(usuario);
    }

    @Test
    void obtenerPerfilNonExistentUserThrowsException() {
        // Arrange
        String email = "nonexistent@example.com";
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class, () -> 
            usuarioService.obtenerPerfil(email));
        
        verify(usuarioRepository).findByEmail(email);
        verify(usuarioMapper, never()).toUsuarioResponseDTO(any());
    }

    @Test
    void listarUsuariosSuccess() {
        // Arrange
        List<Usuario> usuarios = Arrays.asList(
            createTestUsuario(),
            createTestUsuario()
        );
        Page<Usuario> usuariosPage = new PageImpl<>(usuarios);
        List<UsuarioResponseDTO> expectedResponses = usuarios.stream()
            .map(u -> new UsuarioResponseDTO(u.getId(), u.getNombre(), 
                u.getEmail(), u.getRol(), u.getFechaCreacion()))
            .collect(Collectors.toList());

        when(usuarioRepository.findAll(any(Pageable.class))).thenReturn(usuariosPage);
        when(usuarioMapper.toUsuarioResponseDTO(any(Usuario.class)))
            .thenReturn(expectedResponses.get(0), expectedResponses.get(1));

        // Act
        Page<UsuarioResponseDTO> result = usuarioService.listarUsuarios(0, 10, "email", "ASC");

        // Assert
        Page<UsuarioResponseDTO> expectedPage = new PageImpl<>(expectedResponses);
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(expectedPage.getTotalElements(), result.getTotalElements());
        verify(usuarioRepository).findAll(any(Pageable.class));
        verify(usuarioMapper, times(2)).toUsuarioResponseDTO(any(Usuario.class));
    }

    @Test
    void cambiarRolExistingUserSuccess() {
        // Arrange
        Long userId = 1L;
        Usuario usuario = createTestUsuario();
        Usuario.Role newRole = Usuario.Role.ADMIN;

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        // Act
        assertDoesNotThrow(() -> usuarioService.cambiarRol(userId, newRole));

        // Assert
        assertEquals(newRole, usuario.getRol());
        verify(usuarioRepository).findById(userId);
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void cambiarRolNonExistentUserThrowsException() {
        // Arrange
        Usuario.Role newRole = Usuario.Role.ADMIN;

        when(usuarioRepository.findById(NON_EXISTENT_USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> 
            usuarioService.cambiarRol(NON_EXISTENT_USER_ID, newRole));
        
        verify(usuarioRepository).findById(NON_EXISTENT_USER_ID);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void cambiarRolSameRoleSuccess() {
        // Arrange
        Long userId = 1L;
        Usuario usuario = createTestUsuario();
        Usuario.Role sameRole = usuario.getRol();

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        // Act
        assertDoesNotThrow(() -> usuarioService.cambiarRol(userId, sameRole));

        // Assert
        assertEquals(sameRole, usuario.getRol());
        verify(usuarioRepository).findById(userId);
        verify(usuarioRepository).save(usuario);
    }
}
