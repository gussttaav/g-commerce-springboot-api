package com.mitienda.gestion_tienda.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mitienda.gestion_tienda.entities.Compra;
import com.mitienda.gestion_tienda.entities.Usuario;

public interface CompraRepository extends JpaRepository<Compra, Long> {
    List<Compra> findByUsuario(Usuario usuario);
    List<Compra> findByFechaBetween(LocalDateTime yesterday, LocalDateTime tomorrow);
}
