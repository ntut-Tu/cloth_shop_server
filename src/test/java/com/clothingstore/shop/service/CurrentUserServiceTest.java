package com.clothingstore.shop.service;

import com.clothingstore.shop.dto.repository.users.UserRepositoryDTO;
import com.clothingstore.shop.dto.response.auth.CurrentUserResponseDTO;
import com.clothingstore.shop.repository.AuthRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CurrentUserServiceTest {

    private JwtService jwtService;
    private AuthRepository authRepository;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        authRepository = mock(AuthRepository.class);
        authService = new AuthService(jwtService, authRepository);
        when(jwtService.extractUserId("token")).thenReturn(7);
        when(jwtService.extractRole("token")).thenReturn("customer");
    }

    @Test
    void returnsSafeProfileForActiveUser() {
        UserRepositoryDTO user = new UserRepositoryDTO();
        user.setId(7);
        user.setAccount("shopper");
        user.setEmail("shopper@example.com");
        user.setUserType("customer");
        user.setPassword("secret");
        user.setIsActive(true);
        when(authRepository.findByUserIDAndRole(7, "customer")).thenReturn(Optional.of(user));

        CurrentUserResponseDTO result = authService.getCurrentUser("token").orElseThrow();

        assertEquals(7, result.userId());
        assertEquals("shopper", result.account());
        assertEquals("shopper@example.com", result.email());
        assertEquals("customer", result.role());
    }

    @Test
    void hidesDeletedAndInactiveUsers() {
        when(authRepository.findByUserIDAndRole(7, "customer")).thenReturn(Optional.empty());
        assertTrue(authService.getCurrentUser("token").isEmpty());

        UserRepositoryDTO inactive = new UserRepositoryDTO();
        inactive.setIsActive(false);
        when(authRepository.findByUserIDAndRole(7, "customer")).thenReturn(Optional.of(inactive));
        assertTrue(authService.getCurrentUser("token").isEmpty());
    }

    @Test
    void ignoresTokenWithoutAUserId() {
        when(jwtService.extractUserId("token")).thenReturn(null);

        assertTrue(authService.getCurrentUser("token").isEmpty());
        verifyNoInteractions(authRepository);
    }
}
