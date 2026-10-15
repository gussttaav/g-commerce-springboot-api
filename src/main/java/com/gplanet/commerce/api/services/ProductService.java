package com.gplanet.commerce.api.services;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.gplanet.commerce.api.dtos.product.ProductDTO;
import com.gplanet.commerce.api.dtos.product.ProductMapper;
import com.gplanet.commerce.api.dtos.product.ProductResponseDTO;
import com.gplanet.commerce.api.dtos.product.ProductStatus;
import com.gplanet.commerce.api.entities.Product;
import com.gplanet.commerce.api.exceptions.ResourceNotFoundException;
import com.gplanet.commerce.api.repositories.ProductRepository;
import com.gplanet.commerce.api.utilities.DatabaseOperationHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service class that handles product-related operations including
 * listing, creation, and logical deletion of products.
 * 
 * @author Gustavo
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final ProductRepository productRepository;

    /**
     * Lists and searches products based on the specified status and search text with pagination support.
     * Search is performed on both product name and description fields.
     * 
     * @param status The status to filter products by
     * @param searchText Optional text to search within product name and description (case-insensitive)
     * @param page The page number (zero-based)
     * @param size The page size
     * @param sort The field to sort by
     * @param direction The sort direction (ASC or DESC)
     * @return Page of ProductResponseDTO containing filtered and searched paginated products
     */
    public Page<ProductResponseDTO> listProducts(
            ProductStatus status, 
            String searchText, 
            int page, 
            int size, 
            String sort, 
            String direction) {
        
        log.debug("Listing products with status: {}, search: '{}' and pagination - page: {}, "
                + "size: {}, sort: {}, direction: {}", 
                status, searchText, page, size, sort, direction);
        
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        
        Page<Product> productsPage;
        
        if (StringUtils.hasText(searchText)) {
            String searchTerm = "%" + searchText.toLowerCase() + "%";
            
            // Get paginated results based on status and search text
            switch (status) {
                case ACTIVE:
                    productsPage = productRepository.findByActiveTrueAndSearch(searchTerm, pageable);
                    break;
                case INACTIVE:
                    productsPage = productRepository.findByActiveFalseAndSearch(searchTerm, pageable);
                    break;
                case ALL:
                    productsPage = productRepository.findBySearch(searchTerm, pageable);
                    break;
                default:
                    productsPage = Page.empty(pageable);
            }
        } else {
            // Original code for when no search is performed
            productsPage = switch (status) {
                case ACTIVE -> productRepository.findByActiveTrue(pageable);
                case INACTIVE -> productRepository.findByActiveFalse(pageable);
                case ALL -> productRepository.findAll(pageable);
            };
        }
        
        // Map to DTOs
        return productsPage.map(productMapper::toProductResponseDTO);
    }

    /**
     * Creates a new product in the system.
     * 
     * @param productDTO Data transfer object containing product information
     * @return ProductResponseDTO containing the created product's information
     */
    @Transactional
    public ProductResponseDTO createProduct(ProductDTO productDTO) {
        log.info("Creating new product: {}", productDTO.name());
        Product product = productMapper.toProduct(productDTO);
        product.setCreationDate(LocalDateTime.now());
        
        Product savedProduct = DatabaseOperationHandler.executeOperation(() -> 
            productRepository.save(product)
        );
        log.info("Product created with ID: {}", savedProduct.getId());
        return productMapper.toProductResponseDTO(savedProduct);
    }

    /**
     * Updates an existing product in the system.
     * 
     * @param id ID of the product to update
     * @param productDTO Data transfer object containing updated product information
     * @return ProductResponseDTO containing the updated product's information
     * @throws ResourceNotFoundException if product is not found
     */
    @Transactional
    public ProductResponseDTO updateProduct(Long id, ProductDTO productDTO) {
        log.info("Updating product with ID: {}", id);
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        
        productMapper.updateProductFromDTO(productDTO, product);
        
        Product updatedProduct = DatabaseOperationHandler.executeOperation(() -> 
            productRepository.save(product)
        );
        log.info("Product successfully updated - ID: {}", updatedProduct.getId());
        return productMapper.toProductResponseDTO(updatedProduct);
    }

    /**
     * Logically deletes a product by marking it as inactive.
     * 
     * @param id ID of the product to delete
     * @throws ResourceNotFoundException if product is not found
     */
    @Transactional
    public void removeProduct(Long id) {
        log.info("Attempting to delete product with ID: {}", id);
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        product.setActive(false);
        productRepository.save(product);
        log.info("Product successfully marked as inactive - ID: {}", id);
    }
}
