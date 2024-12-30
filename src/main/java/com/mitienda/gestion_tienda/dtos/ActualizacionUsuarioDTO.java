package com.mitienda.gestion_tienda.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ActualizacionUsuarioDTO {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    
    @Email(message = "El email debe ser válido")
    @NotBlank(message = "El email es obligatorio")
    private String nuevoEmail;
    
}
