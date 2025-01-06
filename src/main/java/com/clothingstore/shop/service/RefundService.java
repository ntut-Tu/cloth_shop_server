package com.clothingstore.shop.service;

import com.clothingstore.shop.dto.request.refund.RefundDetailResponseDTO;
import com.clothingstore.shop.dto.response.refund.RefundListSumResponse;
import com.clothingstore.shop.repository.RefundRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RefundService {
    private final JwtService jwtService;
    private final RefundRepository refundRepository;
    private final AuthService authService;

    @Autowired
    public RefundService(JwtService jwtService, RefundRepository refundRepository, AuthService authService) {
        this.jwtService = jwtService;
        this.refundRepository = refundRepository;
        this.authService = authService;
    }

    public Integer createRefund(RefundDetailResponseDTO refundDetailResponseDTO, String token) {
        String role = jwtService.extractRole(token);
        Integer userId = jwtService.extractUserId(token);
        if (!role.equals("customer")) {
            throw new IllegalArgumentException("Only customers can create refunds");
        }
        return refundRepository.createRefund(userId, refundDetailResponseDTO);
    }

    public RefundDetailResponseDTO getRefundDetailsByRefundId(String token, Integer refundId) {
        return refundRepository.fetchRefundDetailsByRefundId(refundId);
    }

    public RefundDetailResponseDTO getRefundDetailsByOrderItem(String token, Integer orderItemId) {
        return refundRepository.fetchRefundDetailsByOrderItem(orderItemId);
    }

    public Integer updateRefund(RefundDetailResponseDTO refundDetailResponseDTO, String token, Integer refundId) {
        String role = jwtService.extractRole(token);
        Integer userId = jwtService.extractUserId(token);
        String status = refundRepository.getRefundStatus(refundId);
        if(refundRepository.isRefundNotClose(refundId)){
            throw new IllegalArgumentException("Refund is already closed");
        }
        switch (status) {
            case "vendor_pending":
                if (role.equals("vendor")) {
                    return refundRepository.updateRefund(refundDetailResponseDTO, refundId, authService.getVendorId(userId), role);
                } else {
                    throw new IllegalArgumentException("Only vendors can update pending refunds");
                }
            case "admin_pending":
                if (role.equals("admin")) {
                    return refundRepository.updateRefund(refundDetailResponseDTO, refundId, authService.getAdminId(userId), role);
                } else {
                    throw new IllegalArgumentException("Only customers can update their pending refunds");
                }
            case "vendor_rejected":
                if (role.equals("customer")) {
                    return refundRepository.updateRefund(refundDetailResponseDTO, refundId, authService.getCustomerId(userId), role);
                } else {
                    throw new IllegalArgumentException("Only customers can update their rejected refunds");
                }
            default:
                throw new IllegalArgumentException("Refund status is not pending");
        }
    }

    public Boolean checkRequestExist(String token, Integer orderItemId) {
        return refundRepository.isRequestExist(orderItemId);
    }

    public List<RefundListSumResponse> getRefundList(String token) {
        String role = jwtService.extractRole(token);
        Integer userId = jwtService.extractUserId(token);
        return refundRepository.getRefundList(role,userId);
    }
}
