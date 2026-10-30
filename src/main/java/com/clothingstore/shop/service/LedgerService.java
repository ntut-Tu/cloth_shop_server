package com.clothingstore.shop.service;

import com.clothingstore.shop.dto.response.ledger.PlatformLedgerResponseDTO;
import com.clothingstore.shop.dto.response.ledger.VendorLedgerResponseDTO;
import com.clothingstore.shop.repository.LedgerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class LedgerService {
    private final JwtService jwtService;
    private final LedgerRepository ledgerRepository;
    private final AuthService authService;

    @Autowired
    LedgerService(JwtService jwtService, LedgerRepository ledgerRepository, AuthService authService){
        this.jwtService = jwtService;
        this.ledgerRepository = ledgerRepository;
        this.authService = authService;
    }

    public List<PlatformLedgerResponseDTO> findAllPlatformLedgerEntries(String token, LocalDate fromDate, LocalDate toDate) {
        try {
            validateDateRange(fromDate, toDate);
            Integer userId = jwtService.extractUserId(token);
            String role = jwtService.extractRole(token);
            if (userId == null) {
                throw new IllegalArgumentException("User not found");
            }
            if(!role.equals("admin")) {
                throw new IllegalArgumentException("Invalid role");
            }
            return ledgerRepository.findAllPlatformLedgerEntries(fromDate, toDate);
        } catch (Exception e) {
            throw e;
        }
    }

    public List<VendorLedgerResponseDTO> findAllVendorLedgerEntries(String token, LocalDate fromDate, LocalDate toDate) {
        try {
            validateDateRange(fromDate, toDate);
            Integer userId = jwtService.extractUserId(token);
            String role = jwtService.extractRole(token);
            if (userId == null) {
                throw new IllegalArgumentException("User not found");
            }
            if(!role.equals("vendor")) {
                throw new IllegalArgumentException("Invalid role");
            }
            return ledgerRepository.findAllVendorLedgerEntries(authService.getVendorId(userId), fromDate, toDate);
        } catch (Exception e) {
            throw e;
        }
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("fromDate must be on or before toDate");
        }
    }
}
