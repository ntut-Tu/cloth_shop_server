package com.clothingstore.shop.repository;

import com.clothingstore.shop.dto.repository.orders.OrderItemDetailRepositoryDTO;
import com.clothingstore.shop.dto.repository.orders.OrderSummaryRepositoryDTO;
import com.clothingstore.shop.dto.repository.orders.StoreOrderSummaryRepositoryDTO;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.clothingstore.shop.jooq.Tables.*;

@Repository
public class OrderRepository {

    private final DSLContext dsl;

    @Autowired
    public OrderRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    // 1. 查詢訂單簡介
    public List<OrderSummaryRepositoryDTO> findOrderSummariesByCustomerId(Integer customerId, int limit, int offset) {
        return dsl.select(ORDER.ORDER_ID, ORDER.ORDER_DATE, ORDER.TOTAL_AMOUNT, ORDER.PAY_STATUS, ORDER.SHIP_STATUS)
                .from(ORDER)
                .where(ORDER.FK_CUSTOMER_ID.eq(customerId))
                .limit(limit)
                .offset(offset)
                .fetchInto(OrderSummaryRepositoryDTO.class);
    }

    // 2. 查詢商家訂單簡介（延遲加載）
    public List<StoreOrderSummaryRepositoryDTO> findStoreOrdersByOrderId(Integer orderId) {
        return dsl.select(STORE_ORDER.STORE_ORDER_ID, STORE_ORDER.FK_VENDOR_ID, STORE_ORDER.SEASONAL_DISCOUNT_ID, STORE_ORDER.SPECIAL_DISCOUNT_ID, STORE_ORDER.SHIPPING_DISCOUNT_ID)
                .from(STORE_ORDER)
                .where(STORE_ORDER.FK_ORDER_ID.eq(orderId))
                .fetchInto(StoreOrderSummaryRepositoryDTO.class);
    }

    // 3. 查詢單個店家的商品詳情
    public List<OrderItemDetailRepositoryDTO> findOrderItemsByStoreOrderId(Integer storeOrderId) {
        return dsl.select(ORDER_ITEM.ORDER_ITEM_ID, ORDER_ITEM.UNIT_PRICE, ORDER_ITEM.QUANTITY, PRODUCT_VARIANT.COLOR, PRODUCT_VARIANT.SIZE, PRODUCT_VARIANT.PRICE)
                .from(ORDER_ITEM)
                .join(PRODUCT_VARIANT).on(ORDER_ITEM.FK_PRODUCT_VARIANT_ID.eq(PRODUCT_VARIANT.PRODUCT_VARIANT_ID))
                .where(ORDER_ITEM.FK_STORE_ORDER_ID.eq(storeOrderId))
                .fetchInto(OrderItemDetailRepositoryDTO.class);
    }
}