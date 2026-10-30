package com.clothingstore.shop.service;

import com.clothingstore.shop.dto.request.accounting.ReverseJournalRequest;
import com.clothingstore.shop.dto.response.accounting.AccountingJournalResponse;
import com.clothingstore.shop.dto.response.accounting.AccountingReconciliationResponse;
import com.clothingstore.shop.repository.AccountingJournalRepository;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountingJournalServiceTest {
    @Mock
    private AccountingJournalRepository repository;
    @Mock
    private JwtService jwtService;
    private AccountingJournalService service;

    @BeforeEach
    void setUp() {
        service = new AccountingJournalService(repository, jwtService);
    }

    @Test
    void rejectsUnauthenticatedAccessBeforeReadingAccountingData() {
        assertThrows(SecurityException.class,
                () -> service.getJournal(null, 7));
        verifyNoInteractions(jwtService, repository);
    }

    @Test
    void rejectsNonAdminEvenWhenTokenContainsAUserId() {
        when(jwtService.extractUserId("vendor-token")).thenReturn(3);
        when(jwtService.extractRole("vendor-token")).thenReturn("vendor");
        assertThrows(SecurityException.class,
                () -> service.accountBalances("vendor-token", LocalDate.of(2026, 10, 1)));
        verifyNoInteractions(repository);
    }

    @Test
    void malformedTokenCannotReadAccountingData() {
        when(jwtService.extractUserId("malformed")).thenThrow(new MalformedJwtException("invalid"));
        assertThrows(SecurityException.class,
                () -> service.getJournal("malformed", 7));
        verifyNoInteractions(repository);
    }

    @Test
    void postsReversalWithStableEventKeyAndReturnsItsJournal() {
        when(jwtService.extractUserId("admin-token")).thenReturn(5);
        when(jwtService.extractRole("admin-token")).thenReturn("admin");
        when(repository.reverse(11L, "manual-reversal:correction-0001", "Wrong settlement", 5))
                .thenReturn(17L);
        AccountingJournalResponse reversal = new AccountingJournalResponse(
                17L, 31, "REVERSAL", "manual-reversal:correction-0001",
                null, 11L, 5, "Wrong settlement", List.of());
        when(repository.findJournal(17L)).thenReturn(reversal);

        AccountingJournalResponse result = service.reverse("admin-token", 11L,
                new ReverseJournalRequest("correction-0001", "  Wrong settlement  "));

        assertEquals(17L, result.journalId());
        assertEquals(11L, result.reversesJournalId());
        verify(repository).reverse(11L, "manual-reversal:correction-0001", "Wrong settlement", 5);
    }

    @Test
    void rejectsMalformedIdempotencyKeyBeforePosting() {
        when(jwtService.extractUserId("admin-token")).thenReturn(5);
        when(jwtService.extractRole("admin-token")).thenReturn("admin");
        assertThrows(IllegalArgumentException.class,
                () -> service.reverse("admin-token", 11L,
                        new ReverseJournalRequest("x", "Wrong settlement")));
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsBlankReasonBeforePosting() {
        when(jwtService.extractUserId("admin-token")).thenReturn(5);
        when(jwtService.extractRole("admin-token")).thenReturn("admin");
        assertThrows(IllegalArgumentException.class,
                () -> service.reverse("admin-token", 11L,
                        new ReverseJournalRequest("correction-0001", "  ")));
        verifyNoInteractions(repository);
    }

    @Test
    void adminCanInspectDailyReconciliation() {
        when(jwtService.extractUserId("admin-token")).thenReturn(5);
        when(jwtService.extractRole("admin-token")).thenReturn("admin");
        LocalDate day = LocalDate.of(2026, 10, 1);
        AccountingReconciliationResponse report = new AccountingReconciliationResponse(
                day, 0, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), true);
        when(repository.reconcile(day)).thenReturn(report);

        assertTrue(service.reconcile("admin-token", day).reconciled());
        verify(repository).reconcile(day);
    }
}
