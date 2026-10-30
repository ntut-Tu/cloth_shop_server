package com.clothingstore.shop.service;

import com.clothingstore.shop.dto.request.auth.RegisterRequestDTO;
import com.clothingstore.shop.dto.repository.users.UserRepositoryDTO;
import com.clothingstore.shop.repository.AuthRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceRbacTest {
    private final JwtService jwtService = mock(JwtService.class);
    private final AuthRepository repository = mock(AuthRepository.class);
    private final AuthService service = new AuthService(jwtService, repository);

    @Test
    void publicRegistrationRejectsAdministrativeRole() {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setRole("admin");
        assertFalse(service.register(request));
        verify(repository, never()).saveUser(any(), any());
    }

    @Test
    void publicRegistrationStillAcceptsCustomerRole() {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setRole("customer");
        request.setAccount("buyer");
        when(repository.findByUserAccountAndRole("buyer", "customer")).thenReturn(Optional.empty());
        when(repository.saveUser(any(UserRepositoryDTO.class), eq(request))).thenReturn(true);
        assertTrue(service.register(request));
    }

    @Test
    void inactiveUserIsNotAcceptedByLegacyRoleChecks() {
        UserRepositoryDTO user = new UserRepositoryDTO();
        user.setIsActive(false);
        when(repository.findByUserIDAndRole(7, "admin")).thenReturn(Optional.of(user));
        assertFalse(service.checkUserExists(7, "admin"));
    }
}
