package com.mitienda.gestion_tienda.controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.mitienda.gestion_tienda.dtos.producto.ProductoDTO;
import com.mitienda.gestion_tienda.dtos.producto.ProductoResponseDTO;
import com.mitienda.gestion_tienda.services.ProductoService;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;

    @GetMapping("/listar")
    public List<ProductoResponseDTO> listarProductos() {
        return productoService.listarProductos();
    }

    @PostMapping("/crear")
    public ProductoResponseDTO crearProducto(@Valid @RequestBody ProductoDTO productoDTO) {
        return productoService.crearProducto(productoDTO);
    }

    @DeleteMapping("/eliminar/{id}")
    public void eliminarProducto(@PathVariable @Min(1) Long id) {
        productoService.eliminarProducto(id);
    }
}
