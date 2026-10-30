package com.clothingstore.shop.controller;

import com.clothingstore.shop.dto.request.product.ProductStockUpdateRequestDTO;
import com.clothingstore.shop.service.ProductService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class BulkProductStockControllerTest {

    @Test
    void returnsUpdatedCount() {
        ProductService service = mock(ProductService.class);
        HttpServletRequest request = requestWithToken();
        List<ProductStockUpdateRequestDTO> updates = List.of(new ProductStockUpdateRequestDTO(23, 5));
        when(service.updateProductStockBulk("token", updates)).thenReturn(1);

        var response = new ProductController(service).updateProductStockBulk(request, updates);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getData());
    }

    @Test
    void returnsUnauthorizedWithoutToken() {
        ProductService service = mock(ProductService.class);

        var response = new ProductController(service).updateProductStockBulk(
                mock(HttpServletRequest.class), List.of(new ProductStockUpdateRequestDTO(23, 5)));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(service);
    }

    @Test
    void mapsValidationAndAuthorizationFailuresToHttpStatus() {
        ProductService service = mock(ProductService.class);
        HttpServletRequest request = requestWithToken();
        List<ProductStockUpdateRequestDTO> updates = List.of(new ProductStockUpdateRequestDTO(23, -1));
        when(service.updateProductStockBulk("token", updates))
                .thenThrow(new IllegalArgumentException("Invalid stock"))
                .thenThrow(new SecurityException("Forbidden"));

        ProductController controller = new ProductController(service);
        assertEquals(HttpStatus.BAD_REQUEST, controller.updateProductStockBulk(request, updates).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, controller.updateProductStockBulk(request, updates).getStatusCode());
    }

    private HttpServletRequest requestWithToken() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("token", "token")});
        return request;
    }
}
