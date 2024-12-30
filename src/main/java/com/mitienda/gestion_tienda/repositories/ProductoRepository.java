package com.mitienda.gestion_tienda.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mitienda.gestion_tienda.entities.Producto;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findByActivoTrue();
}
