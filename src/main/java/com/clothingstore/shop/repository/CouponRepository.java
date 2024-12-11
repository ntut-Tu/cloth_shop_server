package com.clothingstore.shop.repository;

import com.clothingstore.shop.dto.repository.coupon.SpecialDiscountDTO;
import com.clothingstore.shop.dto.repository.coupon.StandardDiscountDTO;
import com.clothingstore.shop.dto.response.Coupon.DiscountDetailResponseDTO;
import com.clothingstore.shop.dto.response.Coupon.DiscountSummaryResponseDTO;
import com.clothingstore.shop.enums.CouponType;
import com.clothingstore.shop.enums.RoleType;
import com.clothingstore.shop.exceptions.SharedException;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

import static com.clothingstore.shop.jooq.Tables.*;
@Repository
public class CouponRepository {
    private final DSLContext dsl;
    @Autowired
    public CouponRepository(DSLContext dslContext) {
        this.dsl = dslContext;
    }

    public Integer createCoupon(DiscountDetailResponseDTO discountDetailResponseDTO, Integer userId, RoleType role) throws SharedException {
        return dsl.transactionResult(configuration -> {
            try {
                Integer couponId;
                if (role == RoleType.VENDOR) {
                    Integer vendorId = dsl.select(VENDOR.VENDOR_ID)
                            .from(VENDOR)
                            .where(VENDOR.FK_USER_ID.eq(userId))
                            .fetchOneInto(Integer.class);
                    couponId = dsl.insertInto(COUPON)
                            .set(COUPON.FK_VENDOR_ID, vendorId)
                            .set(COUPON.START_DATE, OffsetDateTime.parse(discountDetailResponseDTO.getStartDate()))
                            .set(COUPON.END_DATE, OffsetDateTime.parse(discountDetailResponseDTO.getEndDate()))
                            .set(COUPON.MAXIMUM_USAGE_PER_CUSTOMER, discountDetailResponseDTO.getMaximumUsagePerCustomer())
                            .set(COUPON.CODE, discountDetailResponseDTO.getCode())
                            .set(COUPON.TYPE, discountDetailResponseDTO.getType())
                            .returning(COUPON)
                            .fetchOne()
                            .get(COUPON.COUPON_ID);
                } else if (role == RoleType.ADMIN) {
                    couponId = dsl.insertInto(COUPON)
                            .set(COUPON.FK_ADMIN_ID, userId)
                            .set(COUPON.START_DATE, OffsetDateTime.parse(discountDetailResponseDTO.getStartDate()))
                            .set(COUPON.END_DATE, OffsetDateTime.parse(discountDetailResponseDTO.getEndDate()))
                            .set(COUPON.MAXIMUM_USAGE_PER_CUSTOMER, discountDetailResponseDTO.getMaximumUsagePerCustomer())
                            .set(COUPON.CODE, discountDetailResponseDTO.getCode())
                            .set(COUPON.TYPE, discountDetailResponseDTO.getType())
                            .returning(COUPON)
                            .fetchOne()
                            .get(COUPON.COUPON_ID);
                } else {
                    throw new SharedException("Invalid role");
                }
                CouponType couponType = CouponType.valueOf(discountDetailResponseDTO.getType().toUpperCase(Locale.ROOT));
                Integer discount_id;
                switch (couponType) {
                    case SEASONAL_DISCOUNT: {
                        StandardDiscountDTO specialDiscount = (StandardDiscountDTO) discountDetailResponseDTO.getDiscount();
                        discount_id = dsl.insertInto(SEASONAL_DISCOUNT)
                                .set(SEASONAL_DISCOUNT.FK_COUPON_ID, couponId)
                                .set(SEASONAL_DISCOUNT.DISCOUNT_AMOUNT, specialDiscount.getDiscount_amount())
                                .set(SEASONAL_DISCOUNT.RATIO, specialDiscount.getRatio())
                                .set(SEASONAL_DISCOUNT.DISCOUNT_TYPE, specialDiscount.getDiscount_type())
                                .set(SEASONAL_DISCOUNT.MINIMUM_SPEND, specialDiscount.getMinimum_spend())
                                .returning(SEASONAL_DISCOUNT)
                                .fetchOne()
                                .get(SEASONAL_DISCOUNT.SEASONAL_DISCOUNT_ID);
                        break;
                    }
                    case SHIPPING_DISCOUNT: {
                        StandardDiscountDTO shippingDiscount = (StandardDiscountDTO) discountDetailResponseDTO.getDiscount();
                        discount_id = dsl.insertInto(SHIPPING_DISCOUNT)
                                .set(SHIPPING_DISCOUNT.FK_COUPON_ID, couponId)
                                .set(SHIPPING_DISCOUNT.DISCOUNT_AMOUNT, shippingDiscount.getDiscount_amount())
                                .set(SHIPPING_DISCOUNT.RATIO, shippingDiscount.getRatio())
                                .set(SHIPPING_DISCOUNT.DISCOUNT_TYPE, shippingDiscount.getDiscount_type())
                                .set(SHIPPING_DISCOUNT.MINIMUM_SPEND, shippingDiscount.getMinimum_spend())
                                .returning(SHIPPING_DISCOUNT)
                                .fetchOne()
                                .get(SHIPPING_DISCOUNT.SHIPPING_DISCOUNT_ID);
                        break;
                    }
                    case SPECIAL_DISCOUNT: {
                        SpecialDiscountDTO specialDiscount = (SpecialDiscountDTO) discountDetailResponseDTO.getDiscount();
                        discount_id = dsl.insertInto(SPECIAL_DISCOUNT)
                                .set(SPECIAL_DISCOUNT.FK_COUPON_ID, couponId)
                                .set(SPECIAL_DISCOUNT.BUY_QUANTITY, specialDiscount.getBuyQuantity())
                                .set(SPECIAL_DISCOUNT.GIFT_QUANTITY, specialDiscount.getGiftQuantity())
                                .set(SPECIAL_DISCOUNT.BUY_VARIANT_ID, specialDiscount.getBuyVariantId())
                                .set(SPECIAL_DISCOUNT.GIFT_VARIANT_ID, specialDiscount.getGiftVariantId())
                                .returning(SPECIAL_DISCOUNT)
                                .fetchOne()
                                .get(SPECIAL_DISCOUNT.SPECIAL_DISCOUNT_ID);
                        break;
                    }
                    default: {
                        throw new SharedException("Invalid coupon type");
                    }
                }
                return couponId;
            } catch (Exception e) {
                throw e;
            }
        });
    }

