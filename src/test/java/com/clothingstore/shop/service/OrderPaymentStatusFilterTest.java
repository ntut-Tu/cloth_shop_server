package com.clothingstore.shop.service;

import com.clothingstore.shop.repository.OrderRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OrderPaymentStatusFilterTest {
    private final OrderRepository repository = mock(OrderRepository.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final OrderService service = new OrderService(repository, jwtService, mock(AuthService.class));

    @Test
    void customerFilterIsNormalizedAndAppliedBeforePagination() throws Exception {
        when(jwtService.extractUserId("token")).thenReturn(12);
        when(jwtService.extractRole("token")).thenReturn("customer");
        when(repository.findOrderSummariesByCustomerId(12, 10, 10, "PAID")).thenReturn(List.of());

        assertEquals(List.of(), service.getUserOrderSummaries("token", 2, 10, " paid "));
        verify(repository).findOrderSummariesByCustomerId(12, 10, 10, "PAID");
    }

    @Test
    void adminCanFilterAllOrders() throws Exception {
        when(jwtService.extractUserId("token")).thenReturn(3);
        when(jwtService.extractRole("token")).thenReturn("admin");

        service.getUserOrderSummaries("token", 1, 20, "cancelled");

        verify(repository).findAllOrderSummaries(20, 0, "CANCELLED");
    }

    @Test
    void missingStatusKeepsUnfilteredListing() throws Exception {
        when(jwtService.extractUserId("token")).thenReturn(12);
        when(jwtService.extractRole("token")).thenReturn("customer");

        service.getUserOrderSummaries("token", 1, 10, null);

        verify(repository).findOrderSummariesByCustomerId(12, 10, 0, null);
    }

    @Test
    void invalidStatusAndPaginationNeverReachRepository() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getUserOrderSummaries("token", 1, 10, "refunded"));
        assertThrows(IllegalArgumentException.class,
                () -> service.getUserOrderSummaries("token", 0, 10, "paid"));
        assertThrows(IllegalArgumentException.class,
                () -> service.getUserOrderSummaries("token", 1, 101, "paid"));
        assertThrows(IllegalArgumentException.class,
                () -> service.getUserOrderSummaries("token", Integer.MAX_VALUE, 100, "paid"));
        verifyNoInteractions(repository);
    }
}
