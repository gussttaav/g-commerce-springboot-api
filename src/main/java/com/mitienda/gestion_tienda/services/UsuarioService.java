package com.mitienda.gestion_tienda.services;

import lombok.RequiredArgsConstructor;

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

@Service
@RequiredArgsConstructor
public class UsuarioService {
    
    private final UsuarioMapper usuarioMapper;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UsuarioResponseDTO registrarUsuario(UsuarioDTO usuarioDTO){
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

        return usuarioMapper.toUsuarioResponseDTO(
            DatabaseOperationHandler.executeOperation(() -> 
                usuarioRepository.save(usuario)
        ));
    }

    public UsuarioResponseDTO login(LoginDTO loginDTO) {
        Usuario usuario = usuarioRepository.findByEmail(loginDTO.getEmail())
            .orElseThrow(() -> new ResourceNotFoundException(
                "No existe ningún usuario con el email proporcionado"
            ));

        if (!passwordEncoder.matches(loginDTO.getPassword(), usuario.getPassword())) {
            throw new InvalidPasswordException("Contraseña incorrecta.");
        }

        return usuarioMapper.toUsuarioResponseDTO(usuario);
    }

    @Transactional
    public UsuarioResponseDTO actualizarPerfil(String email, ActualizacionUsuarioDTO perfilDTO) {
        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        usuario.setEmail(perfilDTO.getNuevoEmail());
        usuario.setNombre(perfilDTO.getNombre());

        return usuarioMapper.toUsuarioResponseDTO(
            DatabaseOperationHandler.executeOperation(() -> 
                usuarioRepository.save(usuario)
        ));
    }

    @Transactional
    public void cambiarContraseña(String email, CambioPasswdDTO contraseñaDTO) {
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
    }
}