    public List<DiscountSummaryResponseDTO> fetchCoupons(Integer userId, RoleType role) {
        try {
            Integer fetchId = role == RoleType.VENDOR ? dsl.select(VENDOR.VENDOR_ID)
                    .from(VENDOR)
                    .where(VENDOR.FK_USER_ID.eq(userId))
                    .fetchOneInto(Integer.class) : userId;
            return dsl.select(COUPON.COUPON_ID, COUPON.CODE, COUPON.TYPE, COUPON.START_DATE, COUPON.END_DATE, COUPON.IS_LIST, COUPON.MAXIMUM_USAGE_PER_CUSTOMER)
                    .from(COUPON)
                    .where(role == RoleType.VENDOR ? COUPON.FK_VENDOR_ID.eq(fetchId) : COUPON.FK_ADMIN_ID.eq(fetchId))
                    .fetchInto(DiscountSummaryResponseDTO.class);
        }catch (Exception e){
            throw e;
        }

    }

    public DiscountDetailResponseDTO fetchCouponDetails(Integer couponId) {
        try {
            DiscountDetailResponseDTO coupon= dsl.fetchOne(COUPON, COUPON.COUPON_ID.eq(couponId)).into(DiscountDetailResponseDTO.class);
            coupon.setDiscount(dsl.fetchOne(SPECIAL_DISCOUNT, SPECIAL_DISCOUNT.FK_COUPON_ID.eq(couponId)).into(SpecialDiscountDTO.class));
            return coupon;
        }catch (Exception e){
            throw e;
        }
    }

    public Boolean updateCouponStatus(Integer couponId, Integer userId, RoleType role, String status)throws SharedException {
        try{
            Integer fetchId = role == RoleType.VENDOR ? dsl.select(VENDOR.VENDOR_ID)
                    .from(VENDOR)
                    .where(VENDOR.FK_USER_ID.eq(userId))
                    .fetchOneInto(Integer.class) : userId;
            if(role == RoleType.VENDOR) {
                return dsl.update(COUPON)
                        .set(COUPON.IS_LIST, status.equals("active"))
                        .where(COUPON.COUPON_ID.eq(couponId).and(COUPON.FK_VENDOR_ID.eq(fetchId)))
                        .execute() == 1;
            } else if (role == RoleType.ADMIN) {
                return dsl.update(COUPON)
                        .set(COUPON.IS_LIST, status.equals("active"))
                        .where(COUPON.COUPON_ID.eq(couponId).and(COUPON.FK_ADMIN_ID.eq(fetchId)))
                        .execute() == 1;
            } else {
                throw new SharedException("Invalid role");
            }
        }catch (Exception e){
            throw e;
        }
    }
}
