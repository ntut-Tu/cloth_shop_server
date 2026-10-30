package com.clothingstore.shop.controller;

import com.clothingstore.shop.exceptions.SharedException;
import com.clothingstore.shop.service.CheckoutService;
import com.clothingstore.shop.service.DiscountService;
import com.clothingstore.shop.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CheckoutQuoteControllerTest {
    private final CheckoutService service = mock(CheckoutService.class);
    private final CheckoutController controller = new CheckoutController(mock(DiscountService.class),
            mock(JwtService.class), service);

    @Test
    void missingTokenReturnsUnauthorizedWithoutLookingUpQuote() {
        var response = controller.getTemporaryQuote(new MockHttpServletRequest(), "quote-1");

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertFalse(response.getBody().isStatus());
        verifyNoInteractions(service);
    }

    @Test
    void invalidTokenReturnsUnauthorized() throws Exception {
        when(service.getTemporaryQuote("quote-1", "bad-token"))
                .thenThrow(new JwtException("Malformed token"));

        var response = controller.getTemporaryQuote(request("bad-token"), "quote-1");

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void blankOrderIdReturnsBadRequest() throws Exception {
        when(service.getTemporaryQuote(" ", "valid-token"))
                .thenThrow(new IllegalArgumentException("Order ID is required"));

        var response = controller.getTemporaryQuote(request("valid-token"), " ");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void nonOwnerReturnsForbidden() throws Exception {
        when(service.getTemporaryQuote("quote-1", "valid-token"))
                .thenThrow(new SecurityException("Order does not belong to customer"));

        var response = controller.getTemporaryQuote(request("valid-token"), "quote-1");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void expiredQuoteReturnsNotFound() throws Exception {
        when(service.getTemporaryQuote("quote-1", "valid-token"))
                .thenThrow(new SharedException("Temporary order expired or not found"));

        var response = controller.getTemporaryQuote(request("valid-token"), "quote-1");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    private MockHttpServletRequest request(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("token", token));
        return request;
    }
}
