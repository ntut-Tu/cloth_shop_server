package com.clothingstore.shop.service;

import com.clothingstore.shop.dto.response.ledger.PlatformLedgerResponseDTO;
import com.clothingstore.shop.dto.response.ledger.VendorLedgerResponseDTO;
import com.clothingstore.shop.repository.LedgerRepository;
import com.clothingstore.shop.security.Privilege;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LedgerService {
    private final JwtService jwtService;
    private final LedgerRepository ledgerRepository;
    private final AuthService authService;
    private final RbacService rbacService;

    @Autowired
    LedgerService(JwtService jwtService, LedgerRepository ledgerRepository, AuthService authService,
                  RbacService rbacService){
        this.jwtService = jwtService;
        this.ledgerRepository = ledgerRepository;
        this.authService = authService;
        this.rbacService = rbacService;
    }

    public List<PlatformLedgerResponseDTO> findAllPlatformLedgerEntries(String token) {
        try {
            rbacService.requirePermission(token, Privilege.PLATFORM_LEDGER_READ);
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
            if(!role.equals("vendor") || !authService.checkUserExists(userId, "vendor")) {
                throw new IllegalArgumentException("Invalid role");
            }
            return ledgerRepository.findAllVendorLedgerEntries(authService.getVendorId(userId));
        } catch (Exception e) {
            throw e;
        }
    }
}
