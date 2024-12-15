package com.clothingstore.shop.service;

import com.clothingstore.shop.dto.response.ledger.PlatformLedgerResponseDTO;
import com.clothingstore.shop.dto.response.ledger.VendorLedgerResponseDTO;
import com.clothingstore.shop.repository.LedgerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LedgerService {
    private final JwtService jwtService;
    private final LedgerRepository ledgerRepository;

    @Autowired
    LedgerService(JwtService jwtService, LedgerRepository ledgerRepository){
        this.jwtService = jwtService;
        this.ledgerRepository = ledgerRepository;
    }

    public List<PlatformLedgerResponseDTO> findAllPlatformLedgerEntries(String token) {
        try {
            Integer userId = jwtService.extractUserId(token);
            String role = jwtService.extractRole(token);
            if (userId == null) {
                throw new IllegalArgumentException("User not found");
            }
            if(!role.equals("admin")) {
                throw new IllegalArgumentException("Invalid role");
            }
            return ledgerRepository.findAllPlatformLedgerEntries();
        } catch (Exception e) {
            throw e;
        }
    }

    public List<VendorLedgerResponseDTO> findAllVendorLedgerEntries(String token) {
        try {
            Integer userId = jwtService.extractUserId(token);
            String role = jwtService.extractRole(token);
            if (userId == null) {
                throw new IllegalArgumentException("User not found");
            }
            if(!role.equals("vendor")) {
                throw new IllegalArgumentException("Invalid role");
            }
            return ledgerRepository.findAllVendorLedgerEntries();
        } catch (Exception e) {
            throw e;
        }
    }
}

