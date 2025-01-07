package com.clothingstore.shop.repository;

import com.clothingstore.shop.dto.others.discount.*;
import com.clothingstore.shop.dto.response.checkout.ConfirmDiscountResponseDTO;
import com.clothingstore.shop.enums.CouponType;
import com.clothingstore.shop.exceptions.SharedException;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

import static com.clothingstore.shop.jooq.Tables.*;

@Repository
public class DiscountRepository {
    private final DSLContext dsl;

    public DiscountRepository(DSLContext dsl) {
        this.dsl = dsl;
    }
    public Integer queryDiscountIdByCode(String discountCode)throws SharedException {
        try{
            return dsl.select(COUPON.COUPON_ID)
                    .from(COUPON)
                    .where(COUPON.CODE.eq(discountCode))
                    .fetchOneInto(Integer.class);
        }catch (Exception e){
            throw new SharedException("Invalid coupon code");
        }

    }
    public String queryDiscountType(Integer discountId)throws SharedException {
        if(discountId == null){
            throw new SharedException("Failed to query");
        }
        return dsl.select(COUPON.TYPE)
                .from(COUPON)
                .where(COUPON.COUPON_ID.eq(discountId))
                .fetchOneInto(String.class);
    }
    public Boolean queryDiscountIsAvailable(Integer discountId,Integer customerId){
        org.jooq.Record record = dsl.select(COUPON.IS_LIST,COUPON.MAXIMUM_USAGE_PER_CUSTOMER,COUPON.START_DATE,COUPON.END_DATE)
                .from(COUPON)
                .where(COUPON.COUPON_ID.eq(discountId))
                .fetchOne();
        //取得 customer 使用 coupon 的次數
        Integer userUsed = dsl.selectCount()
                .from(COUPON_USAGE)
                .where(COUPON_USAGE.FK_CUSTOMER_ID.eq(customerId).and(COUPON_USAGE.FK_COUPON_ID.eq(discountId)))
                .fetchOneInto(Integer.class);
        if(userUsed == null){
            userUsed = 0;
        }
        return record != null && record.get(COUPON.IS_LIST) && record.get(COUPON.MAXIMUM_USAGE_PER_CUSTOMER) > userUsed && record.get(COUPON.START_DATE).isBefore(java.time.OffsetDateTime.now()) && record.get(COUPON.END_DATE).isAfter(java.time.OffsetDateTime.now());
    }
    public DiscountDetailsDTO queryDiscountDetails(Integer discountId, CouponType couponType,Integer customerId)throws SharedException{
        try {
            if(discountId == null){
                return null;
            }
            DiscountDetailsDTO discountDetails;
            switch (couponType) {
                case SPECIAL_DISCOUNT:
                    discountDetails = dsl.select(
                            COUPON.CODE.as("code"),
                            SPECIAL_DISCOUNT.BUY_QUANTITY.as("buyQuantity"),
                            SPECIAL_DISCOUNT.GIFT_QUANTITY.as("giftQuantity"),
                            SPECIAL_DISCOUNT.BUY_VARIANT_ID.as("buyVariantId"),
                            SPECIAL_DISCOUNT.GIFT_VARIANT_ID.as("giftVariantId")
                            )
                            .from(COUPON)
                            .leftOuterJoin(SPECIAL_DISCOUNT).on(COUPON.COUPON_ID.eq(SPECIAL_DISCOUNT.FK_COUPON_ID))
                            .where(COUPON.COUPON_ID.eq(discountId))
                            .fetchOne()
                            .into(SpecialDiscountDTO.class);
                    break;
                case SEASONAL_DISCOUNT:
                    discountDetails = dsl.select(
                            COUPON.CODE.as("code"),
                            SEASONAL_DISCOUNT.DISCOUNT_AMOUNT.as("amount"),
                            SEASONAL_DISCOUNT.MINIMUM_SPEND.as("minimumSpend"),
                            SEASONAL_DISCOUNT.RATIO.as("ratio")
                            )
                            .from(COUPON)
                            .leftOuterJoin(SEASONAL_DISCOUNT).on(COUPON.COUPON_ID.eq(SEASONAL_DISCOUNT.FK_COUPON_ID))
                            .where(COUPON.COUPON_ID.eq(discountId))
                            .fetchOne()
                            .into(SeasonalDiscountDTO.class);
                    break;
                case SHIPPING_DISCOUNT:
                    discountDetails = dsl.select(
                            COUPON.CODE.as("code"),
                            SHIPPING_DISCOUNT.DISCOUNT_AMOUNT.as("amount"),
                            SHIPPING_DISCOUNT.MINIMUM_SPEND.as("minimumSpend"),
                            SHIPPING_DISCOUNT.RATIO.as("ratio")
                            )
                            .from(COUPON)
                            .leftOuterJoin(SHIPPING_DISCOUNT).on(COUPON.COUPON_ID.eq(SHIPPING_DISCOUNT.FK_COUPON_ID))
                            .where(COUPON.COUPON_ID.eq(discountId))
                            .fetchOne()
                            .into(ShippingDiscountDTO.class);
                    break;
                default:
                    throw new SharedException("Invalid discount type");
            }

            return discountDetails;
        } catch (Exception e) {
            throw e;
        }
    }

    public ConfirmDiscountResponseDTO fetchDiscountDetails(Integer discountId, CouponType couponType, Integer customerId) throws SharedException {
        try {
            Record record = dsl.select()
                    .from(COUPON)
                    .where(COUPON.COUPON_ID.eq(discountId))
                    .fetchOne();

            if (record == null) {
                throw new SharedException("Discount not found");
            }

            if (!queryDiscountIsAvailable(discountId, customerId)) {
                throw new SharedException("Discount not available");
            }

            DiscountDetailsDTO discountDetails;
            String couponBelongs ;
            switch (couponType) {
                case SPECIAL_DISCOUNT:
                    discountDetails = record.into(SpecialDiscountDTO.class);
                    couponBelongs = "store";
                    break;
                case SEASONAL_DISCOUNT:
                    discountDetails = record.into(SeasonalDiscountDTO.class);
                    couponBelongs = "store";
                    break;
                case SHIPPING_DISCOUNT:
                    discountDetails = record.into(ShippingDiscountDTO.class);
                    couponBelongs = "platform";
                    break;
                default:
                    throw new SharedException("Invalid discount type");
            }

            DiscountSummaryModel couponSummary = new DiscountSummaryModel();
            couponSummary.setCouponId(discountId);
            couponSummary.setDiscountDetails(discountDetails);
            couponSummary.setCode(record.get(COUPON.CODE));
            couponSummary.setStartDate(record.get(COUPON.START_DATE).toString());
            couponSummary.setEndDate(record.get(COUPON.END_DATE).toString());
            couponSummary.setMaxUsage(record.get(COUPON.MAXIMUM_USAGE_PER_CUSTOMER));
            couponSummary.setDiscountType(couponType.name());
            couponSummary.setList(record.get(COUPON.IS_LIST));

            ConfirmDiscountResponseDTO response = new ConfirmDiscountResponseDTO();
            response.setIsValid(true);
            response.setDiscountType(couponBelongs);
            response.setCoupon(couponSummary);
            return response;
        } catch (Exception e) {
            throw new SharedException("Error while querying discount details", e);
        }
    }
}
