package com.clothingstore.shop.repository;

import com.clothingstore.shop.dto.others.discount.DiscountDetailsDTO;
import com.clothingstore.shop.dto.others.discount.SeasonalDiscountDTO;
import com.clothingstore.shop.dto.others.discount.ShippingDiscountDTO;
import com.clothingstore.shop.dto.others.discount.SpecialDiscountDTO;
import com.clothingstore.shop.enums.CouponType;
import com.clothingstore.shop.exceptions.SharedException;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

import static com.clothingstore.shop.jooq.Tables.COUPON;
import static com.clothingstore.shop.jooq.Tables.COUPON_USAGE;

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
    public CouponType queryDiscountType(Integer discountId)throws SharedException {
        if(discountId == null){
            throw new SharedException("Failed to query");
        }
        return dsl.select(COUPON.TYPE)
                .from(COUPON)
                .where(COUPON.COUPON_ID.eq(discountId))
                .fetchOneInto(CouponType.class);
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
            Record record = dsl.select()
                    .from(COUPON)
                    .where(COUPON.COUPON_ID.eq(discountId))
                    .fetchOne();
            if (record == null) {
                return null;
            }
            if (!queryDiscountIsAvailable(discountId, customerId)) {
                return null;
            }

            DiscountDetailsDTO discountDetails;
            switch (couponType) {
                case SPECIAL_DISCOUNT:
                    discountDetails = record.into(SpecialDiscountDTO.class);
                    break;
                case SEASONAL_DISCOUNT:
                    discountDetails = record.into(SeasonalDiscountDTO.class);
                    break;
                case SHIPPING_DISCOUNT:
                    discountDetails = record.into(ShippingDiscountDTO.class);
                    break;
                default:
                    throw new SharedException("Invalid discount type");
            }

            return discountDetails;
        } catch (Exception e) {
            throw e;
        }
    }
}
