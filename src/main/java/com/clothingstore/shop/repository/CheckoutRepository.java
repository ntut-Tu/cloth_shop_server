package com.clothingstore.shop.repository;

import com.clothingstore.shop.dto.others.checkout.*;

import com.clothingstore.shop.dto.others.tempOrder.TemporaryOrder;
import com.clothingstore.shop.dto.others.tempOrder.TemporaryProductVariant;
import com.clothingstore.shop.dto.others.tempOrder.TemporaryStoreOrder;
import com.clothingstore.shop.dto.request.checkout.SubmitOrderRequestDTO;
import com.clothingstore.shop.enums.*;
import com.clothingstore.shop.exceptions.SharedException;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import static com.clothingstore.shop.jooq.Tables.*;

@Repository
public class CheckoutRepository {
    private final DSLContext dsl;
    private final Integer shippingFee = 60;
    @Autowired
    public CheckoutRepository(DSLContext dslContext) {
        this.dsl = dslContext;
    }

    public Boolean queryProductIsAvailable(Integer productId){
        // 確定商品是否上架
        Record record = dsl.select(PRODUCT.IS_LIST)
                .from(PRODUCT)
                .where(PRODUCT.PRODUCT_ID.eq(productId))
                .fetchOne();
        return record != null && record.get(PRODUCT.IS_LIST);
    }

    public ProductVariantDTO queryProductVariantById(Integer id){
        // 透過商品變體 ID 查詢商品變體
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
    public Integer queryShippingFee(SubmitOrderRequestDTO requestDTO){
        // 查詢地區運費(目前固定)
        return shippingFee;
    }

    public Integer queryShippingFee(){
        // 查詢運費(目前固定)
        return shippingFee;
    }

    public Integer saveOrder(SubmitOrderRequestDTO requestDTO, TemporaryOrder tempOrder, Integer userId) throws SharedException {
        try {
            return dsl.transactionResult(configuration -> {
                DSLContext temp_dsl = DSL.using(configuration);

                //插入訂單
                Integer customerId = temp_dsl.select(CUSTOMER.CUSTOMER_ID)
                        .from(CUSTOMER)
                        .where(CUSTOMER.FK_USER_ID.eq(userId))
                        .fetchOneInto(Integer.class);
                Integer orderId = temp_dsl.insertInto(ORDER)
                        .set(ORDER.FK_CUSTOMER_ID, customerId)
                        .set(ORDER.PAYMENT_METHOD, requestDTO.getPayment_method())
                        .set(ORDER.PAY_STATUS, requestDTO.getPayment_method().equals(PayMethod.CARD.getMethod())
                                ? PayStatus.PAID.toString()
                                : PayStatus.PENDING.toString())
                        .set(ORDER.DELIVER_TYPE, requestDTO.getDelivery_type())
                        .set(ORDER.PICKUP_STORE, requestDTO.getPickup_store())
                        .set(ORDER.SHIP_STATUS, ShipStatus.PENDING.getStatus())
                        .set(ORDER.SHIPPING_ADDRESS, requestDTO.getShipping_address())
                        .set(ORDER.SUBTOTAL, tempOrder.getSubtotal())
                        .set(ORDER.SHIPPING_DISCOUNT_AMOUNT, tempOrder.getShippingDiscountAmount())
                        .set(ORDER.FK_SHIPPING_DISCOUNT_ID, tempOrder.getShippingDiscountId())
                        .set(ORDER.TOTAL_AMOUNT, tempOrder.getTotalAmount())
                        .returning(ORDER.ORDER_ID)
                        .fetchOptional().orElseThrow(() -> new SharedException("Failed to insert order"))
                        .getOrderId();

                //插入商店訂單和商品數據
                for (TemporaryStoreOrder storeOrderResult : tempOrder.getStoreOrders()) {
                    Integer storeOrderId = temp_dsl.insertInto(STORE_ORDER)
                            .set(STORE_ORDER.FK_ORDER_ID, orderId)
                            .set(STORE_ORDER.FK_VENDOR_ID, storeOrderResult.getStoreId())
                            .set(STORE_ORDER.STORE_DISCOUNT_AMOUNT, storeOrderResult.getDiscountAmount())
                            .set(STORE_ORDER.STORE_SUBTOTAL_AMOUNT, storeOrderResult.getSubtotal())
                            .set(STORE_ORDER.STORE_NET_AMOUNT, storeOrderResult.getTotalAmount())
                            .set(STORE_ORDER.SPECIAL_DISCOUNT_ID, storeOrderResult.getSpecialDiscountId())
                            .set(STORE_ORDER.SEASONAL_DISCOUNT_ID, storeOrderResult.getSeasonalDiscountId())
                            .set(STORE_ORDER.STORE_ORDER_STATUS, StoreOrderStatus.PENDING.getStatus())
                            .returning(STORE_ORDER.STORE_ORDER_ID)
                            .fetchOne()
                            .getStoreOrderId();

                    for (TemporaryProductVariant product : storeOrderResult.getProductVariants()) {
                        temp_dsl.insertInto(ORDER_ITEM)
                                .set(ORDER_ITEM.FK_STORE_ORDER_ID, storeOrderId)
                                .set(ORDER_ITEM.FK_PRODUCT_VARIANT_ID, product.getProductVariantId())
                                .set(ORDER_ITEM.QUANTITY, product.getQuantity())
                                .set(ORDER_ITEM.UNIT_PRICE, product.getUnitPrice())
                                .set(ORDER_ITEM.TOTAL_PRICE, product.getTotalAmount())
                                .set(ORDER_ITEM.FK_ORDER_ID, orderId)
                                .execute();
                    }
                }
                return orderId;
            });
        } catch (Exception e) {
            throw e;
        }
    }

}