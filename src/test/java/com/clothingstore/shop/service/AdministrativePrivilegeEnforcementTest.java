package com.clothingstore.shop.service;

import com.clothingstore.shop.dto.request.refund.RefundDetailResponseDTO;
import com.clothingstore.shop.repository.OrderRepository;
import com.clothingstore.shop.repository.ProductRepository;
import com.clothingstore.shop.repository.RbacRepository;
import com.clothingstore.shop.repository.RefundRepository;
import com.clothingstore.shop.security.Privilege;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AdministrativePrivilegeEnforcementTest {
    private final JwtService jwt = mock(JwtService.class);
    private final RbacService rbac = mock(RbacService.class);
    private final AuthService auth = mock(AuthService.class);

    @Test
    void revokedOrderReadGrantBlocksAdminOrderListing() throws Exception {
        OrderRepository orders = mock(OrderRepository.class);
        OrderService service = new OrderService(orders, jwt, auth, rbac);
        when(rbac.requireActiveUser("jwt")).thenReturn(7);
        when(jwt.extractRole("jwt")).thenReturn("admin");
        when(rbac.requirePermission("jwt", Privilege.ADMIN_ORDERS_READ))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.getUserOrderSummaries("jwt", 1, 10));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verifyNoInteractions(orders);
    }

    @Test
    void grantedOrderReadAllowsAdminOrderListing() throws Exception {
        OrderRepository orders = mock(OrderRepository.class);
        OrderService service = new OrderService(orders, jwt, auth, rbac);
        when(rbac.requireActiveUser("jwt")).thenReturn(7);
        when(jwt.extractRole("jwt")).thenReturn("admin");
        when(orders.findAllOrderSummaries(10, 0)).thenReturn(List.of());

        assertEquals(List.of(), service.getUserOrderSummaries("jwt", 1, 10));
        verify(rbac).requirePermission("jwt", Privilege.ADMIN_ORDERS_READ);
        verify(orders).findAllOrderSummaries(10, 0);
    }

    @Test
    void revokedRefundGrantBlocksAdminBeforeRefundLookup() {
        RefundRepository refunds = mock(RefundRepository.class);
        RefundService service = new RefundService(jwt, refunds, auth, rbac);
        when(rbac.requireActiveUser("jwt")).thenReturn(7);
        when(jwt.extractRole("jwt")).thenReturn("admin");
        when(rbac.requirePermission("jwt", Privilege.ADMIN_REFUNDS_MANAGE))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.updateRefund(new RefundDetailResponseDTO(), "jwt", 42));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verifyNoInteractions(refunds);
    }

    @Test
    void grantedRefundPrivilegeAllowsAdminUpdate() {
        RefundRepository refunds = mock(RefundRepository.class);
        RefundService service = new RefundService(jwt, refunds, auth, rbac);
        RefundDetailResponseDTO request = new RefundDetailResponseDTO();
        when(rbac.requireActiveUser("jwt")).thenReturn(7);
        when(jwt.extractRole("jwt")).thenReturn("admin");
        when(refunds.getRefundStatus(42)).thenReturn("admin_pending");
        when(auth.getAdminId(7)).thenReturn(3);
        when(refunds.updateRefund(request, 42, 3, "admin")).thenReturn(1);

        assertEquals(1, service.updateRefund(request, "jwt", 42));
        verify(rbac).requirePermission("jwt", Privilege.ADMIN_REFUNDS_MANAGE);
        verify(refunds).updateRefund(request, 42, 3, "admin");
    }

    @Test
    void malformedProductStatusTokenIsUnauthorizedBeforeMutation() {
        ProductRepository products = mock(ProductRepository.class);
        RbacService realRbac = new RbacService(jwt, mock(RbacRepository.class));
        ProductService service = new ProductService(products, jwt, auth, realRbac);
        when(jwt.extractUserId("broken")).thenThrow(new JwtException("bad signature"));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.updateProductStatus("broken", 23, false));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        verifyNoInteractions(products);
    }

    @Test
    void inactiveProductStatusAccountIsForbiddenBeforeMutation() {
        ProductRepository products = mock(ProductRepository.class);
        RbacService realRbac = new RbacService(jwt, mock(RbacRepository.class));
        ProductService service = new ProductService(products, jwt, auth, realRbac);
        when(jwt.extractUserId("jwt")).thenReturn(7);
        when(jwt.extractRole("jwt")).thenReturn("admin");

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.updateProductStatus("jwt", 23, false));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verifyNoInteractions(products);
    }
}
