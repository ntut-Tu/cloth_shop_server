package com.clothingstore.shop.controller;

import com.clothingstore.shop.dto.response.ApiResponseDTO;
import com.clothingstore.shop.dto.response.coupon.DiscountDetailResponseDTO;
import com.clothingstore.shop.dto.response.coupon.DiscountSummaryResponseDTO;
import com.clothingstore.shop.service.CouponService;
import com.clothingstore.shop.service.JwtService;
import com.clothingstore.shop.utils.TokenUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {
    private final CouponService couponService;
    @Autowired
    public CouponController(JwtService jwtService,CouponService couponService) {
        this.couponService = couponService;
    }
    @PostMapping("/create")
    public ResponseEntity<ApiResponseDTO<Integer>> createCoupon(
            HttpServletRequest request,
            @RequestBody DiscountDetailResponseDTO discountDetailResponseDTO) {
        try {
            // 解析 token
            String token = TokenUtils.extractTokenFromCookies(request);
            if (token == null) {
                throw new IllegalArgumentException("Token not found");
            }
            // 傳遞到 Service 層
            Integer ret = couponService.createCoupon(discountDetailResponseDTO, token);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Coupon created successfully", ret));
        } catch (Exception e) {
            return ResponseEntity.ok(new ApiResponseDTO<>(false, e.getMessage(), null));
        }
    }
    @GetMapping("/list")
    public ResponseEntity<ApiResponseDTO<List<DiscountSummaryResponseDTO>>> getCoupons(
            HttpServletRequest request) {
        try {
            String token = TokenUtils.extractTokenFromCookies(request);
            if (token == null) {
                throw new IllegalArgumentException("Token not found");
            }
            List<DiscountSummaryResponseDTO> ret = couponService.getCouponList(token);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Coupon list fetched successfully", ret));
        }catch (Exception e){
            return ResponseEntity.ok(new ApiResponseDTO<>(false, e.getMessage(), null));
        }
    }

    @GetMapping("/details/{couponId}")
    public ResponseEntity<ApiResponseDTO<DiscountDetailResponseDTO>> getCouponDetails(
            HttpServletRequest request,
            @PathVariable Integer couponId) {
        try{
            String token = TokenUtils.extractTokenFromCookies(request);
            if (token == null) {
                throw new IllegalArgumentException("Token not found");
            }
            DiscountDetailResponseDTO ret = couponService.getCouponDetails(token, couponId);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Coupon details fetched successfully", ret));
        }catch (Exception e){
            return ResponseEntity.ok(new ApiResponseDTO<>(false, e.getMessage(), null));
        }
    }

    @PostMapping("/update/{couponId}")
    public ResponseEntity<ApiResponseDTO<Boolean>> updateCoupon(
            HttpServletRequest request,
            @PathVariable Integer couponId,
            @RequestBody String Status) {
        try{
            String token = TokenUtils.extractTokenFromCookies(request);
            if (token == null) {
                throw new IllegalArgumentException("Token not found");
            }
            Boolean ret = couponService.updateCoupon(token, couponId, Status);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Coupon updated successfully", ret));
        }catch (Exception e){
            return ResponseEntity.ok(new ApiResponseDTO<>(false, e.getMessage(), null));
        }
    }

}
