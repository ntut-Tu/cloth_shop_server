package com.clothingstore.shop.controller;

import com.clothingstore.shop.dto.response.ApiResponseDTO;
import com.clothingstore.shop.dto.response.auth.CurrentUserResponseDTO;
import com.clothingstore.shop.service.AuthService;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CurrentUserControllerTest {

    @Test
    void returnsCurrentUserFromCookie() {
        AuthService authService = mock(AuthService.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("token", "token")});
        CurrentUserResponseDTO user = new CurrentUserResponseDTO(7, "shopper", "shopper@example.com", "customer");
        when(authService.getCurrentUser("token")).thenReturn(Optional.of(user));

        ResponseEntity<ApiResponseDTO<CurrentUserResponseDTO>> response =
                controller(authService).currentUser(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(user, response.getBody().getData());
    }

    @Test
    void requiresValidActiveSession() {
        AuthService authService = mock(AuthService.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        AuthController controller = controller(authService);

        assertEquals(HttpStatus.UNAUTHORIZED, controller.currentUser(request).getStatusCode());

        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("token", "token")});
        when(authService.getCurrentUser("token")).thenReturn(Optional.empty());
        assertEquals(HttpStatus.UNAUTHORIZED, controller.currentUser(request).getStatusCode());

        when(authService.getCurrentUser("token")).thenThrow(new MalformedJwtException("bad token"));
        assertEquals(HttpStatus.UNAUTHORIZED, controller.currentUser(request).getStatusCode());
    }

    private AuthController controller(AuthService authService) {
        AuthController controller = new AuthController();
        ReflectionTestUtils.setField(controller, "authService", authService);
        return controller;
    }
}
