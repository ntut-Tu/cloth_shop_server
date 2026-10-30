package com.clothingstore.shop.service;

import com.clothingstore.shop.dto.response.analytics.VendorDailySalesResponseDTO;
import com.clothingstore.shop.repository.VendorAnalyticsRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class VendorAnalyticsService {
    private final JwtService jwtService;
    private final VendorAnalyticsRepository vendorAnalyticsRepository;

    public VendorAnalyticsService(JwtService jwtService, VendorAnalyticsRepository vendorAnalyticsRepository) {
        this.jwtService = jwtService;
        this.vendorAnalyticsRepository = vendorAnalyticsRepository;
    }

    public List<VendorDailySalesResponseDTO> getDailySales(String token, LocalDate fromDate, LocalDate toDate) {
        Integer userId = jwtService.extractUserId(token);
        if (userId == null || !"vendor".equals(jwtService.extractRole(token))) {
            throw new SecurityException("Vendor access required");
        }
        Integer vendorId = vendorAnalyticsRepository.findActiveVendorId(userId);
        if (vendorId == null) {
            throw new SecurityException("Active vendor access required");
        }
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("fromDate must be on or before toDate");
        }
        return vendorAnalyticsRepository.findDailySales(vendorId, fromDate, toDate);
    }
}
