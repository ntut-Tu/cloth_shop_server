package com.clothingstore.shop.repository;

import com.clothingstore.shop.dto.others.checkout.CheckoutBaseProductVariantModel;
import com.clothingstore.shop.dto.others.checkout.CheckoutBaseStoreOrderModel;
import com.clothingstore.shop.dto.others.discount.DiscountDetailsDTO;
import com.clothingstore.shop.dto.others.checkout.ProductVariantDTO;
import com.clothingstore.shop.dto.others.discount.SeasonalDiscountDTO;
import com.clothingstore.shop.dto.others.discount.ShippingDiscountDTO;
import com.clothingstore.shop.dto.others.discount.SpecialDiscountDTO;
import com.clothingstore.shop.dto.request.checkout.SubmitOrderRequestDTO;
import com.clothingstore.shop.enums.*;
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


    public Integer saveOrder(SubmitOrderRequestDTO submitOrderRequestDTO,Integer customerId) throws SharedException {
        try {
            // 驗證訂單資料
            if (submitOrderRequestDTO == null || submitOrderRequestDTO.getStore_orders() == null) {
                throw new SharedException("Invalid order data");
            }
            Integer totalAmount = 0;
            // 插入訂單資料
            Integer orderId = dsl.insertInto(ORDER)
                    .set(ORDER.FK_CUSTOMER_ID, customerId)
                    .set(ORDER.PAYMENT_METHOD, submitOrderRequestDTO.getPayment_method())
                    .set(ORDER.PAY_STATUS, submitOrderRequestDTO.getPayment_method().equals(PayMethod.CARD.getMethod()) ? PayStatus.PAID.toString() : PayStatus.PENDING.toString())
                    .set(ORDER.CREDIT_CARD_LAST_FOUR, submitOrderRequestDTO.getCredit_card_last_four())
                    .set(ORDER.DELIVER_TYPE, submitOrderRequestDTO.getDelivery_type())
                    .set(ORDER.PICKUP_STORE, submitOrderRequestDTO.getPickup_store())
                    .set(ORDER.SHIP_STATUS, ShipStatus.PENDING.getStatus())
                    .set(ORDER.SHIPPING_ADDRESS, submitOrderRequestDTO.getShipping_address())
                    .returning(ORDER.ORDER_ID)
                    .fetchOne()
                    .getOrderId();

            // TODO 商店order應該包含:商店總價,折扣金額;總order應該包含消費金額,折扣金額,總價
            for (CheckoutBaseStoreOrderModel storeOrder : submitOrderRequestDTO.getStore_orders()) {
                Integer storeOrderId = dsl.insertInto(STORE_ORDER)
                        .set(STORE_ORDER.FK_ORDER_ID, orderId)
                        .set(STORE_ORDER.FK_VENDOR_ID, storeOrder.getStore_id())
                        .set(STORE_ORDER.SPECIAL_DISCOUNT_ID, queryDiscountIdByCode(storeOrder.getSpecial_discount_code()))
                        .set(STORE_ORDER.SEASONAL_DISCOUNT_ID, queryDiscountIdByCode(storeOrder.getSeasonal_discount_code()))
                        .returning(STORE_ORDER.STORE_ORDER_ID)
                        .fetchOne()
                        .getStoreOrderId();
                for (CheckoutBaseProductVariantModel productVariant : storeOrder.getProduct_variants()) {
                    Integer unitPrice = dsl.select(PRODUCT_VARIANT.PRICE)
                            .from(PRODUCT_VARIANT)
                            .where(PRODUCT_VARIANT.PRODUCT_VARIANT_ID.eq(productVariant.getProduct_variant_id()))
                            .fetchOneInto(Integer.class);
                    totalAmount += unitPrice * productVariant.getQuantity();
                    dsl.insertInto(ORDER_ITEM)
                            .set(ORDER_ITEM.FK_STORE_ORDER_ID, storeOrderId)
                            .set(ORDER_ITEM.FK_PRODUCT_VARIANT_ID, productVariant.getProduct_variant_id())
                            .set(ORDER_ITEM.QUANTITY, productVariant.getQuantity())
                            .set(ORDER_ITEM.UNIT_PRICE, unitPrice)
                            .execute();
                }
            }
            dsl.update(ORDER)
                    .set(ORDER.TOTAL_AMOUNT, totalAmount)
                    .where(ORDER.ORDER_ID.eq(orderId))
                    .execute();
            return orderId;
        } catch (Exception e) {
            throw new SharedException("Failed to save order", e);
        }
    }
}