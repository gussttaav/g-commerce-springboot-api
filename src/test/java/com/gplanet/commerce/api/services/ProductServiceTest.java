package com.gplanet.commerce.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.gplanet.commerce.api.dtos.product.ProductDTO;
import com.gplanet.commerce.api.dtos.product.ProductMapper;
import com.gplanet.commerce.api.dtos.product.ProductResponseDTO;
import com.gplanet.commerce.api.dtos.product.ProductStatus;
import com.gplanet.commerce.api.entities.Product;
import com.gplanet.commerce.api.exceptions.ResourceNotFoundException;
import com.gplanet.commerce.api.repositories.ProductRepository;

/**
 * Unit tests for ProductService.
 * Tests product management operations.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final String TEST_PRODUCT_NAME = "Test Product";
    private static final String TEST_PRODUCT_DESC = "Test Product Description";
    private static final BigDecimal PRICE_99 = new BigDecimal("99.99");

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private ProductDTO productDTO;
    private Product product;
    private ProductResponseDTO productResponseDTO;
    private Product inactiveProduct;
    private ProductResponseDTO inactiveProductResponseDTO;


    private Page<ProductResponseDTO> searchProductsOnePageResultSorteAscByName(
            ProductStatus status, String searchString) {
        return productService.listProducts(
            status, searchString, 0, 10, "name", "ASC"
        );        
    }

    private Page<ProductResponseDTO> listOnePageProductsSortedAscByName(ProductStatus status) {
        return searchProductsOnePageResultSorteAscByName(status, null);
    }

    @BeforeEach
    void setUp() {
        productDTO = new ProductDTO(
            TEST_PRODUCT_NAME,
            TEST_PRODUCT_DESC,
            PRICE_99,
            true
        );

        product = new Product();
        product.setId(1L);
        product.setName(TEST_PRODUCT_NAME);
        product.setDescription(TEST_PRODUCT_DESC);
        product.setPrice(PRICE_99);
        product.setCreationDate(LocalDateTime.now());
        product.setActive(true);

        productResponseDTO = new ProductResponseDTO(
            1L,
            TEST_PRODUCT_NAME,
            TEST_PRODUCT_DESC,
            PRICE_99,
            product.getCreationDate(),
            true
        );

        inactiveProduct = new Product();
        inactiveProduct.setId(2L);
        inactiveProduct.setName("Inactive Product");
        inactiveProduct.setDescription("Inactive Description");
        inactiveProduct.setPrice(new BigDecimal("49.99"));
        inactiveProduct.setCreationDate(LocalDateTime.now());
        inactiveProduct.setActive(false);

        inactiveProductResponseDTO = new ProductResponseDTO(
            2L,
            "Inactive Product",
            "Inactive Description",
            new BigDecimal("49.99"),
            inactiveProduct.getCreationDate(),
            false
        );
    }

    /**
     * Verifies that products are correctly listed by status without search.
     */
    @Test
    void listProductsActiveStatusReturnsOnlyActiveProducts() {
        // Arrange
        Page<Product> mockPage = new PageImpl<>(List.of(product));
        when(productRepository.findByActiveTrue(any(Pageable.class))).thenReturn(mockPage);
        when(productMapper.toProductResponseDTO(product)).thenReturn(productResponseDTO);

        // Act - now with null searchText
        Page<ProductResponseDTO> resultado = listOnePageProductsSortedAscByName(ProductStatus.ACTIVE);

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.getTotalElements());
        assertEquals(productResponseDTO, resultado.getContent().get(0));
        verify(productRepository).findByActiveTrue(any(Pageable.class));
        verify(productMapper).toProductResponseDTO(product);
    }

    /**
     * Verifies that products are correctly listed by status without search.
     */
    @Test
    void listProductsInactiveStatusReturnsOnlyInactiveProducts() {
        // Arrange
        Page<Product> mockPage = new PageImpl<>(List.of(inactiveProduct));
        when(productRepository.findByActiveFalse(any(Pageable.class))).thenReturn(mockPage);
        when(productMapper.toProductResponseDTO(inactiveProduct))
            .thenReturn(inactiveProductResponseDTO);

        // Act - now with null searchText
        Page<ProductResponseDTO> resultado = listOnePageProductsSortedAscByName(ProductStatus.INACTIVE);

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.getTotalElements());
        assertEquals(inactiveProductResponseDTO, resultado.getContent().get(0));
        verify(productRepository).findByActiveFalse(any(Pageable.class));
        verify(productMapper).toProductResponseDTO(inactiveProduct);
    }

    /**
     * Verifies that products are correctly listed by status without search.
     */
    @Test
    void listProductsAllStatusReturnsAllProducts() {
        // Arrange
        List<Product> allProducts = List.of(product, inactiveProduct);
        Page<Product> mockPage = new PageImpl<>(allProducts);
        when(productRepository.findAll(any(Pageable.class))).thenReturn(mockPage);
        when(productMapper.toProductResponseDTO(product)).thenReturn(productResponseDTO);
        when(productMapper.toProductResponseDTO(inactiveProduct))
            .thenReturn(inactiveProductResponseDTO);

        // Act - now with null searchText
        Page<ProductResponseDTO> resultado = listOnePageProductsSortedAscByName(ProductStatus.ALL);

        // Assert
        assertNotNull(resultado);
        assertEquals(2, resultado.getTotalElements());
        assertTrue(resultado.getContent().contains(productResponseDTO));
        assertTrue(resultado.getContent().contains(inactiveProductResponseDTO));
        verify(productRepository).findAll(any(Pageable.class));
        verify(productMapper, times(2)).toProductResponseDTO(any(Product.class));
    }
    
    /**
     * Verifies that search works correctly for active products.
     */
    @Test
    void listProductsActiveStatusAndSearchReturnsMatchingActiveProducts() {
        // Arrange
        String searchTerm = "Test";
        String searchTermWithWildcards = "%test%";
        Page<Product> mockPage = new PageImpl<>(List.of(product));
        
        when(productRepository.findByActiveTrueAndSearch(eq(searchTermWithWildcards), any(Pageable.class)))
            .thenReturn(mockPage);
        when(productMapper.toProductResponseDTO(product)).thenReturn(productResponseDTO);

        // Act
        Page<ProductResponseDTO> resultado = searchProductsOnePageResultSorteAscByName(
                                                ProductStatus.ACTIVE, searchTerm);

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.getTotalElements());
        assertEquals(productResponseDTO, resultado.getContent().get(0));
        verify(productRepository).findByActiveTrueAndSearch(eq(searchTermWithWildcards), any(Pageable.class));
        verify(productMapper).toProductResponseDTO(product);
    }
    
    /**
     * Verifies that search works correctly for inactive products.
     */
    @Test
    void listProductsInactiveStatusAndSearchReturnsMatchingInactiveProducts() {
        // Arrange
        String searchTerm = "Inactive";
        String searchTermWithWildcards = "%inactive%";
        Page<Product> mockPage = new PageImpl<>(List.of(inactiveProduct));
        
        when(productRepository.findByActiveFalseAndSearch(eq(searchTermWithWildcards), any(Pageable.class)))
            .thenReturn(mockPage);
        when(productMapper.toProductResponseDTO(inactiveProduct))
            .thenReturn(inactiveProductResponseDTO);

        // Act
        Page<ProductResponseDTO> resultado = searchProductsOnePageResultSorteAscByName(
                                                ProductStatus.INACTIVE, searchTerm);

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.getTotalElements());
        assertEquals(inactiveProductResponseDTO, resultado.getContent().get(0));
        verify(productRepository).findByActiveFalseAndSearch(eq(searchTermWithWildcards), any(Pageable.class));
        verify(productMapper).toProductResponseDTO(inactiveProduct);
    }
    
    /**
     * Verifies that search works correctly for all products.
     */
    @Test
    void listProductsAllStatusAndSearchReturnsAllMatchingProducts() {
        // Arrange
        String searchTerm = "Product";
        String searchTermWithWildcards = "%product%";
        List<Product> allProducts = List.of(product, inactiveProduct);
        Page<Product> mockPage = new PageImpl<>(allProducts);
        
        when(productRepository.findBySearch(eq(searchTermWithWildcards), any(Pageable.class)))
            .thenReturn(mockPage);
        when(productMapper.toProductResponseDTO(product)).thenReturn(productResponseDTO);
        when(productMapper.toProductResponseDTO(inactiveProduct))
            .thenReturn(inactiveProductResponseDTO);

        // Act
        Page<ProductResponseDTO> resultado = searchProductsOnePageResultSorteAscByName(ProductStatus.ALL, searchTerm);

        // Assert
        assertNotNull(resultado);
        assertEquals(2, resultado.getTotalElements());
        assertTrue(resultado.getContent().contains(productResponseDTO));
        assertTrue(resultado.getContent().contains(inactiveProductResponseDTO));
        verify(productRepository).findBySearch(eq(searchTermWithWildcards), any(Pageable.class));
        verify(productMapper, times(2)).toProductResponseDTO(any(Product.class));
    }
    
    /**
     * Verifies that empty search text is handled correctly.
     */
    @Test
    void listProductsEmptySearchIgnoresSearch() {
        // Arrange
        String searchTerm = "";  // Empty search
        Page<Product> mockPage = new PageImpl<>(List.of(product));
        when(productRepository.findByActiveTrue(any(Pageable.class))).thenReturn(mockPage);
        when(productMapper.toProductResponseDTO(product)).thenReturn(productResponseDTO);

        // Act
        Page<ProductResponseDTO> resultado = searchProductsOnePageResultSorteAscByName(
                                                ProductStatus.ACTIVE, searchTerm);

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.getTotalElements());
        verify(productRepository).findByActiveTrue(any(Pageable.class));
        // Verify search methods were not called
        verify(productRepository, never()).findByActiveTrueAndSearch(anyString(), any(Pageable.class));
    }

    /**
     * Verifies successful product creation.
     */
    @Test
    void createProductCreatesAndReturnsNewProduct() {
        // Arrange
        when(productMapper.toProduct(productDTO)).thenReturn(product);
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(productMapper.toProductResponseDTO(product)).thenReturn(productResponseDTO);

        // Act
        ProductResponseDTO resultado = productService.createProduct(productDTO);

        // Assert
        assertNotNull(resultado);
        assertEquals(productResponseDTO.id(), resultado.id());
        assertEquals(productResponseDTO.name(), resultado.name());
        assertNotNull(resultado.creationDate());
        verify(productMapper).toProduct(productDTO);
        verify(productRepository).save(any(Product.class));
        verify(productMapper).toProductResponseDTO(product);
    }

    /**
     * Verifies product deactivation.
     */
    @Test
    void removeProductDeactivatesExistingProduct() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // Act
        productService.removeProduct(1L);

        // Assert
        assertFalse(product.isActive());
        verify(productRepository).findById(1L);
        verify(productRepository).save(product);
    }

    @Test
    void removeProductThrowsExceptionWhenProductDoesNotExist() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> 
            productService.removeProduct(1L)
        );
        verify(productRepository).findById(1L);
        verify(productRepository, never()).save(any());
    }

    @Test
    void createProductHandlesDatabaseErrors() {
        // Arrange
        when(productMapper.toProduct(productDTO)).thenReturn(product);
        when(productRepository.save(any(Product.class)))
            .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
            productService.createProduct(productDTO)
        );
        verify(productMapper).toProduct(productDTO);
        verify(productRepository).save(any(Product.class));
    }

    /**
     * Verifies successful product update.
     */
    @Test
    void updateProductUpdatesAndReturnsExistingProduct() {
        // Arrange
        Product updatedProduct = new Product();
        updatedProduct.setId(1L);
        updatedProduct.setName(TEST_PRODUCT_NAME);
        updatedProduct.setDescription(TEST_PRODUCT_DESC);
        updatedProduct.setPrice(PRICE_99);
        updatedProduct.setCreationDate(product.getCreationDate());
        updatedProduct.setActive(true);

        ProductResponseDTO updatedProductResponseDTO = new ProductResponseDTO(
            1L,
            TEST_PRODUCT_NAME,
            TEST_PRODUCT_DESC,
            PRICE_99,
            product.getCreationDate(),
            true
        );

        ProductDTO updatedProductDTO = new ProductDTO(
            TEST_PRODUCT_NAME,
            TEST_PRODUCT_DESC,
            PRICE_99,
            true
        );

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        doNothing().when(productMapper).updateProductFromDTO(updatedProductDTO, product);
        when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);
        when(productMapper.toProductResponseDTO(updatedProduct))
            .thenReturn(updatedProductResponseDTO);

        // Act
        ProductResponseDTO resultado = productService.updateProduct(1L, updatedProductDTO);

        // Assert
        assertNotNull(resultado);
        assertEquals(updatedProductResponseDTO.id(), resultado.id());
        assertEquals(updatedProductResponseDTO.name(), resultado.name());
        assertEquals(updatedProductResponseDTO.description(), resultado.description());
        assertEquals(updatedProductResponseDTO.price(), resultado.price());
        verify(productRepository).findById(1L);
        verify(productMapper).updateProductFromDTO(updatedProductDTO, product);
        verify(productRepository).save(any(Product.class));
        verify(productMapper).toProductResponseDTO(updatedProduct);
    }

    /**
     * Verifies that updating a non-existent product throws ResourceNotFoundException.
     */
    @Test
    void updateProductThrowsExceptionWhenProductDoesNotExist() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> 
            productService.updateProduct(1L, productDTO)
        );
        verify(productRepository).findById(1L);
        verify(productMapper, never()).updateProductFromDTO(any(), any());
        verify(productRepository, never()).save(any());
    }

    /**
     * Verifies that database errors during update are properly handled.
     */
    @Test
    void updateProductHandlesDatabaseErrors() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        doNothing().when(productMapper).updateProductFromDTO(productDTO, product);
        when(productRepository.save(any(Product.class)))
            .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
            productService.updateProduct(1L, productDTO)
        );
        verify(productRepository).findById(1L);
        verify(productMapper).updateProductFromDTO(productDTO, product);
        verify(productRepository).save(any(Product.class));
    }
}
