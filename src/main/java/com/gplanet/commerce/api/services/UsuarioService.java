package com.gplanet.commerce.api.services;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gplanet.commerce.api.dtos.usuario.UsuarioDTO;
import com.gplanet.commerce.api.dtos.usuario.UsuarioMapper;
import com.gplanet.commerce.api.dtos.usuario.UsuarioResponseDTO;
import com.gplanet.commerce.api.dtos.usuario.UsuarioUpdateDTO;
import com.gplanet.commerce.api.entities.Usuario;
import com.gplanet.commerce.api.exceptions.PasswordMismatchException;
import com.gplanet.commerce.api.exceptions.ResourceNotFoundException;
import com.gplanet.commerce.api.repositories.UsuarioRepository;
import com.gplanet.commerce.api.utilities.DatabaseOperationHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    
    private static final String USER_NOT_FOUND_MSG = "Usuario no encontrado";
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
    public UsuarioResponseDTO registrarUsuario(UsuarioDTO usuarioDTO) {
        log.info("Registering new user with email: {}", usuarioDTO.email());
        Usuario usuario = new Usuario();
        usuario.setNombre(usuarioDTO.nombre());
        usuario.setEmail(usuarioDTO.email());
        usuario.setPassword(passwordEncoder.encode(usuarioDTO.password()));
        usuario.setRol(usuarioDTO.rol());
        usuario.setFechaCreacion(LocalDateTime.now());

        Usuario savedUsuario = DatabaseOperationHandler.executeOperation(() -> 
            usuarioRepository.save(usuario)
        );
        log.info("User successfully registered with ID: {}", savedUsuario.getId());
        return usuarioMapper.toUsuarioResponseDTO(savedUsuario);
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
    public UsuarioResponseDTO actualizarPerfil(String email, UsuarioUpdateDTO perfilDTO) {
        log.info("Updating profile for user: {}", email);
        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND_MSG));
        // Update basic profile fields only when provided (partial update semantics)
        if (perfilDTO.nuevoEmail() != null) {
            String nuevoEmail = perfilDTO.nuevoEmail().trim();
            if (nuevoEmail.isEmpty()) {
                throw new IllegalArgumentException("nuevoEmail no puede ser vacío");
            }
            usuario.setEmail(nuevoEmail);
        }

        if (perfilDTO.nombre() != null) {
            String nombre = perfilDTO.nombre().trim();
            if (nombre.isEmpty()) {
                throw new IllegalArgumentException("nombre no puede ser vacío");
            }
            usuario.setNombre(nombre);
        }

        // If any password field is present, attempt password change flow
        if (perfilDTO.currentPassword() != null || perfilDTO.newPassword() != null
                || perfilDTO.confirmPassword() != null) {
            changePassword(usuario, perfilDTO);
        }

        Usuario updatedUsuario = DatabaseOperationHandler.executeOperation(() -> 
            usuarioRepository.save(usuario)
        );
        log.info("Profile updated successfully for user: {}", updatedUsuario.getEmail());
        return usuarioMapper.toUsuarioResponseDTO(updatedUsuario);
    }

    /**
     * Helper method to change the password of the user when updating the profile.
     *
     * @param usuario the existing user whose password is going to change 
     * @param perfilDTO user profile with password change details
     * @throws IllegalArgumentException if any of the password change parameters is null
     * @throws UsernameNotFoundException if the user is not found
     * @throws PasswordMismatchException if the current password is not correct or 
     *          new password is not confirmed correctly
     */
    private void changePassword(Usuario usuario, UsuarioUpdateDTO perfilDTO) {
        log.info("Password change attempt for user: {}", usuario.getEmail());

        // Require all three fields for a password change
        if (perfilDTO.currentPassword() == null || perfilDTO.newPassword() == null
                || perfilDTO.confirmPassword() == null) {
            throw new IllegalArgumentException(
                    "Se requieren currentPassword, newPassword y confirmPassword");
        }

        // Verify current password
        if (!passwordEncoder.matches(perfilDTO.currentPassword(), usuario.getPassword())) {
            throw new PasswordMismatchException("La contraseña actual es incorrecta");
        }

        // Verify new password is not the same as current
        if (passwordEncoder.matches(perfilDTO.newPassword(), usuario.getPassword())) {
            throw new IllegalArgumentException("La nueva contraseña debe ser diferente a la actual");
        }

        // Verify password confirmation
        if (!perfilDTO.newPassword().equals(perfilDTO.confirmPassword())) {
            throw new PasswordMismatchException("La nueva contraseña y su confirmación no coinciden");
        }

        usuario.setPassword(passwordEncoder.encode(perfilDTO.newPassword()));
        log.info("Password updated for user: {}", usuario.getEmail());
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
            .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND_MSG));
        return usuarioMapper.toUsuarioResponseDTO(usuario);
    }
    

    /**
     * Lists all users in the system with pagination support.
     * 
     * @param page The page number (zero-based)
     * @param size The page size
     * @param sort The field to sort by
     * @param direction The sort direction (ASC or DESC)
     * @return Page of UsuarioResponseDTO containing paginated users' information
     */
    public Page<UsuarioResponseDTO> listarUsuarios(int page, int size, String sort, String direction) {
        log.debug("Listing users with pagination - page: {}, size: {}, sort: {}, direction: {}", 
                page, size, sort, direction);
        
        // Create Pageable object with sort direction
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        
        // Get paginated result
        Page<Usuario> usuariosPage = usuarioRepository.findAll(pageable);
        
        // Map to DTOs
        Page<UsuarioResponseDTO> result = usuariosPage.map(usuarioMapper::toUsuarioResponseDTO);
        
        log.debug("Found {} users on page {} of {}", 
                result.getNumberOfElements(), 
                result.getNumber() + 1,  // +1 for human-readable page number
                result.getTotalPages());
                
        return result;
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
