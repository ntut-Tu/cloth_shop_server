package com.clothingstore.shop.controller;

import com.clothingstore.shop.dto.request.accounting.ReverseJournalRequest;
import com.clothingstore.shop.dto.response.ApiResponseDTO;
import com.clothingstore.shop.dto.response.accounting.AccountingAccountBalanceResponse;
import com.clothingstore.shop.dto.response.accounting.AccountingJournalResponse;
import com.clothingstore.shop.dto.response.accounting.AccountingReconciliationResponse;
import com.clothingstore.shop.service.AccountingJournalService;
import com.clothingstore.shop.utils.TokenUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/accounting")
public class AccountingJournalController {
    private final AccountingJournalService service;

    public AccountingJournalController(AccountingJournalService service) {
        this.service = service;
    }

    @GetMapping("/journals/{journalId}")
    public ResponseEntity<ApiResponseDTO<AccountingJournalResponse>> getJournal(
            HttpServletRequest request, @PathVariable long journalId) {
        try {
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Journal fetched",
                    service.getJournal(TokenUtils.extractTokenFromCookies(request), journalId)));
        } catch (Exception error) {
            return failure(error);
        }
    }

    @PostMapping("/journals/{journalId}/reversal")
    public ResponseEntity<ApiResponseDTO<AccountingJournalResponse>> reverse(
            HttpServletRequest request, @PathVariable long journalId,
            @RequestBody ReverseJournalRequest body) {
        try {
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Reversal posted",
                    service.reverse(TokenUtils.extractTokenFromCookies(request), journalId, body)));
        } catch (Exception error) {
            return failure(error);
        }
    }

    @GetMapping("/reconciliation")
    public ResponseEntity<ApiResponseDTO<AccountingReconciliationResponse>> reconcile(
            HttpServletRequest request,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate orderDate) {
        try {
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Reconciliation calculated",
                    service.reconcile(TokenUtils.extractTokenFromCookies(request), orderDate)));
        } catch (Exception error) {
            return failure(error);
        }
    }

    @GetMapping("/balances")
    public ResponseEntity<ApiResponseDTO<List<AccountingAccountBalanceResponse>>> balances(
            HttpServletRequest request,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate throughDate) {
        try {
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Balances calculated",
                    service.accountBalances(TokenUtils.extractTokenFromCookies(request), throughDate)));
        } catch (Exception error) {
            return failure(error);
        }
    }

    private <T> ResponseEntity<ApiResponseDTO<T>> failure(Exception error) {
        HttpStatus status = error instanceof SecurityException ? HttpStatus.FORBIDDEN
                : error instanceof IllegalArgumentException ? HttpStatus.BAD_REQUEST
                : error instanceof IllegalStateException ? HttpStatus.CONFLICT
                : HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status)
                .body(new ApiResponseDTO<>(false,
                        status == HttpStatus.INTERNAL_SERVER_ERROR
                                ? "Accounting operation failed" : error.getMessage(), null));
    }
}
