package com.clothingstore.shop.controller;

import com.clothingstore.shop.dto.others.discount.CouponSummaryDTO;
import com.clothingstore.shop.dto.response.ApiResponseDTO;
import com.clothingstore.shop.dto.response.Coupon.CouponDetailDTO;
import com.clothingstore.shop.exceptions.SharedException;
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
            @RequestBody CouponDetailDTO couponDetailDTO) {

        try{
            String token = TokenUtils.extractTokenFromCookies(request);
            if (token == null) {
                throw new IllegalArgumentException("Token not found");
            }
            Integer ret = couponService.createCoupon(couponDetailDTO, token);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Coupon created successfully", ret));
        }catch (IllegalArgumentException e){
            return ResponseEntity.ok(new ApiResponseDTO<>(false, e.getMessage(), null));
        }catch (Exception e){
            return ResponseEntity.ok(new ApiResponseDTO<>(false, "Failed to create coupon", null));
        }
    }
    @GetMapping("/list")
    public ResponseEntity<ApiResponseDTO<List<CouponSummaryDTO>>> getCoupons(
            HttpServletRequest request) {
        try {
            String token = TokenUtils.extractTokenFromCookies(request);
            if (token == null) {
                throw new IllegalArgumentException("Token not found");
            }
            List<CouponSummaryDTO> ret = couponService.getCouponList(token);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Coupon list fetched successfully", ret));
        }catch (Exception e){
            return ResponseEntity.ok(new ApiResponseDTO<>(false, e.getMessage(), null));
        }
    }

    @GetMapping("/details/{couponId}")
    public ResponseEntity<ApiResponseDTO<CouponDetailDTO>> getCouponDetails(
            HttpServletRequest request,
            @PathVariable Integer couponId) {
        try{
            String token = TokenUtils.extractTokenFromCookies(request);
            if (token == null) {
                throw new IllegalArgumentException("Token not found");
            }
            CouponDetailDTO ret = couponService.getCouponDetails(token, couponId);
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
