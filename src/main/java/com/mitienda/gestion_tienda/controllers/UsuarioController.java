package com.mitienda.gestion_tienda.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.mitienda.gestion_tienda.dtos.ActualizacionUsuarioDTO;
import com.mitienda.gestion_tienda.dtos.CambioPasswdDTO;
import com.mitienda.gestion_tienda.dtos.UsuarioAdminDTO;
import com.mitienda.gestion_tienda.dtos.UsuarioDTO;
import com.mitienda.gestion_tienda.services.UsuarioService;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping("/registro")
    public ResponseEntity<?> registrarUsuario(@Valid @RequestBody UsuarioDTO usuarioDTO) {
        return ResponseEntity.ok(usuarioService.registrarUsuario(usuarioDTO));
    }

    @PostMapping("/admin/registro")
    public ResponseEntity<?> registrarAdmin(@Valid @RequestBody UsuarioAdminDTO usuarioDTO) {
        return ResponseEntity.ok(usuarioService.registrarUsuario(usuarioDTO));
    }

    @PutMapping("/perfil")
    public ResponseEntity<?> actualizarPerfil(Authentication authentication,
            @Valid @RequestBody ActualizacionUsuarioDTO perfilDTO) {
        return ResponseEntity.ok(usuarioService.actualizarPerfil(
            authentication.getName(), perfilDTO));
    }

    @PutMapping("/password")
    public ResponseEntity<?> cambiarContraseña(
            Authentication authentication,
            @Valid @RequestBody CambioPasswdDTO contraseñaDTO) {
        usuarioService.cambiarContraseña(authentication.getName(), contraseñaDTO);
        return ResponseEntity.ok().build();
    }
}
