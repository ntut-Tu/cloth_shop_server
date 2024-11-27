package com.clothingstore.shop.repository;

import com.clothingstore.shop.dto.others.checkout.DiscountDetailsDTO;
import com.clothingstore.shop.dto.others.checkout.ProductVariantDTO;
import com.clothingstore.shop.dto.request.checkout.ConfirmAmountRequestDTO;
import com.clothingstore.shop.dto.request.checkout.ConfirmDiscountRequestDTO;
import com.clothingstore.shop.dto.request.checkout.SubmitOrderRequestDTO;
import com.clothingstore.shop.dto.response.checkout.ConfirmAmountResponseDTO;
import com.clothingstore.shop.dto.response.checkout.SubmitOrderResponseDTO;
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
//    public Boolean queryDiscountIsAvailable(Integer discountId){
//        Record record = dsl.select(COUPON.IS_LIST,COUPON.MAXIMUM_USAGE_PER_CUSTOMER,COUPON.START_DATE,COUPON.END_DATE,COUPON.)
//                .from(COUPON)
//                .where(COUPON.COUPON_ID.eq(discountId))
//                .fetchOne();
//        return record != null && record.get(COUPON.IS_AVAILABLE);
//    }
//    public DiscountDetailsDTO queryDiscountDetails(Integer discountId){
//        // Fetch discount details from database
//        String discountType = dsl.select(COUPON.)
//                .from(COUPON)
//                .where(COUPON.COUPON_ID.eq(discountId))
//                .fetchOneInto(String.class);
//        return null;
//    }


}