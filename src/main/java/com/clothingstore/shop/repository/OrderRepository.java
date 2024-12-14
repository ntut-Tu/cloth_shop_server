package com.clothingstore.shop.repository;

import com.clothingstore.shop.dto.repository.orders.OrderDetailRepositoryDTO;
import com.clothingstore.shop.dto.repository.orders.OrderSummaryRepositoryDTO;
import com.clothingstore.shop.dto.repository.orders.OrderSummeryDetailModel;
import com.clothingstore.shop.dto.repository.orders.StoreOrderSummaryRepositoryDTO;
import com.clothingstore.shop.dto.response.vendorOrder.VendorOrderResponseDTO;
import com.clothingstore.shop.dto.response.vendorOrder.vendorUserOrder.VendorProductVariantDTO;
import com.clothingstore.shop.dto.response.vendorOrder.vendorUserOrder.VendorUserOrderDTO;
import com.clothingstore.shop.enums.RoleType;
import com.clothingstore.shop.enums.ShipStatus;
import com.clothingstore.shop.enums.StoreOrderStatus;
import com.clothingstore.shop.exceptions.SharedException;
import org.apache.commons.lang3.tuple.Pair;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

import static com.clothingstore.shop.jooq.Tables.*;
import static org.jooq.impl.DSL.and;
import static org.jooq.impl.DSL.field;

@Repository
public class OrderRepository {

    private final DSLContext dsl;
    @Autowired
    public OrderRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    // 1. 查詢訂單簡介
    public List<OrderSummaryRepositoryDTO> findOrderSummariesByCustomerId(Integer userId, int limit, int offset) throws SharedException {
        try {
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
                    .leftOuterJoin(COUPON).on(ORDER.FK_SHIPPING_DISCOUNT_ID.eq(COUPON.COUPON_ID))
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
                        return orderSummary;});
        }catch (Exception e){
            throw e;
        }

    }

    // 2. 查詢商家訂單簡介（延遲加載）
    public List<StoreOrderSummaryRepositoryDTO> findStoreOrdersByOrderId(Integer storeOrderId) {
        return dsl.select(
                        STORE_ORDER.STORE_ORDER_ID,
                        USERS.ACCOUNT,
                        USERS.PROFILE_PIC_URL,
                        COUPON.CODE)
                .from(STORE_ORDER)
                .join(VENDOR).on(STORE_ORDER.FK_VENDOR_ID.eq(VENDOR.VENDOR_ID))
                .join(USERS).on(VENDOR.FK_USER_ID.eq(USERS.USER_ID))
                .leftOuterJoin(COUPON).on(STORE_ORDER.SEASONAL_DISCOUNT_ID.eq(COUPON.COUPON_ID).or(STORE_ORDER.SPECIAL_DISCOUNT_ID.eq(COUPON.COUPON_ID)))
                .where(STORE_ORDER.STORE_ORDER_ID.eq(storeOrderId))
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

    public boolean isStoreOrderBelongToVendor(Integer userId, Integer storeOrderId) {
        return dsl.fetchExists(STORE_ORDER.join(VENDOR).on(STORE_ORDER.FK_VENDOR_ID.eq(VENDOR.VENDOR_ID))
                .where(VENDOR.FK_USER_ID.eq(userId).and(STORE_ORDER.STORE_ORDER_ID.eq(storeOrderId))));
    }

    public void updateStoreOrderStatus(Integer storeOrderId, String status) throws SharedException {
        String oldStatus = dsl.select(STORE_ORDER.STORE_ORDER_STATUS)
                .from(STORE_ORDER)
                .where(STORE_ORDER.STORE_ORDER_ID.eq(storeOrderId))
                .fetchOneInto(String.class);
        if (oldStatus!=null && !oldStatus.equalsIgnoreCase(StoreOrderStatus.PENDING.toString())){
            throw new SharedException("Store order status cannot be updated");
        }
        dsl.update(STORE_ORDER)
                .set(STORE_ORDER.STORE_ORDER_STATUS, status)
                .where(STORE_ORDER.STORE_ORDER_ID.eq(storeOrderId))
                .execute();
    }

    public List<VendorOrderResponseDTO> findStoreOrderSummariesByVendorId(Integer userId, int size, int offset) {
        // 獲取供應商ID
        Integer vendorId = dsl.select(VENDOR.VENDOR_ID)
                .from(VENDOR)
                .where(VENDOR.FK_USER_ID.eq(userId))
                .fetchOneInto(Integer.class);

        if (vendorId == null) {
            throw new IllegalArgumentException("Vendor not found");
        }
        System.out.println("Current SQLDialect: " + dsl.configuration().dialect());
        List<VendorOrderResponseDTO> storeOrders = dsl.select(
                        field("store_order_id"),
                        field("total_amount"),
                        field("store_order_status"),
                        field("order_date")
                )
                .from("vendor_order_response_view")
                .where(field("vendor_id").eq(vendorId))
                .offset(offset)
                .limit(size)
                .fetchInto(VendorOrderResponseDTO.class);

        // 查詢 vendor_user_order_view 並組裝到每個 storeOrder
        for (VendorOrderResponseDTO storeOrder : storeOrders) {
            Integer storeOrderId = storeOrder.getStoreOrderId();

            List<VendorUserOrderDTO> userOrders = dsl.select(
                            field("product_id"),
                            field("product_name"),
                            field("product_variant_id")
                    )
                    .from("vendor_user_order_view")
                    .where(field("store_order_id").eq(storeOrderId))
                    .fetchInto(VendorUserOrderDTO.class);

            // 查詢 vendor_product_variant_view 並組裝到每個 userOrder
            for (VendorUserOrderDTO userOrder : userOrders) {
                Integer productId = userOrder.getProductId();

                List<VendorProductVariantDTO> productVariants = dsl.select(
                                field("product_variant_id"),
                                field("color"),
                                field("size"),
                                field("quantity")
                        )
                        .from("vendor_product_variant_view")
                        .where(field("product_id").eq(productId))
                        .fetchInto(VendorProductVariantDTO.class);

                userOrder.setProductVariants(productVariants);
            }

            storeOrder.setOrders(userOrders);
        }

        return storeOrders;
    }

}