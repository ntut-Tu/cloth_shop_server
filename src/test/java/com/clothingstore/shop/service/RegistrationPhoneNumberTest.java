package com.clothingstore.shop.service;

import com.clothingstore.shop.dto.repository.users.UserRepositoryDTO;
import com.clothingstore.shop.dto.request.auth.RegisterRequestDTO;
import com.clothingstore.shop.repository.AuthRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RegistrationPhoneNumberTest {

    @Test
    void savesNormalizedOptionalPhoneNumber() {
        AuthRepository repository = mock(AuthRepository.class);
        AuthService service = new AuthService(mock(JwtService.class), repository);
        RegisterRequestDTO request = request();
        request.setPhoneNumber("  +886 912 345 678  ");
        when(repository.findByUserAccountAndRole("shopper", "customer")).thenReturn(Optional.empty());
        when(repository.saveUser(any(), same(request))).thenReturn(true);

        assertTrue(service.register(request));

        ArgumentCaptor<UserRepositoryDTO> user = ArgumentCaptor.forClass(UserRepositoryDTO.class);
        verify(repository).saveUser(user.capture(), same(request));
        assertEquals("+886 912 345 678", user.getValue().getPhoneNumber());
    }

    @Test
    void keepsPhoneNumberOptional() {
        AuthRepository repository = mock(AuthRepository.class);
        AuthService service = new AuthService(mock(JwtService.class), repository);
        RegisterRequestDTO request = request();
        when(repository.saveUser(any(), same(request))).thenReturn(true);

        assertTrue(service.register(request));

        ArgumentCaptor<UserRepositoryDTO> user = ArgumentCaptor.forClass(UserRepositoryDTO.class);
        verify(repository).saveUser(user.capture(), same(request));
        assertNull(user.getValue().getPhoneNumber());
    }

    @Test
    void treatsBlankPhoneNumberAsMissing() {
        AuthRepository repository = mock(AuthRepository.class);
        AuthService service = new AuthService(mock(JwtService.class), repository);
        RegisterRequestDTO request = request();
        request.setPhoneNumber("  \t  ");
        when(repository.saveUser(any(), same(request))).thenReturn(true);

        assertTrue(service.register(request));

        ArgumentCaptor<UserRepositoryDTO> user = ArgumentCaptor.forClass(UserRepositoryDTO.class);
        verify(repository).saveUser(user.capture(), same(request));
        assertNull(user.getValue().getPhoneNumber());
    }

    @Test
    void rejectsPhoneNumbersExceedingDatabaseLimit() {
        AuthRepository repository = mock(AuthRepository.class);
        AuthService service = new AuthService(mock(JwtService.class), repository);
        RegisterRequestDTO request = request();
        request.setPhoneNumber("1".repeat(51));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.register(request));

        assertEquals("Phone number must be at most 50 characters.", error.getMessage());
        verify(repository, never()).saveUser(any(), any());
    }

    private RegisterRequestDTO request() {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setAccount("shopper");
        request.setEmail("shopper@example.com");
        request.setPassword("password");
        request.setRole("customer");
        return request;
    }
}
