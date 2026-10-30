package com.clothingstore.shop.controller;

import com.clothingstore.shop.dto.response.ApiResponseDTO;
import com.clothingstore.shop.dto.response.analytics.VendorDailySalesResponseDTO;
import com.clothingstore.shop.service.VendorAnalyticsService;
import com.clothingstore.shop.utils.TokenUtils;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/vendor/analytics")
public class VendorAnalyticsController {
    private final VendorAnalyticsService vendorAnalyticsService;

    public VendorAnalyticsController(VendorAnalyticsService vendorAnalyticsService) {
        this.vendorAnalyticsService = vendorAnalyticsService;
    }

    /**
     * Groups paid, completed store orders by their placement date in Asia/Taipei.
     * Gross sales use the store subtotal before discounts; dates without sales are omitted.
     */
    @GetMapping("/daily-sales")
    public ResponseEntity<ApiResponseDTO<List<VendorDailySalesResponseDTO>>> getDailySales(
            HttpServletRequest request,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        String token = TokenUtils.extractTokenFromCookies(request);
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponseDTO<>(false, "Token not found", null));
        }
        try {
            List<VendorDailySalesResponseDTO> sales = vendorAnalyticsService.getDailySales(token, fromDate, toDate);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Daily sales retrieved successfully", sales));
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponseDTO<>(false, "Invalid token", null));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponseDTO<>(false, e.getMessage(), null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponseDTO<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Unable to retrieve daily sales", null));
        }
    }
}
