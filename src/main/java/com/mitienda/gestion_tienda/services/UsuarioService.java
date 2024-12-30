package com.mitienda.gestion_tienda.services;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mitienda.gestion_tienda.dtos.ActualizacionUsuarioDTO;
import com.mitienda.gestion_tienda.dtos.CambioPasswdDTO;
import com.mitienda.gestion_tienda.dtos.UsuarioAdminDTO;
import com.mitienda.gestion_tienda.dtos.UsuarioDTO;
import com.mitienda.gestion_tienda.entities.Usuario;
import com.mitienda.gestion_tienda.repositories.UsuarioRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UsuarioService {
    
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Usuario registrarUsuario(UsuarioDTO usuarioDTO){
        if (usuarioRepository.existsByEmail(usuarioDTO.getEmail())) {
            throw new RuntimeException("El email ya está registrado");
        }

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

        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario actualizarPerfil(String email, ActualizacionUsuarioDTO perfilDTO) {
        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        // If changing email, verify it's not already taken
        if (!usuario.getEmail().equals(perfilDTO.getNuevoEmail())) {
            if (usuarioRepository.existsByEmail(perfilDTO.getNuevoEmail())) {
                throw new RuntimeException("El email ya está en uso");
            }
            usuario.setEmail(perfilDTO.getNuevoEmail());
        }

        usuario.setNombre(perfilDTO.getNombre());
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public void cambiarContraseña(String email, CambioPasswdDTO contraseñaDTO) {
        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        // Verify current password
        if (!passwordEncoder.matches(contraseñaDTO.getCurrentPassword(), usuario.getPassword())) {
            throw new RuntimeException("La contraseña actual es incorrecta");
        }

        // Update password
        usuario.setPassword(passwordEncoder.encode(contraseñaDTO.getNewPassword()));
        usuarioRepository.save(usuario);
    }
}
