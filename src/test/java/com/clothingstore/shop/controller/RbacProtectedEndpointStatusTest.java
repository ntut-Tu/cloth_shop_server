package com.clothingstore.shop.controller;

import com.clothingstore.shop.dto.request.refund.RefundDetailResponseDTO;
import com.clothingstore.shop.service.OrderService;
import com.clothingstore.shop.service.ProductService;
import com.clothingstore.shop.service.RefundService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

class RbacProtectedEndpointStatusTest {
    @Test
    void productStatusReturnsUnauthorizedForInvalidToken() {
        ProductService service = mock(ProductService.class);
        doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED))
                .when(service).updateProductStatus("broken", 23, false);

        var response = new ProductController(service).updateProductStatus(request(), 23, false);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void orderListingReturnsForbiddenForRevokedGrant() throws Exception {
        OrderService service = mock(OrderService.class);
        when(service.getUserOrderSummaries("broken", 1, 10))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        var response = new OrderController(service).getUserOrderSummaries(request(), 1, 10);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void refundUpdateReturnsForbiddenForRevokedGrant() {
        RefundService service = mock(RefundService.class);
        RefundDetailResponseDTO body = new RefundDetailResponseDTO();
        when(service.updateRefund(body, "broken", 42))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        var response = new RefundController(service).updateRefund(request(), 42, body);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    private static MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("token", "broken"));
        return request;
    }
}
