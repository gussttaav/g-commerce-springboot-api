package com.mitienda.gestion_tienda.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.mitienda.gestion_tienda.dtos.CompraDTO;
import com.mitienda.gestion_tienda.dtos.CompraResponseDTO;
import com.mitienda.gestion_tienda.services.CompraService;

@RestController
@RequestMapping("/api/compras")
@RequiredArgsConstructor
public class CompraController {

    private final CompraService compraService;

    @PostMapping("/nueva")
    public ResponseEntity<CompraResponseDTO> realizarCompra(
            Authentication authentication,
            @Valid @RequestBody CompraDTO compraDTO) {
        CompraResponseDTO response = compraService.realizarCompra(
            authentication.getName(), compraDTO);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/listar")
    public ResponseEntity<?> listarCompras(Authentication authentication) {
        return ResponseEntity.ok(compraService.listarCompras(authentication.getName()));
    }
}
