package com.clothingstore.shop.service;

import com.clothingstore.shop.dto.request.accounting.ReverseJournalRequest;
import com.clothingstore.shop.dto.response.accounting.AccountingAccountBalanceResponse;
import com.clothingstore.shop.dto.response.accounting.AccountingJournalResponse;
import com.clothingstore.shop.dto.response.accounting.AccountingReconciliationResponse;
import com.clothingstore.shop.repository.AccountingJournalRepository;
import io.jsonwebtoken.JwtException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class AccountingJournalService {
    private static final Pattern IDEMPOTENCY_KEY = Pattern.compile("[A-Za-z0-9._-]{8,80}");
    private final AccountingJournalRepository repository;
    private final JwtService jwtService;

    public AccountingJournalService(AccountingJournalRepository repository, JwtService jwtService) {
        this.repository = repository;
        this.jwtService = jwtService;
    }

    public AccountingJournalResponse getJournal(String token, long journalId) {
        requireAdmin(token);
        if (journalId <= 0) {
            throw new IllegalArgumentException("Journal ID must be positive");
        }
        return repository.findJournal(journalId);
    }

    public AccountingJournalResponse reverse(String token, long journalId, ReverseJournalRequest request) {
        int actorUserId = requireAdmin(token);
        if (journalId <= 0) {
            throw new IllegalArgumentException("Journal ID must be positive");
        }
        if (request == null || request.idempotencyKey() == null
                || !IDEMPOTENCY_KEY.matcher(request.idempotencyKey()).matches()) {
            throw new IllegalArgumentException("Idempotency key must be 8 to 80 letters, digits, dots, underscores, or hyphens");
        }
        if (request.reason() == null || request.reason().isBlank()
                || request.reason().length() > 500) {
            throw new IllegalArgumentException("A reversal reason of at most 500 characters is required");
        }
        String eventKey = "manual-reversal:" + request.idempotencyKey();
        long reversalId = repository.reverse(journalId, eventKey, request.reason().trim(), actorUserId);
        return repository.findJournal(reversalId);
    }

    public AccountingReconciliationResponse reconcile(String token, LocalDate orderDate) {
        requireAdmin(token);
        if (orderDate == null) {
            throw new IllegalArgumentException("Order date is required");
        }
        return repository.reconcile(orderDate);
    }

    public List<AccountingAccountBalanceResponse> accountBalances(String token, LocalDate throughDate) {
        requireAdmin(token);
        if (throughDate == null) {
            throw new IllegalArgumentException("Through date is required");
        }
        return repository.accountBalances(throughDate);
    }

    private int requireAdmin(String token) {
        if (token == null || token.isBlank()) {
            throw new SecurityException("Authentication required");
        }
        try {
            Integer userId = jwtService.extractUserId(token);
            String role = jwtService.extractRole(token);
            if (userId != null && "admin".equals(role)) {
                return userId;
            }
        } catch (JwtException | IllegalArgumentException ignored) {
            throw new SecurityException("Authentication required");
        }
        throw new SecurityException("Administrator access required");
    }
}
