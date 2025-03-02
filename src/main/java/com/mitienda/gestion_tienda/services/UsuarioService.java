package com.mitienda.gestion_tienda.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mitienda.gestion_tienda.dtos.usuario.ActualizacionUsuarioDTO;
import com.mitienda.gestion_tienda.dtos.usuario.CambioPasswdDTO;
import com.mitienda.gestion_tienda.dtos.usuario.LoginDTO;
import com.mitienda.gestion_tienda.dtos.usuario.UsuarioAdminDTO;
import com.mitienda.gestion_tienda.dtos.usuario.UsuarioDTO;
import com.mitienda.gestion_tienda.dtos.usuario.UsuarioMapper;
import com.mitienda.gestion_tienda.dtos.usuario.UsuarioResponseDTO;
import com.mitienda.gestion_tienda.entities.Usuario;
import com.mitienda.gestion_tienda.exceptions.InvalidPasswordException;
import com.mitienda.gestion_tienda.exceptions.PasswordMismatchException;
import com.mitienda.gestion_tienda.exceptions.ResourceNotFoundException;
import com.mitienda.gestion_tienda.repositories.UsuarioRepository;
import com.mitienda.gestion_tienda.utilities.DatabaseOperationHandler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class that handles user-related operations including registration,
 * authentication, profile updates, and password management.
 * 
 * @author Gustavo
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioService {
    
    private final UsuarioMapper usuarioMapper;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registers a new user in the system.
     * 
     * @param usuarioDTO Data transfer object containing user registration information
     * @return UsuarioResponseDTO containing the created user's information
     */
    @Transactional
    public UsuarioResponseDTO registrarUsuario(UsuarioDTO usuarioDTO){
        log.info("Registering new user with email: {}", usuarioDTO.getEmail());
        Usuario usuario = new Usuario();
        usuario.setNombre(usuarioDTO.getNombre());
        usuario.setEmail(usuarioDTO.getEmail());
        usuario.setPassword(passwordEncoder.encode(usuarioDTO.getPassword()));

        if(usuarioDTO instanceof UsuarioAdminDTO){
            usuario.setRol(Usuario.Role.ADMIN);
        }else{
            usuario.setRol(Usuario.Role.USER);
        }
        usuario.setFechaCreacion(LocalDateTime.now());

        Usuario savedUsuario = DatabaseOperationHandler.executeOperation(() -> 
            usuarioRepository.save(usuario)
        );
        log.info("User successfully registered with ID: {}", savedUsuario.getId());
        return usuarioMapper.toUsuarioResponseDTO(savedUsuario);
    }

    /**
     * Authenticates a user using email and password.
     * 
     * @param loginDTO Data transfer object containing login credentials
     * @return UsuarioResponseDTO containing the authenticated user's information
     * @throws ResourceNotFoundException if user is not found
     * @throws InvalidPasswordException if password is incorrect
     */
    public UsuarioResponseDTO login(LoginDTO loginDTO) {
        log.info("Login attempt for user: {}", loginDTO.getEmail());
        try {
            Usuario usuario = usuarioRepository.findByEmail(loginDTO.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "No existe ningún usuario con el email proporcionado"
                ));

            if (!passwordEncoder.matches(loginDTO.getPassword(), usuario.getPassword())) {
                throw new InvalidPasswordException("Contraseña incorrecta.");
            }

            log.info("Successful login for user: {}", loginDTO.getEmail());
            return usuarioMapper.toUsuarioResponseDTO(usuario);
        } catch (ResourceNotFoundException | InvalidPasswordException e) {
            log.warn("Login failed for user: {} - {}", loginDTO.getEmail(), e.getMessage());
            throw e;
        }
    }

    /**
     * Updates a user's profile information.
     * 
     * @param email Current email of the user
     * @param perfilDTO Data transfer object containing new profile information
     * @return UsuarioResponseDTO containing the updated user information
     * @throws UsernameNotFoundException if user is not found
     */
    @Transactional
    public UsuarioResponseDTO actualizarPerfil(String email, ActualizacionUsuarioDTO perfilDTO) {
        log.info("Updating profile for user: {}", email);
        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        usuario.setEmail(perfilDTO.getNuevoEmail());
        usuario.setNombre(perfilDTO.getNombre());

        Usuario updatedUsuario = DatabaseOperationHandler.executeOperation(() -> 
            usuarioRepository.save(usuario)
        );
        log.info("Profile updated successfully for user: {}", updatedUsuario.getEmail());
        return usuarioMapper.toUsuarioResponseDTO(updatedUsuario);
    }

    /**
     * Changes a user's password after validating current password.
     * 
     * @param email Email of the user
     * @param contraseñaDTO Data transfer object containing password change information
     * @throws UsernameNotFoundException if user is not found
     * @throws InvalidPasswordException if current password is incorrect or new password is same as current
     * @throws PasswordMismatchException if new password and confirmation don't match
     */
    @Transactional
    public void cambiarContraseña(String email, CambioPasswdDTO contraseñaDTO) {
        log.info("Password change attempt for user: {}", email);
        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        // Verify current password
        if (!passwordEncoder.matches(contraseñaDTO.getCurrentPassword(), usuario.getPassword())) {
            throw new InvalidPasswordException("La contraseña actual es incorrecta");
        }

        // Verify new password is not the same as current
        if (passwordEncoder.matches(contraseñaDTO.getNewPassword(), usuario.getPassword())) {
            throw new InvalidPasswordException("La nueva contraseña debe ser diferente a la actual");
        }

        // Verify password confirmation
        if (!contraseñaDTO.getNewPassword().equals(contraseñaDTO.getConfirmPassword())) {
            throw new PasswordMismatchException("La nueva contraseña y su confirmación no coinciden");
        }

        // Update password
        usuario.setPassword(passwordEncoder.encode(contraseñaDTO.getNewPassword()));
        usuarioRepository.save(usuario);
        log.info("Password successfully changed for user: {}", email);
    }
    
    /**
     * Retrieves a user's profile information.
     * 
     * @param email Email of the user
     * @return UsuarioResponseDTO containing the user's information
     * @throws UsernameNotFoundException if user is not found
     */
    public UsuarioResponseDTO obtenerPerfil(String email) {
        log.debug("Retrieving profile for user: {}", email);
        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        return usuarioMapper.toUsuarioResponseDTO(usuario);
    }
    

    /**
     * Lists all users in the system.
     * 
     * @return List of UsuarioResponseDTO containing all users' information
     */
    public List<UsuarioResponseDTO> listarUsuarios() {
        log.debug("Listing all users");
        List<UsuarioResponseDTO> users = usuarioRepository.findAll().stream()
            .map(usuarioMapper::toUsuarioResponseDTO)
            .collect(Collectors.toList());
        log.debug("Found {} users", users.size());
        return users;
    }

    /**
     * Updates a user's role.
     * 
     * @param userId ID of the user
     * @param newRole New role to assign
     * @throws ResourceNotFoundException if user is not found
     */
    @Transactional
    public void cambiarRol(Long userId, Usuario.Role newRole) {
        log.info("Changing role to {} for user ID: {}", newRole, userId);
        Usuario usuario = usuarioRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No existe ningún usuario con el ID proporcionado"
            ));
        
        usuario.setRol(newRole);
        usuarioRepository.save(usuario);
        log.info("Role successfully updated for user ID: {}", userId);
    }
}
