package com.mitienda.gestion_tienda.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.mitienda.gestion_tienda.dtos.compra.CompraDTO;
import com.mitienda.gestion_tienda.dtos.compra.CompraResponseDTO;
import com.mitienda.gestion_tienda.services.CompraService;

@RestController
@RequestMapping("/api/compras")
@RequiredArgsConstructor
public class CompraController {

    private final CompraService compraService;

    @PostMapping("/nueva")
    public CompraResponseDTO realizarCompra(
            Authentication authentication,
            @Valid @RequestBody CompraDTO compraDTO) {

        return compraService.realizarCompra(
            authentication.getName(), compraDTO);
    }

    @GetMapping("/listar")
    public List<CompraResponseDTO> listarCompras(Authentication authentication) {
        return compraService.listarCompras(authentication.getName());
    }
}
