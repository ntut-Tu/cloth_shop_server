package com.clothingstore.shop.service;

import com.clothingstore.shop.components.TemporaryOrderStorage;
import com.clothingstore.shop.dto.others.tempOrder.TemporaryOrder;
import com.clothingstore.shop.dto.response.checkout.TemporaryOrderQuoteResponseDTO;
import com.clothingstore.shop.exceptions.SharedException;
import com.clothingstore.shop.repository.CheckoutRepository;
import com.clothingstore.shop.repository.DiscountRepository;
import com.clothingstore.shop.repository.InventoryRepository;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TemporaryOrderQuoteServiceTest {
    private final JwtService jwtService = mock(JwtService.class);
    private final TemporaryOrderStorage storage = mock(TemporaryOrderStorage.class);
    private final InventoryRepository inventory = mock(InventoryRepository.class);
    private final CheckoutRepository checkoutRepository = mock(CheckoutRepository.class);
    private final DiscountRepository discountRepository = mock(DiscountRepository.class);
    private final CheckoutService service = new CheckoutService(jwtService, checkoutRepository, storage,
            inventory, mock(DiscountService.class), discountRepository);

    @Test
    void returnsCurrentQuoteOnlyToItsCustomer() throws Exception {
        long expiresAt = Instant.parse("2100-01-01T00:00:00Z").toEpochMilli();
        TemporaryOrder order = quote(12, expiresAt);
        when(jwtService.extractUserId("owner-token")).thenReturn(12);
        when(storage.getTemporaryOrder("quote-1")).thenReturn(order);

        TemporaryOrderQuoteResponseDTO response = service.getTemporaryQuote("quote-1", "owner-token");

        assertEquals(new TemporaryOrderQuoteResponseDTO("quote-1", 200, 30, 20, 210,
                "2100-01-01T00:00:00Z"), response);
        verifyNoInteractions(inventory, checkoutRepository);
    }

    @Test
    void rejectsQuoteOwnedByAnotherCustomer() {
        when(jwtService.extractUserId("other-token")).thenReturn(99);
        when(storage.getTemporaryOrder("quote-1")).thenReturn(quote(12,
                Instant.parse("2100-01-01T00:00:00Z").toEpochMilli()));

        assertThrows(SecurityException.class,
                () -> service.getTemporaryQuote("quote-1", "other-token"));
        verifyNoInteractions(inventory, checkoutRepository);
    }

    @Test
    void rejectsTokenWithoutCustomerIdBeforeReadingQuote() {
        when(jwtService.extractUserId("invalid-token")).thenReturn(null);

        assertThrows(JwtException.class,
                () -> service.getTemporaryQuote("quote-1", "invalid-token"));
        verifyNoInteractions(storage);
    }

    @Test
    void expiredOrMissingQuotesAreUnavailable() {
        when(jwtService.extractUserId("owner-token")).thenReturn(12);
        when(storage.getTemporaryOrder("expired")).thenReturn(quote(12, 0));

        assertThrows(SharedException.class, () -> service.getTemporaryQuote("expired", "owner-token"));
        assertThrows(SharedException.class, () -> service.getTemporaryQuote("missing", "owner-token"));
    }

    private TemporaryOrder quote(int customerId, long expiresAt) {
        TemporaryOrder order = new TemporaryOrder(discountRepository);
        order.setCustomerId(customerId);
        order.setSubtotal(200);
        order.setShippingFee(30);
        order.setTotalStoreDiscountAmount(20);
        order.setTotalAmount(210);
        order.setExpirationTime(expiresAt);
        return order;
    }
}
