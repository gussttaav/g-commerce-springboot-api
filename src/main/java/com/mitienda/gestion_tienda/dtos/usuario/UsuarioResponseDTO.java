package com.mitienda.gestion_tienda.dtos.usuario;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

import com.mitienda.gestion_tienda.entities.Usuario;

@Data
@AllArgsConstructor
@Builder
public class UsuarioResponseDTO {
    private Long id;
    private String nombre;
    private String email;
    private Usuario.Role rol;
    private LocalDateTime fechaCreacion;
}
