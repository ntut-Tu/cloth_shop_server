package com.clothingstore.shop.controller;

import com.clothingstore.shop.dto.request.refund.RefundDetailResponseDTO;
import com.clothingstore.shop.dto.response.ApiResponseDTO;
import com.clothingstore.shop.service.RefundService;
import com.clothingstore.shop.utils.TokenUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/refunds")
public class RefundController {
    private final RefundService refundService;

    @Autowired
    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponseDTO<Integer>> createRefund(
            HttpServletRequest request,
            @RequestBody RefundDetailResponseDTO refundDetailResponseDTO) {
        try {
            // 解析 token
            String token = TokenUtils.extractTokenFromCookies(request);
            if (token == null) {
                throw new IllegalArgumentException("Token not found");
            }
            // 傳遞到 Service 層
            Integer ret = refundService.createRefund(refundDetailResponseDTO, token);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Refund created successfully", ret));
        } catch (Exception e) {
            return ResponseEntity.ok(new ApiResponseDTO<>(false, e.getMessage(), null));
        }
    }

    @GetMapping("/request/{refundId}")
    public ResponseEntity<ApiResponseDTO<RefundDetailResponseDTO>> getRefundDetails(
            HttpServletRequest request,
            @PathVariable Integer refundId) {
        try {
            String token = TokenUtils.extractTokenFromCookies(request);
            if (token == null) {
                throw new IllegalArgumentException("Token not found");
            }
            RefundDetailResponseDTO ret = refundService.getRefundDetails(token, refundId);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Refund details fetched successfully", ret));
        } catch (Exception e) {
            return ResponseEntity.ok(new ApiResponseDTO<>(false, e.getMessage(), null));
        }
    }
}
