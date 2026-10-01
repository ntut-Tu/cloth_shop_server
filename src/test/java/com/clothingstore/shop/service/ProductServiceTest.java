package com.clothingstore.shop.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.clothingstore.shop.dto.request.AddProductRequestDTO;
import com.clothingstore.shop.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ProductServiceTest {
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
    }

    @AfterEach
    void verifyOnlyExpectedCollaboratorCalls() {
        verifyNoMoreInteractions(productRepository, jwtService, authService);
    }

    @Test
    @DisplayName("Authorized vendor ID replaces the client-provided ID before saving")
    void authorizedVendorUsesServerSideVendorId() {
        AddProductRequestDTO request = new AddProductRequestDTO();
        request.setName("Test shirt");
        request.setFkVendorId(999);
        when(jwtService.extractUserId("vendor-token")).thenReturn(7);
        when(authService.checkUserExists(7, "vendor")).thenReturn(true);
        when(authService.getVendorId(7)).thenReturn(42);
        when(productRepository.addProduct(any(AddProductRequestDTO.class))).thenReturn(101);

        Integer productId = productService.addProduct("vendor-token", request);

        verify(jwtService).extractUserId("vendor-token");
        verify(authService).checkUserExists(7, "vendor");
        verify(authService).getVendorId(7);
        ArgumentCaptor<AddProductRequestDTO> saved = ArgumentCaptor.forClass(AddProductRequestDTO.class);
        verify(productRepository).addProduct(saved.capture());
        assertAll(
                () -> assertEquals(101, productId),
                () -> assertSame(request, saved.getValue()),
                () -> assertEquals(42, saved.getValue().getFkVendorId()));
    }

    @Test
    @DisplayName("Unauthorized user cannot save a product")
    void unauthorizedUserNeverReachesRepository() {
        AddProductRequestDTO request = new AddProductRequestDTO();
        request.setFkVendorId(999);
        when(jwtService.extractUserId("customer-token")).thenReturn(8);
        when(authService.checkUserExists(8, "vendor")).thenReturn(false);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> productService.addProduct("customer-token", request));

        assertAll(
                () -> assertEquals("User is not authorized to add products for this vendor.", error.getMessage()),
                () -> assertEquals(999, request.getFkVendorId()));
        verify(jwtService).extractUserId("customer-token");
        verify(authService).checkUserExists(8, "vendor");
        verifyNoInteractions(productRepository);
    }
}
