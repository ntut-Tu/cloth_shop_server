package com.clothingstore.shop.repository;

import com.clothingstore.shop.dto.others.discount.DiscountDetailsDTO;
import com.clothingstore.shop.dto.others.checkout.ProductVariantDTO;
import com.clothingstore.shop.dto.others.discount.SeasonalDiscountDTO;
import com.clothingstore.shop.dto.others.discount.ShippingDiscountDTO;
import com.clothingstore.shop.dto.others.discount.SpecialDiscountDTO;
import com.clothingstore.shop.enums.CouponType;
import com.clothingstore.shop.enums.ExceptionCode;
import com.clothingstore.shop.exceptions.SharedException;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import static com.clothingstore.shop.jooq.Tables.*;

@Repository
public class CheckoutRepository {
    private final DSLContext dsl;

    @Autowired
    public CheckoutRepository(DSLContext dslContext) {
        this.dsl = dslContext;
    }
    public Boolean queryProductIsAvailable(Integer productId){
        Record record = dsl.select(PRODUCT.IS_LIST)
                .from(PRODUCT)
                .where(PRODUCT.PRODUCT_ID.eq(productId))
                .fetchOne();
        return record != null && record.get(PRODUCT.IS_LIST);
    }
    public ProductVariantDTO queryProductVariantById(Integer id){
        Integer productId = dsl.select(PRODUCT_VARIANT.FK_PRODUCT_ID)
                .from(PRODUCT_VARIANT)
                .where(PRODUCT_VARIANT.PRODUCT_VARIANT_ID.eq(id))
                .fetchOneInto(Integer.class);
        if (productId == null || queryProductIsAvailable(productId)){
            return null;
        }
        ProductVariantDTO productVariant= dsl.select(PRODUCT_VARIANT.PRODUCT_VARIANT_ID, PRODUCT_VARIANT.FK_PRODUCT_ID, PRODUCT_VARIANT.PRICE,PRODUCT_VARIANT.STOCK)
                .from(PRODUCT_VARIANT)
                .where(PRODUCT_VARIANT.PRODUCT_VARIANT_ID.eq(id))
                .fetchOneInto(ProductVariantDTO.class);
        productVariant.setIsAvailable(true);
        return productVariant;
    }
    public Integer queryDiscountIdByCode(String discountCode)throws SharedException{
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
        Record record = dsl.select(COUPON.IS_LIST,COUPON.MAXIMUM_USAGE_PER_CUSTOMER,COUPON.START_DATE,COUPON.END_DATE)
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
    public DiscountDetailsDTO queryDiscountDetails(Integer discountId, CouponType couponType)throws SharedException{
        try {
            Record record = dsl.select()
                    .from(COUPON)
                    .where(COUPON.COUPON_ID.eq(discountId))
                    .fetchOne();

            if (record == null) {
                throw new SharedException("Failed to query");
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