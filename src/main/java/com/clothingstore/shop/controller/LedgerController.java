package com.clothingstore.shop.controller;

import com.clothingstore.shop.dto.response.ApiResponseDTO;
import com.clothingstore.shop.dto.response.ledger.PlatformLedgerResponseDTO;
import com.clothingstore.shop.dto.response.ledger.VendorLedgerResponseDTO;
import com.clothingstore.shop.service.LedgerService;
import com.clothingstore.shop.utils.TokenUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ledger")
public class LedgerController {
    private final LedgerService ledgerService;
    @Autowired
    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    // 取得平台帳本
    // 只有管理員可以取得
    @GetMapping("/platform")
    public ResponseEntity<ApiResponseDTO<List<PlatformLedgerResponseDTO>>> getPlatformLedger(
            HttpServletRequest request
    ) {
        try {
            // 解析 token
            String token = TokenUtils.extractTokenFromCookies(request);
            if (token == null) {
                throw new IllegalArgumentException("Token not found");
            }
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Platform ledger fetched successfully", ledgerService.findAllPlatformLedgerEntries(token)));
        } catch (Exception e) {
            return ResponseEntity.ok(new ApiResponseDTO<>(false, e.getMessage(), null));
        }
    }

    // 取得商家帳本
    // 只有商家可以取得
    @GetMapping("/vendor")
    public ResponseEntity<ApiResponseDTO<List<VendorLedgerResponseDTO>>> getVendorLedger(
            HttpServletRequest request
    ) {
        try {
            // 解析 token
            String token = TokenUtils.extractTokenFromCookies(request);
            if (token == null) {
                throw new IllegalArgumentException("Token not found");
            }
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Vendor ledger fetched successfully", ledgerService.findAllVendorLedgerEntries(token)));
        } catch (Exception e) {
            return ResponseEntity.ok(new ApiResponseDTO<>(false, e.getMessage(), null));
        }
    }
}
