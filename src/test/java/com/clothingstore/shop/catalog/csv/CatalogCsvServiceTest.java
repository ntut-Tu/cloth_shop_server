package com.clothingstore.shop.catalog.csv;

import com.clothingstore.shop.dto.repository.users.UserRepositoryDTO;
import com.clothingstore.shop.repository.AuthRepository;
import com.clothingstore.shop.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CatalogCsvServiceTest {
    private final JwtService jwt = mock(JwtService.class);
    private final AuthRepository auth = mock(AuthRepository.class);

    @Test
    void resolvesOnlyActiveVendorAccounts() {
        when(jwt.extractUserId("valid")).thenReturn(5);
        when(jwt.extractRole("valid")).thenReturn("vendor");
        UserRepositoryDTO user = mock(UserRepositoryDTO.class);
        when(user.getIsActive()).thenReturn(true);
        when(auth.findByUserIDAndRole(5, "vendor")).thenReturn(Optional.of(user));
        when(auth.getVendorId(5)).thenReturn(7);
        assertEquals(7, service().requireVendor("valid"));

        when(user.getIsActive()).thenReturn(false);
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service().requireVendor("valid"));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void rejectsMissingAndNonVendorTokens() {
        assertEquals(HttpStatus.UNAUTHORIZED,
                assertThrows(ResponseStatusException.class, () -> service().requireVendor(null)).getStatusCode());
        when(jwt.extractUserId("customer")).thenReturn(5);
        when(jwt.extractRole("customer")).thenReturn("customer");
        assertEquals(HttpStatus.FORBIDDEN,
                assertThrows(ResponseStatusException.class, () -> service().requireVendor("customer")).getStatusCode());
    }

    private CatalogCsvService service() {
        CatalogCsvProperties properties = new CatalogCsvProperties();
        return new CatalogCsvService(jwt, auth, mock(CatalogCsvGateway.class),
                mock(CatalogCsvMutationRepository.class), mock(CatalogCsvTemplate.class), new CatalogCsvCodec(), properties);
    }
}
