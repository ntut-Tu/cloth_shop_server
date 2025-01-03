package com.clothingstore.shop.repository;

import com.clothingstore.shop.dto.repository.coupon.BaseDiscountModel;
import com.clothingstore.shop.dto.repository.coupon.SpecialDiscountModel;
import com.clothingstore.shop.dto.repository.coupon.StandardDiscountModel;
import com.clothingstore.shop.dto.response.coupon.DiscountDetailResponseDTO;
import com.clothingstore.shop.dto.response.coupon.DiscountSummaryResponseDTO;
import com.clothingstore.shop.enums.RoleType;
import com.clothingstore.shop.exceptions.SharedException;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

import static com.clothingstore.shop.jooq.Tables.*;
@Repository
public class CouponRepository {
    private final DSLContext dsl;
    @Autowired
    public CouponRepository(DSLContext dslContext) {
        this.dsl = dslContext;
    }

    public Integer createCoupon(DiscountDetailResponseDTO discountDetailResponseDTO, Integer userId, RoleType role, BaseDiscountModel discountModel) throws SharedException {
        return dsl.transactionResult(configuration -> {
            try {
                DSLContext ctx = DSL.using(configuration);
                Integer couponId;
                if (role == RoleType.VENDOR) {
                    couponId = ctx.insertInto(COUPON)
                            .set(COUPON.FK_VENDOR_ID, userId)
                            .set(COUPON.START_DATE, OffsetDateTime.parse(discountDetailResponseDTO.getStartDate()))
                            .set(COUPON.END_DATE, OffsetDateTime.parse(discountDetailResponseDTO.getEndDate()))
                            .set(COUPON.MAXIMUM_USAGE_PER_CUSTOMER, discountDetailResponseDTO.getMaximumUsagePerCustomer())
                            .set(COUPON.CODE, discountDetailResponseDTO.getCode())
                            .set(COUPON.TYPE, discountDetailResponseDTO.getType())
                            .set(COUPON.IS_LIST, true)
                            .returning(COUPON.COUPON_ID)
                            .fetchOne()
                            .get(COUPON.COUPON_ID);
                } else if (role == RoleType.ADMIN) {
                    couponId = ctx.insertInto(COUPON)
                            .set(COUPON.FK_ADMIN_ID, userId)
                            .set(COUPON.START_DATE, OffsetDateTime.parse(discountDetailResponseDTO.getStartDate()))
                            .set(COUPON.END_DATE, OffsetDateTime.parse(discountDetailResponseDTO.getEndDate()))
                            .set(COUPON.MAXIMUM_USAGE_PER_CUSTOMER, discountDetailResponseDTO.getMaximumUsagePerCustomer())
                            .set(COUPON.CODE, discountDetailResponseDTO.getCode())
                            .set(COUPON.TYPE, discountDetailResponseDTO.getType())
                            .set(COUPON.IS_LIST, true)
                            .returning(COUPON.COUPON_ID)
                            .fetchOne()
                            .get(COUPON.COUPON_ID);
                } else {
                    throw new SharedException("Invalid role");
                }

                Integer discountId;
                if (discountDetailResponseDTO.getType().equalsIgnoreCase("seasonal_discount") && discountModel instanceof StandardDiscountModel standardDiscount) {
                    ctx.insertInto(SEASONAL_DISCOUNT)
                            .set(SEASONAL_DISCOUNT.FK_COUPON_ID, couponId)
                            .set(SEASONAL_DISCOUNT.DISCOUNT_AMOUNT, standardDiscount.getDiscount_amount())
                            .set(SEASONAL_DISCOUNT.RATIO, standardDiscount.getRatio())
                            .set(SEASONAL_DISCOUNT.DISCOUNT_TYPE, standardDiscount.getDiscount_type())
                            .set(SEASONAL_DISCOUNT.MINIMUM_SPEND, standardDiscount.getMinimum_spend())
                            .execute();

                } else if (discountModel instanceof SpecialDiscountModel specialDiscount) {
                    ctx.insertInto(SPECIAL_DISCOUNT)
                            .set(SPECIAL_DISCOUNT.FK_COUPON_ID, couponId)
                            .set(SPECIAL_DISCOUNT.BUY_QUANTITY, specialDiscount.getBuyQuantity())
                            .set(SPECIAL_DISCOUNT.GIFT_QUANTITY, specialDiscount.getGiftQuantity())
                            .set(SPECIAL_DISCOUNT.BUY_VARIANT_ID, specialDiscount.getBuyVariantId())
                            .set(SPECIAL_DISCOUNT.GIFT_VARIANT_ID, specialDiscount.getGiftVariantId())
                            .execute();
                } else if(discountModel instanceof StandardDiscountModel standardDiscount) {
                    ctx.insertInto(SHIPPING_DISCOUNT)
                            .set(SHIPPING_DISCOUNT.FK_COUPON_ID, couponId)
                            .set(SHIPPING_DISCOUNT.DISCOUNT_AMOUNT, standardDiscount.getDiscount_amount())
                            .set(SHIPPING_DISCOUNT.RATIO, standardDiscount.getRatio())
                            .set(SHIPPING_DISCOUNT.DISCOUNT_TYPE, standardDiscount.getDiscount_type())
                            .set(SHIPPING_DISCOUNT.MINIMUM_SPEND, standardDiscount.getMinimum_spend())
                            .execute();
                }else {
                    throw new SharedException("Unsupported discount type");
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
            return dsl.select(COUPON.COUPON_ID.as("discountId"), COUPON.CODE, COUPON.TYPE.as("discountType"), COUPON.START_DATE, COUPON.END_DATE, COUPON.IS_LIST, COUPON.MAXIMUM_USAGE_PER_CUSTOMER)
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
            coupon.setDiscount(dsl.fetchOne(SPECIAL_DISCOUNT, SPECIAL_DISCOUNT.FK_COUPON_ID.eq(couponId)).into(SpecialDiscountModel.class));
            return coupon;
        }catch (Exception e){
            throw e;
        }
    }

    public Boolean updateCouponStatus(Integer couponId, Integer userId, RoleType role, Boolean isActive)throws SharedException {
        try{
            Integer fetchId = role == RoleType.VENDOR ? dsl.select(VENDOR.VENDOR_ID)
                    .from(VENDOR)
                    .where(VENDOR.FK_USER_ID.eq(userId))
                    .fetchOneInto(Integer.class) : userId;
            if(role == RoleType.VENDOR) {
                return dsl.update(COUPON)
                        .set(COUPON.IS_LIST, isActive)
                        .where(COUPON.COUPON_ID.eq(couponId).and(COUPON.FK_VENDOR_ID.eq(fetchId)))
                        .execute() == 1;
            } else if (role == RoleType.ADMIN) {
                return dsl.update(COUPON)
                        .set(COUPON.IS_LIST, isActive)
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
