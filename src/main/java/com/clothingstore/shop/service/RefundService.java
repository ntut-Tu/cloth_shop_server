package com.clothingstore.shop.service;

import com.clothingstore.shop.dto.request.refund.RefundDetailResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RefundService {
    private final JwtService jwtService;
    @Autowired
    public RefundService(JwtService jwtService) {
        this.jwtService = jwtService;
    }
    public Integer createRefund(RefundDetailResponseDTO refundDetailResponseDTO, String token) {
        return null;
    }

    public RefundDetailResponseDTO getRefundDetails(String token, Integer refundId) {
        return null;
    }
}
