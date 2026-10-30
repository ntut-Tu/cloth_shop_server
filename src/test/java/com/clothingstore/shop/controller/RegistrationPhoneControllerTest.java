package com.clothingstore.shop.controller;

import com.clothingstore.shop.dto.request.auth.RegisterRequestDTO;
import com.clothingstore.shop.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class RegistrationPhoneControllerTest {

    @Test
    void returnsBadRequestForInvalidPhoneNumber() {
        AuthService service = mock(AuthService.class);
        AuthController controller = new AuthController();
        ReflectionTestUtils.setField(controller, "authService", service);
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setPhoneNumber("1".repeat(51));
        when(service.register(request)).thenThrow(new IllegalArgumentException("Phone number must be at most 50 characters."));

        var response = controller.registerDetails(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Phone number must be at most 50 characters.", response.getBody().getMessage());
    }
}
