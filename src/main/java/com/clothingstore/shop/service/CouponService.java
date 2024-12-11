package com.clothingstore.shop.service;

import com.clothingstore.shop.dto.others.discount.CouponSummaryDTO;
import com.clothingstore.shop.dto.response.Coupon.CouponDetailDTO;
import com.clothingstore.shop.enums.RoleType;
import com.clothingstore.shop.exceptions.SharedException;
import com.clothingstore.shop.repository.CouponRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CouponService {
    private final JwtService jwtService;
    private final CouponRepository couponRepository;
    @Autowired
    CouponService(JwtService jwtService, CouponRepository couponRepository) {
        this.jwtService = jwtService;
        this.couponRepository = couponRepository;
    }
    public Integer createCoupon(CouponDetailDTO couponDetailDTO, String token)throws SharedException {
        try{
            Integer userId = jwtService.extractUserId(token);
            RoleType role = RoleType.valueOf(jwtService.extractRole(token));
            return couponRepository.createCoupon(couponDetailDTO, userId,role);
        }catch (Exception e){
            throw e;
        }
    }
    public List<CouponSummaryDTO> getCouponList(String token) throws SharedException {
        try{
            Integer userId = jwtService.extractUserId(token);
            RoleType role = RoleType.valueOf(jwtService.extractRole(token));
            return couponRepository.fetchCoupons(userId,role);
        }catch (Exception e){
            throw e;
        }
    }
    public CouponDetailDTO getCouponDetails(String token, Integer couponId) throws SharedException {
        try{
            Integer userId = jwtService.extractUserId(token);
            return couponRepository.fetchCouponDetails(couponId);
        }catch (Exception e){
            throw e;
        }

    }
    public Boolean updateCoupon(String token,Integer couponId, String status) throws SharedException {
        try {
            Integer userId = jwtService.extractUserId(token);
            RoleType role = RoleType.valueOf(jwtService.extractRole(token));
            return couponRepository.updateCouponStatus(couponId, userId, role, status);
        }catch (Exception e){
            throw e;
        }
    }
}
