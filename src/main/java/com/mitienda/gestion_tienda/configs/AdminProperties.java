package com.mitienda.gestion_tienda.configs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "admin.default")
public class AdminProperties {
    private String email;
    private String password;
    private String nombre;
}
