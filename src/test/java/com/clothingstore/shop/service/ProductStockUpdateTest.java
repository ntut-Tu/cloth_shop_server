package com.clothingstore.shop.service;

import com.clothingstore.shop.dto.request.product.ProductStockUpdateRequestDTO;
import com.clothingstore.shop.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ProductStockUpdateTest {

    private ProductRepository productRepository;
    private JwtService jwtService;
    private AuthService authService;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        jwtService = mock(JwtService.class);
        authService = mock(AuthService.class);
        productService = new ProductService(productRepository, jwtService, authService);

        when(jwtService.extractUserId("token")).thenReturn(7);
        when(authService.checkUserExists(7, "vendor")).thenReturn(true);
        when(authService.getVendorId(7)).thenReturn(11);
    }

    @Test
    void acceptsZeroStockForAnAuthorizedVendor() {
        productService.updateProductStock("token", 23, 0);

        verify(productRepository).updateProductStock(11, 23, 0);
    }

    @Test
    void rejectsNegativeOrMissingStockBeforeWriting() {
        IllegalArgumentException negative = assertThrows(IllegalArgumentException.class,
                () -> productService.updateProductStock("token", 23, -1));
        assertEquals("Product stock must be non-negative.", negative.getMessage());
        assertThrows(IllegalArgumentException.class,
                () -> productService.updateProductStock("token", 23, null));

        verifyNoInteractions(productRepository);
    }

    @Test
    void rejectsInvalidVariantIdsBeforeWriting() {
        assertThrows(IllegalArgumentException.class,
                () -> productService.updateProductStock("token", 0, 5));
        assertThrows(IllegalArgumentException.class,
                () -> productService.updateProductStock("token", null, 5));

        verifyNoInteractions(productRepository);
    }

    @Test
    void rejectsNonVendorsBeforeWriting() {
        when(authService.checkUserExists(7, "vendor")).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> productService.updateProductStock("token", 23, 5));

        verifyNoInteractions(productRepository);
    }

    @Test
    void updatesMultipleVariantsForTheSameVendor() {
        int count = productService.updateProductStockBulk("token", List.of(
                new ProductStockUpdateRequestDTO(23, 0),
                new ProductStockUpdateRequestDTO(24, 8)));

        assertEquals(2, count);
        verify(authService).getVendorId(7);
        verify(productRepository).updateProductStock(11, 23, 0);
        verify(productRepository).updateProductStock(11, 24, 8);
    }

    @Test
    void validatesTheEntireBatchBeforeWriting() {
        assertThrows(IllegalArgumentException.class, () -> productService.updateProductStockBulk("token", List.of(
                new ProductStockUpdateRequestDTO(23, 4),
                new ProductStockUpdateRequestDTO(24, -1))));
        assertThrows(IllegalArgumentException.class, () -> productService.updateProductStockBulk("token", List.of(
                new ProductStockUpdateRequestDTO(23, 4),
                new ProductStockUpdateRequestDTO(23, 5))));
        assertThrows(IllegalArgumentException.class,
                () -> productService.updateProductStockBulk("token", List.of()));

        verifyNoInteractions(productRepository);
    }

    @Test
    void rejectsNonVendorsForBulkUpdates() {
        when(authService.checkUserExists(7, "vendor")).thenReturn(false);

        assertThrows(SecurityException.class,
                () -> productService.updateProductStockBulk("token", List.of(new ProductStockUpdateRequestDTO(23, 5))));

        verifyNoInteractions(productRepository);
    }

    @Test
    void propagatesMissingVariantFailures() {
        doThrow(new IllegalArgumentException("Product variant not found"))
                .when(productRepository).updateProductStock(11, 24, 8);

        assertThrows(IllegalArgumentException.class, () -> productService.updateProductStockBulk("token", List.of(
                new ProductStockUpdateRequestDTO(23, 4),
                new ProductStockUpdateRequestDTO(24, 8))));
    }
}
