package com.clothingstore.shop.repository;

import com.clothingstore.shop.dto.repository.orders.OrderDetailRepositoryDTO;
import com.clothingstore.shop.dto.repository.orders.OrderSummaryRepositoryDTO;
import com.clothingstore.shop.dto.repository.orders.OrderSummeryDetailModel;
import com.clothingstore.shop.dto.repository.orders.StoreOrderSummaryRepositoryDTO;
import com.clothingstore.shop.enums.RoleType;
import com.clothingstore.shop.enums.ShipStatus;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.clothingstore.shop.jooq.Tables.*;
import static org.jooq.impl.DSL.and;

@Repository
public class OrderRepository {

    private final DSLContext dsl;

    @Autowired
    public OrderRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    // 1. 查詢訂單簡介
    public List<OrderSummaryRepositoryDTO> findOrderSummariesByCustomerId(Integer userId, int limit, int offset) {
        return dsl.select(
        ORDER.ORDER_ID,
        ORDER.ORDER_DATE,
        ORDER.TOTAL_AMOUNT,
        ORDER.PAY_STATUS,
        ORDER.SHIP_STATUS,
        COUPON.CODE,
        ORDER.CREDIT_CARD_LAST_FOUR,
        ORDER.PAYMENT_METHOD,
        ORDER.SHIPPING_ADDRESS,
        ORDER.DELIVER_TYPE)
    .from(ORDER)
    .join(CUSTOMER).on(ORDER.FK_CUSTOMER_ID.eq(CUSTOMER.CUSTOMER_ID))
    .join(COUPON).on(ORDER.FK_SHIPPING_DISCOUNT_ID.eq(COUPON.COUPON_ID))
    .where(CUSTOMER.FK_USER_ID.eq(userId))
    .limit(limit)
    .offset(offset)
    .fetch()
    .map(record -> {
        OrderSummaryRepositoryDTO orderSummary = record.into(OrderSummaryRepositoryDTO.class);
        OrderSummeryDetailModel detailModel = new OrderSummeryDetailModel();
        detailModel.setCreditCardLastFour(record.get(ORDER.CREDIT_CARD_LAST_FOUR));
        detailModel.setPaymentMethod(record.get(ORDER.PAYMENT_METHOD));
        detailModel.setShippingAddress(record.get(ORDER.SHIPPING_ADDRESS));
        detailModel.setShippingMethod(record.get(ORDER.DELIVER_TYPE));
        orderSummary.setOrderSummeryDetailModel(detailModel);
        return orderSummary;
    });
    }

    // 2. 查詢商家訂單簡介（延遲加載）
    public List<StoreOrderSummaryRepositoryDTO> findStoreOrdersByOrderId(Integer orderId) {
        return dsl.select(
                        STORE_ORDER.STORE_ORDER_ID,
                        USERS.ACCOUNT,
                        COUPON.CODE)
                .from(STORE_ORDER)
                .join(VENDOR).on(STORE_ORDER.FK_VENDOR_ID.eq(VENDOR.VENDOR_ID))
                .join(USERS).on(VENDOR.FK_USER_ID.eq(USERS.USER_ID))
                .join(COUPON).on(STORE_ORDER.SEASONAL_DISCOUNT_ID.eq(COUPON.COUPON_ID).or(STORE_ORDER.SPECIAL_DISCOUNT_ID.eq(COUPON.COUPON_ID)))
                .where(STORE_ORDER.FK_ORDER_ID.eq(orderId))
                .fetchInto(StoreOrderSummaryRepositoryDTO.class);
    }

    // 3. 查詢單個店家的商品詳情
//    public List<OrderItemDetailRepositoryDTO> findOrderItemsByStoreOrderId(Integer storeOrderId) {
//        return dsl.select(ORDER_ITEM.ORDER_ITEM_ID, ORDER_ITEM.UNIT_PRICE, ORDER_ITEM.QUANTITY, PRODUCT_VARIANT.COLOR, PRODUCT_VARIANT.SIZE, PRODUCT_VARIANT.PRICE)
//                .from(ORDER_ITEM)
//                .join(PRODUCT_VARIANT).on(ORDER_ITEM.FK_PRODUCT_VARIANT_ID.eq(PRODUCT_VARIANT.PRODUCT_VARIANT_ID))
//                .where(ORDER_ITEM.FK_STORE_ORDER_ID.eq(storeOrderId))
//                .fetchInto(OrderItemDetailRepositoryDTO.class);
//    }

    public boolean isOrderBelongToCustomer(Integer userId, Integer orderId) {
        return dsl.fetchExists(ORDER.join(CUSTOMER).on(ORDER.FK_CUSTOMER_ID.eq(CUSTOMER.CUSTOMER_ID))
                .where(CUSTOMER.FK_USER_ID.eq(userId).and(ORDER.ORDER_ID.eq(orderId))));
    }

    public boolean isEditOrderStatusValid(String role, Integer orderId) {
        ShipStatus shipStatus = dsl.select(ORDER.SHIP_STATUS)
                .from(ORDER)
                .where(ORDER.ORDER_ID.eq(orderId))
                .fetchOneInto(ShipStatus.class);
        if(role == RoleType.CUSTOMER.getRole()){
            if(shipStatus == ShipStatus.DELIVERED){
                return true;
            }
            return false;
        }else if(role == RoleType.VENDOR.getRole()){
            if(shipStatus == ShipStatus.PENDING){
                return true;
            }
            return false;
        }else{
            throw new IllegalArgumentException("Invalid role");
        }
    }

    public boolean isOrderBelongToVendor(Integer userId, Integer orderId) {
        return dsl.fetchExists(ORDER.join(STORE_ORDER).on(ORDER.ORDER_ID.eq(STORE_ORDER.FK_ORDER_ID))
                .where(STORE_ORDER.FK_VENDOR_ID.eq(userId).and(ORDER.ORDER_ID.eq(orderId))));
    }

    public void updateOrderStatus(Integer orderId, String status) {
        dsl.update(ORDER)
                .set(ORDER.SHIP_STATUS, status)
                .where(ORDER.ORDER_ID.eq(orderId))
                .execute();
    }

    public List<OrderDetailRepositoryDTO> findOrderDetailsByOrderId(Integer orderId) {
        return dsl.select(ORDER_ITEM.ORDER_ITEM_ID, ORDER_ITEM.QUANTITY, ORDER_ITEM.UNIT_PRICE,ORDER_ITEM.TOTAL_PRICE,PRODUCT.IMAGE_URL,PRODUCT.NAME, PRODUCT_VARIANT.SIZE, PRODUCT_VARIANT.COLOR)
                .from(ORDER_ITEM)
                .join(PRODUCT_VARIANT).on(ORDER_ITEM.FK_PRODUCT_VARIANT_ID.eq(PRODUCT_VARIANT.PRODUCT_VARIANT_ID))
                .join(PRODUCT).on(PRODUCT_VARIANT.FK_PRODUCT_ID.eq(PRODUCT.PRODUCT_ID))
                .where(ORDER_ITEM.FK_STORE_ORDER_ID.eq(orderId))
                .fetchInto(OrderDetailRepositoryDTO.class);
    }
}