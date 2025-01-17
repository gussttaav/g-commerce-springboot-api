package com.mitienda.gestion_tienda.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mitienda.gestion_tienda.entities.Usuario;
import com.mitienda.gestion_tienda.entities.Usuario.Role;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    List<Usuario> findByRol(Role role);
}
