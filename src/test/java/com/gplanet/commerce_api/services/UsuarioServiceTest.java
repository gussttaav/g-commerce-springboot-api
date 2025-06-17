package com.gplanet.commerce_api.services;

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

import com.gplanet.commerce_api.dtos.usuario.ActualizacionUsuarioDTO;
import com.gplanet.commerce_api.dtos.usuario.CambioPasswdDTO;
import com.gplanet.commerce_api.dtos.usuario.LoginDTO;
import com.gplanet.commerce_api.dtos.usuario.UsuarioDTO;
import com.gplanet.commerce_api.dtos.usuario.UsuarioMapper;
import com.gplanet.commerce_api.dtos.usuario.UsuarioResponseDTO;
import com.gplanet.commerce_api.entities.Usuario;
import com.gplanet.commerce_api.exceptions.ApiException;
import com.gplanet.commerce_api.exceptions.InvalidPasswordException;
import com.gplanet.commerce_api.exceptions.ResourceNotFoundException;
import com.gplanet.commerce_api.repositories.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {
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
        usuario.setNombre("Test User");
        usuario.setEmail("test@example.com");
        usuario.setPassword("encodedPassword");
        usuario.setRol(Usuario.Role.USER);
        usuario.setFechaCreacion(LocalDateTime.now());
        return usuario;
    }
    
    private UsuarioDTO createTestUsuarioDTO() {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setNombre("Test User");
        dto.setEmail("test@example.com");
        dto.setPassword("password123");
        return dto;
    }
    
    @Test
    void registrarUsuario_ValidData_Success() {
        // Arrange
        UsuarioDTO usuarioDTO = createTestUsuarioDTO();
        Usuario savedUsuario = createTestUsuario();
        UsuarioResponseDTO expectedResponse = new UsuarioResponseDTO(
            1L, usuarioDTO.getNombre(), usuarioDTO.getEmail(), 
            Usuario.Role.USER, savedUsuario.getFechaCreacion());
        
        when(passwordEncoder.encode(usuarioDTO.getPassword())).thenReturn("encodedPassword");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(savedUsuario);
        when(usuarioMapper.toUsuarioResponseDTO(savedUsuario)).thenReturn(expectedResponse);
        
        // Act
        UsuarioResponseDTO result = usuarioService.registrarUsuario(usuarioDTO);
        
        // Assert
        assertNotNull(result);
        assertEquals(savedUsuario.getId(), result.getId());
        assertEquals(savedUsuario.getNombre(), result.getNombre());
        assertEquals(savedUsuario.getEmail(), result.getEmail());
        assertEquals(Usuario.Role.USER, result.getRol());
        
        verify(passwordEncoder).encode(usuarioDTO.getPassword());
        verify(usuarioRepository).save(any(Usuario.class));
    }
    
    @Test
    void registrarUsuario_ExistingEmail_ThrowsException() {
        // Arrange
        UsuarioDTO usuarioDTO = createTestUsuarioDTO();
        usuarioDTO.setEmail("existing@example.com");
        
        when(passwordEncoder.encode(usuarioDTO.getPassword())).thenReturn("encodedPassword");
        when(usuarioRepository.save(any(Usuario.class)))
            .thenThrow(new DataIntegrityViolationException("Duplicate email"));
        
        assertThrows(ApiException.class, () -> 
            usuarioService.registrarUsuario(usuarioDTO));
        
        verify(passwordEncoder).encode(usuarioDTO.getPassword());
        verify(usuarioRepository).save(any(Usuario.class));
    }
    
    @Test
    void actualizarPerfil_ValidData_Success() {
        // Arrange
        Usuario existingUser = createTestUsuario();
        String userEmail = existingUser.getEmail();
        ActualizacionUsuarioDTO perfilDTO = new ActualizacionUsuarioDTO(
            "Updated Name", "new@example.com");
        Usuario updatedUser = createTestUsuario();
        updatedUser.setNombre(perfilDTO.getNombre());
        updatedUser.setEmail(perfilDTO.getNuevoEmail());
        UsuarioResponseDTO expectedResponse = UsuarioResponseDTO.builder()
            .nombre(perfilDTO.getNombre())
            .email(perfilDTO.getNuevoEmail())
            .build();
        
        when(usuarioRepository.findByEmail(userEmail)).thenReturn(Optional.of(existingUser));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(updatedUser);
        when(usuarioMapper.toUsuarioResponseDTO(updatedUser)).thenReturn(expectedResponse);
        
        // Act
        UsuarioResponseDTO result = usuarioService.actualizarPerfil(userEmail, perfilDTO);
        
        // Assert
        assertNotNull(result);
        assertEquals(perfilDTO.getNombre(), result.getNombre());
        assertEquals(perfilDTO.getNuevoEmail(), result.getEmail());
    }
    
    @Test
    void cambiarContraseña_ValidData_Success() {
        // Arrange
        String userEmail = "test@example.com";
        CambioPasswdDTO contraseñaDTO = new CambioPasswdDTO();
        contraseñaDTO.setCurrentPassword("oldPassword");
        contraseñaDTO.setNewPassword("newPassword");
        contraseñaDTO.setConfirmPassword("newPassword");
        
        Usuario usuario = new Usuario();
        usuario.setPassword("encodedOldPassword");
        
        when(usuarioRepository.findByEmail(userEmail)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(contraseñaDTO.getCurrentPassword(), usuario.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(contraseñaDTO.getNewPassword())).thenReturn("encodedNewPassword");
        
        // Act & Assert
        assertDoesNotThrow(() -> 
            usuarioService.cambiarContraseña(userEmail, contraseñaDTO));
        
        verify(usuarioRepository).save(usuario);
        assertEquals("encodedNewPassword", usuario.getPassword());
    }

    @Test
    void login_ValidCredentials_Success() {
        // Arrange
        String email = "test@example.com";
        String password = "password123";
        Usuario usuario = createTestUsuario();
        LoginDTO loginDTO = new LoginDTO(email, password);
        UsuarioResponseDTO expectedResponse = new UsuarioResponseDTO(
            usuario.getId(), usuario.getNombre(), usuario.getEmail(), 
            usuario.getRol(), usuario.getFechaCreacion()
        );

        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(password, usuario.getPassword())).thenReturn(true);
        when(usuarioMapper.toUsuarioResponseDTO(usuario)).thenReturn(expectedResponse);

        // Act
        UsuarioResponseDTO result = usuarioService.login(loginDTO);

        // Assert
        assertNotNull(result);
        assertEquals(expectedResponse.getId(), result.getId());
        assertEquals(expectedResponse.getNombre(), result.getNombre());
        assertEquals(expectedResponse.getEmail(), result.getEmail());
        assertEquals(expectedResponse.getRol(), result.getRol());
    }

    @Test
    void login_InvalidPassword_ThrowsException() {
        // Arrange
        String email = "test@example.com";
        String password = "wrongPassword";
        Usuario usuario = createTestUsuario();
        LoginDTO loginDTO = new LoginDTO(email, password);

        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(password, usuario.getPassword())).thenReturn(false);

        // Act & Assert
        assertThrows(InvalidPasswordException.class, () -> usuarioService.login(loginDTO));
    }

    @Test
    void login_NonExistentEmail_ThrowsException() {
        // Arrange
        String email = "nonexistent@example.com";
        String password = "password123";
        LoginDTO loginDTO = new LoginDTO(email, password);

        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> usuarioService.login(loginDTO));
    }

    @Test
    void obtenerPerfil_ExistingUser_Success() {
        // Arrange
        String email = "test@example.com";
        Usuario usuario = createTestUsuario();
        UsuarioResponseDTO expectedResponse = new UsuarioResponseDTO(
            usuario.getId(), usuario.getNombre(), usuario.getEmail(), 
            usuario.getRol(), usuario.getFechaCreacion()
        );

        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));
        when(usuarioMapper.toUsuarioResponseDTO(usuario)).thenReturn(expectedResponse);

        // Act
        UsuarioResponseDTO result = usuarioService.obtenerPerfil(email);

        // Assert
        assertNotNull(result);
        assertEquals(expectedResponse.getId(), result.getId());
        assertEquals(expectedResponse.getNombre(), result.getNombre());
        assertEquals(expectedResponse.getEmail(), result.getEmail());
        assertEquals(expectedResponse.getRol(), result.getRol());
        
        verify(usuarioRepository).findByEmail(email);
        verify(usuarioMapper).toUsuarioResponseDTO(usuario);
    }

    @Test
    void obtenerPerfil_NonExistentUser_ThrowsException() {
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
    void listarUsuarios_Success() {
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
        Page<UsuarioResponseDTO> expectedPage = new PageImpl<>(expectedResponses);

        when(usuarioRepository.findAll(any(Pageable.class))).thenReturn(usuariosPage);
        when(usuarioMapper.toUsuarioResponseDTO(any(Usuario.class)))
            .thenReturn(expectedResponses.get(0), expectedResponses.get(1));

        // Act
        Page<UsuarioResponseDTO> result = usuarioService.listarUsuarios(0, 10, "email", "ASC");

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(expectedPage.getTotalElements(), result.getTotalElements());
        verify(usuarioRepository).findAll(any(Pageable.class));
        verify(usuarioMapper, times(2)).toUsuarioResponseDTO(any(Usuario.class));
    }

    @Test
    void cambiarRol_ExistingUser_Success() {
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
    void cambiarRol_NonExistentUser_ThrowsException() {
        // Arrange
        Long userId = 999L;
        Usuario.Role newRole = Usuario.Role.ADMIN;

        when(usuarioRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> 
            usuarioService.cambiarRol(userId, newRole));
        
        verify(usuarioRepository).findById(userId);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void cambiarRol_SameRole_Success() {
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
