package com.gplanet.commerce.api.services;

import java.util.Collections;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.gplanet.commerce.api.entities.Usuario;
import com.gplanet.commerce.api.repositories.UsuarioRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service class that implements Spring Security's UserDetailsService.
 * Provides user authentication and authority information to Spring Security.
 * 
 * @author Gustavo
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class UsuarioDetallesService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    /**
     * Loads user details by email for Spring Security authentication.
     * 
     * @param email The email of the user to load
     * @return UserDetails object containing user's security information
     * @throws UsernameNotFoundException if user is not found
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        return new User(
            usuario.getEmail(),
            usuario.getPassword(),
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + usuario.getRol()))
        );
    }
}
