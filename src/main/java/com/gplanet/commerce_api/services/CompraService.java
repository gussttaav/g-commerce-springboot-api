package com.gplanet.commerce_api.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gplanet.commerce_api.dtos.compra.CompraDTO;
import com.gplanet.commerce_api.dtos.compra.CompraMapper;
import com.gplanet.commerce_api.dtos.compra.CompraProductoDTO;
import com.gplanet.commerce_api.dtos.compra.CompraResponseDTO;
import com.gplanet.commerce_api.entities.Compra;
import com.gplanet.commerce_api.entities.CompraProducto;
import com.gplanet.commerce_api.entities.Producto;
import com.gplanet.commerce_api.entities.Usuario;
import com.gplanet.commerce_api.exceptions.ResourceNotFoundException;
import com.gplanet.commerce_api.repositories.CompraRepository;
import com.gplanet.commerce_api.repositories.ProductoRepository;
import com.gplanet.commerce_api.repositories.UsuarioRepository;
import com.gplanet.commerce_api.utilities.DatabaseOperationHandler;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
     * Lists purchases based on user role with pagination support.
     * Admins see all purchases, regular users see only their own purchases.
     * 
     * @param email Email of the requesting user
     * @param page The page number (zero-based)
     * @param size The page size
     * @param sort The field to sort by
     * @param direction The sort direction (ASC or DESC)
     * @return Page of CompraResponseDTO containing paginated purchase information
     * @throws UsernameNotFoundException if user is not found
     */
    @Transactional(readOnly = true)
    public Page<CompraResponseDTO> listarCompras(String email, int page, int size, String sort, String direction) {
        log.debug("Listing purchases for user: {} with pagination - page: {}, size: {}, sort: {}, direction: {}", 
                email, page, size, sort, direction);
        
        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
            
        // Create Pageable object with sort direction
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        
        // Get paginated result based on user role
        Page<Compra> comprasPage;
        if (usuario.getRol() == Usuario.Role.ADMIN) {
            comprasPage = compraRepository.findAll(pageable);
        } else {
            comprasPage = compraRepository.findByUsuario(usuario, pageable);
        }
        
        // Map to DTOs
        Page<CompraResponseDTO> result = comprasPage.map(compraMapper::toCompraResponseDTO);
        
        log.debug("Found {} purchases on page {} of {}", 
                result.getNumberOfElements(), 
                result.getNumber() + 1,  // +1 for human-readable page number
                result.getTotalPages());
                
        return result;
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

        for (CompraProductoDTO item : compraDTO.productos()) {
            Producto producto = productoRepository.findById(item.productoId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

            CompraProducto compraProducto = new CompraProducto();
            compraProducto.setProducto(producto);
            compraProducto.setCantidad(item.cantidad());
            compraProducto.setSubtotal(
                producto.getPrecio().multiply(BigDecimal.valueOf(item.cantidad()))
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