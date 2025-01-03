package com.clothingstore.shop.service;

import com.clothingstore.shop.dto.repository.coupon.BaseDiscountModel;
import com.clothingstore.shop.dto.repository.coupon.SpecialDiscountModel;
import com.clothingstore.shop.dto.repository.coupon.StandardDiscountModel;
import com.clothingstore.shop.dto.response.coupon.DiscountDetailResponseDTO;
import com.clothingstore.shop.dto.response.coupon.DiscountSummaryResponseDTO;
import com.clothingstore.shop.enums.RoleType;
import com.clothingstore.shop.exceptions.SharedException;
import com.clothingstore.shop.repository.CouponRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class CouponService {

    private final JwtService jwtService;
    private final CouponRepository couponRepository;
    private final AuthService authService;

    @Autowired
    CouponService(JwtService jwtService, CouponRepository couponRepository, AuthService authService) {
        this.jwtService = jwtService;
        this.couponRepository = couponRepository;
        this.authService = authService;
    }

    public Integer createCoupon(DiscountDetailResponseDTO discountDetailResponseDTO, String token) throws SharedException {
        try {
            Integer userId = jwtService.extractUserId(token);
            RoleType role = RoleType.valueOf(jwtService.extractRole(token).toUpperCase(Locale.ROOT));
            userId = (role == RoleType.VENDOR)
                    ? authService.getVendorId(userId)
                    : authService.getAdminId(userId);

            // 解析 discount 字段
            BaseDiscountModel discountModel = parseDiscount(discountDetailResponseDTO);

            // 傳遞解析後的資料到 Repository
            return couponRepository.createCoupon(discountDetailResponseDTO, userId, role, discountModel);
        } catch (Exception e) {
            throw e;
        }
    }
    public List<DiscountSummaryResponseDTO> getCouponList(String token) throws SharedException {
        try{
            Integer userId = jwtService.extractUserId(token);
            RoleType role = RoleType.valueOf(jwtService.extractRole(token).toUpperCase(Locale.ROOT));
            return couponRepository.fetchCoupons(userId,role);
        }catch (Exception e){
            throw e;
        }
    }
    public DiscountDetailResponseDTO getCouponDetails(String token, Integer couponId) throws SharedException {
        try{
            Integer userId = jwtService.extractUserId(token);
            return couponRepository.fetchCouponDetails(couponId);
        }catch (Exception e){
            throw e;
        }

    }
    public Boolean updateCoupon(String token,Integer couponId, Boolean isActive) throws SharedException {
        try {
            Integer userId = jwtService.extractUserId(token);
            RoleType role = RoleType.valueOf(jwtService.extractRole(token).toUpperCase(Locale.ROOT));
            return couponRepository.updateCouponStatus(couponId, userId, role, isActive);
        }catch (Exception e){
            throw e;
        }
    }

    private BaseDiscountModel parseDiscount(DiscountDetailResponseDTO dto) {
        String type = dto.getType(); // "standard" or "special"
        Object discount = dto.getDiscount();

        if ("seasonal_discount".equalsIgnoreCase(type)||"shipping_discount".equalsIgnoreCase(type)) {
            // 手動解析為 StandardDiscountModel
            if (discount instanceof Map) {
                Map<String, Object> discountMap = (Map<String, Object>) discount;
                StandardDiscountModel standardDiscount = new StandardDiscountModel();
                standardDiscount.setDiscount_type((String) discountMap.get("discount_type"));
                standardDiscount.setRatio(discountMap.get("ratio") != null ? new BigDecimal((Integer) discountMap.get("ratio")) : null);
                standardDiscount.setDiscount_amount((Integer) discountMap.get("discount_amount"));
                standardDiscount.setMinimum_spend((Integer) discountMap.get("minimumSpend"));
                return standardDiscount;
            } else if (discount instanceof StandardDiscountModel) {
                return (StandardDiscountModel) discount;
            } else {
                throw new IllegalArgumentException("Invalid discount format for standard discount");
            }
        } else if ("special_discount".equalsIgnoreCase(type)) {
            // 手動解析為 SpecialDiscountModel
            if (discount instanceof Map) {
                Map<String, Object> discountMap = (Map<String, Object>) discount;
                SpecialDiscountModel specialDiscount = new SpecialDiscountModel();
                specialDiscount.setBuyQuantity((Integer) discountMap.get("buyQuantity"));
                specialDiscount.setGiftQuantity((Integer) discountMap.get("giftQuantity"));
                specialDiscount.setBuyVariantId((Integer) discountMap.get("buyVariantId"));
                specialDiscount.setGiftVariantId((Integer) discountMap.get("giftVariantId"));
                return specialDiscount;
            } else if (discount instanceof SpecialDiscountModel) {
                return (SpecialDiscountModel) discount;
            } else {
                throw new IllegalArgumentException("Invalid discount format for special discount");
            }
        } else {
            throw new IllegalArgumentException("Unsupported discount type: " + type);
        }
    }
}
