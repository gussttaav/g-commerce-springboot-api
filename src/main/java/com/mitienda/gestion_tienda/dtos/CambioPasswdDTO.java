package com.mitienda.gestion_tienda.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CambioPasswdDTO {
    @NotBlank(message = "La contraseña actual es obligatoria")
    private String currentPassword;
    
    @NotBlank(message = "La nueva contraseña es obligatoria")
    private String newPassword;
    
    @NotBlank(message = "La confirmación de contraseña es obligatoria")
    private String confirmPassword;
}
