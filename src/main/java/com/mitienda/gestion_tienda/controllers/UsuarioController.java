package com.mitienda.gestion_tienda.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.mitienda.gestion_tienda.dtos.usuario.ActualizacionUsuarioDTO;
import com.mitienda.gestion_tienda.dtos.usuario.CambioPasswdDTO;
import com.mitienda.gestion_tienda.dtos.usuario.UsuarioAdminDTO;
import com.mitienda.gestion_tienda.dtos.usuario.UsuarioDTO;
import com.mitienda.gestion_tienda.dtos.usuario.UsuarioResponseDTO;
import com.mitienda.gestion_tienda.services.UsuarioService;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping("/registro")
    public UsuarioResponseDTO registrarUsuario(@Valid @RequestBody UsuarioDTO usuarioDTO) {
        return usuarioService.registrarUsuario(usuarioDTO);
    }

    @PostMapping("/admin/registro")
    public UsuarioResponseDTO registrarAdmin(@Valid @RequestBody UsuarioAdminDTO usuarioDTO) {
        return usuarioService.registrarUsuario(usuarioDTO);
    }

    @PutMapping("/perfil")
    public UsuarioResponseDTO actualizarPerfil(Authentication authentication,
            @Valid @RequestBody ActualizacionUsuarioDTO perfilDTO) {
        return usuarioService.actualizarPerfil(
            authentication.getName(), perfilDTO);
    }

    @PutMapping("/password")
    public void cambiarContraseña(
            Authentication authentication,
            @Valid @RequestBody CambioPasswdDTO contraseñaDTO) {
        usuarioService.cambiarContraseña(authentication.getName(), contraseñaDTO);
    }
}
