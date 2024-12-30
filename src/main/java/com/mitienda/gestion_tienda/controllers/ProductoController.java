package com.mitienda.gestion_tienda.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.mitienda.gestion_tienda.dtos.ProductoDTO;
import com.mitienda.gestion_tienda.services.ProductoService;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;

    @GetMapping("/listar")
    public ResponseEntity<?> listarProductos() {
        return ResponseEntity.ok(productoService.listarProductos());
    }

    @PostMapping("/crear")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> crearProducto(@Valid @RequestBody ProductoDTO productoDTO) {
        return ResponseEntity.ok(productoService.crearProducto(productoDTO));
    }

    @DeleteMapping("/eliminar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> eliminarProducto(@PathVariable Long id) {
        productoService.eliminarProducto(id);
        return ResponseEntity.ok().build();
    }
}
