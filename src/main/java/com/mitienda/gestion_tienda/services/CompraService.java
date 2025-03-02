package com.mitienda.gestion_tienda.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mitienda.gestion_tienda.dtos.compra.CompraDTO;
import com.mitienda.gestion_tienda.dtos.compra.CompraMapper;
import com.mitienda.gestion_tienda.dtos.compra.CompraProductoDTO;
import com.mitienda.gestion_tienda.dtos.compra.CompraResponseDTO;
import com.mitienda.gestion_tienda.entities.Compra;
import com.mitienda.gestion_tienda.entities.CompraProducto;
import com.mitienda.gestion_tienda.entities.Producto;
import com.mitienda.gestion_tienda.entities.Usuario;
import com.mitienda.gestion_tienda.exceptions.ResourceNotFoundException;
import com.mitienda.gestion_tienda.repositories.CompraRepository;
import com.mitienda.gestion_tienda.repositories.ProductoRepository;
import com.mitienda.gestion_tienda.repositories.UsuarioRepository;
import com.mitienda.gestion_tienda.utilities.DatabaseOperationHandler;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class that handles purchase-related operations including
 * creating new purchases and listing purchase history.
 * 
 * @author Gustavo
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompraService {
    private final CompraMapper compraMapper;
    private final CompraRepository compraRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Lists purchases based on user role. Admins see all purchases,
     * regular users see only their own purchases.
     * 
     * @param email Email of the requesting user
     * @return List of CompraResponseDTO containing purchase information
     * @throws UsernameNotFoundException if user is not found
     */
    @Transactional(readOnly = true)
    public List<CompraResponseDTO> listarCompras(String email) {
        log.debug("Listing purchases for user: {}", email);
        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
            
        List<Compra> compras;
        if (usuario.getRol() == Usuario.Role.ADMIN) {
            compras = compraRepository.findAll();
        } else {
            compras = compraRepository.findByUsuario(usuario);
        }
        
        log.debug("Found {} purchases", compras.size());
        return compras.stream()
            .map(compraMapper::toCompraResponseDTO)
            .collect(Collectors.toList());
    }

    /**
     * Processes a new purchase for a user, calculating totals and
     * creating all necessary purchase records.
     * 
     * @param email Email of the user making the purchase
     * @param compraDTO Data transfer object containing purchase information
     * @return CompraResponseDTO containing the created purchase information
     * @throws UsernameNotFoundException if user is not found
     * @throws ResourceNotFoundException if any product in the purchase is not found
     */
    @Transactional
    public CompraResponseDTO realizarCompra(String email, CompraDTO compraDTO) {
        log.info("Starting new purchase for user: {}", email);
        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        Compra compra = new Compra();
        compra.setUsuario(usuario);
        compra.setFecha(LocalDateTime.now());
        compra.setTotal(BigDecimal.ZERO);

        BigDecimal total = BigDecimal.ZERO;

        for (CompraProductoDTO item : compraDTO.getProductos()) {
            Producto producto = productoRepository.findById(item.getProductoId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

            CompraProducto compraProducto = new CompraProducto();
            compraProducto.setProducto(producto);
            compraProducto.setCantidad(item.getCantidad());
            compraProducto.setSubtotal(
                producto.getPrecio().multiply(BigDecimal.valueOf(item.getCantidad()))
            );
            
            compra.addCompraProducto(compraProducto);
            total = total.add(compraProducto.getSubtotal());
        }

        compra.setTotal(total);        
        Compra savedCompra = DatabaseOperationHandler.executeOperation(() -> 
            compraRepository.save(compra)
        );
        log.info("Purchase completed - ID: {}, Total: {}", savedCompra.getId(), savedCompra.getTotal());
        return compraMapper.toCompraResponseDTO(savedCompra);
    }
}