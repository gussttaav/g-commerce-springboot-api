package com.mitienda.gestion_tienda.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mitienda.gestion_tienda.entities.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
}
