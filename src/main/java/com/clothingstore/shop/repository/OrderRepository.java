package com.clothingstore.shop.repository;

import com.clothingstore.shop.dto.repository.orders.OrderDetailRepositoryDTO;
import com.clothingstore.shop.dto.repository.orders.OrderSummaryRepositoryDTO;
import com.clothingstore.shop.dto.repository.orders.OrderSummeryDetailModel;
import com.clothingstore.shop.dto.repository.orders.StoreOrderSummaryRepositoryDTO;
import com.clothingstore.shop.dto.response.vendorOrder.VendorOrderResponseDTO;
import com.clothingstore.shop.dto.response.vendorOrder.vendorUserOrder.VendorProductVariantDTO;
import com.clothingstore.shop.dto.response.vendorOrder.vendorUserOrder.VendorUserOrderDTO;
import com.clothingstore.shop.enums.PayStatus;
import com.clothingstore.shop.enums.RoleType;
import com.clothingstore.shop.enums.ShipStatus;
import com.clothingstore.shop.enums.StoreOrderStatus;
import com.clothingstore.shop.exceptions.SharedException;
import org.apache.commons.lang3.tuple.Pair;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

import static com.clothingstore.shop.jooq.Tables.*;
import static org.jooq.impl.DSL.*;

@Repository
public class OrderRepository {

    private final DSLContext dsl;
    @Autowired
    public OrderRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    // 查詢訂單簡介
    public List<OrderSummaryRepositoryDTO> findOrderSummariesByCustomerId(Integer userId, int limit, int offset) throws SharedException {
        try {
            return dsl.select(
                            ORDER.ORDER_ID,
                            ORDER.ORDER_DATE,
                            ORDER.TOTAL_AMOUNT,
                            ORDER.PAY_STATUS,
                            ORDER.SHIP_STATUS,
                            COUPON.CODE.as("shippingDiscountCode"),
                            ORDER.CREDIT_CARD_LAST_FOUR,
                            ORDER.PAYMENT_METHOD,
                            ORDER.SHIPPING_ADDRESS,
                            ORDER.DELIVER_TYPE)
                    .from(ORDER)
                    .join(CUSTOMER).on(ORDER.FK_CUSTOMER_ID.eq(CUSTOMER.CUSTOMER_ID))
                    .leftOuterJoin(COUPON).on(ORDER.FK_SHIPPING_DISCOUNT_ID.eq(COUPON.COUPON_ID))
                    .where(CUSTOMER.FK_USER_ID.eq(userId))
                    .orderBy(ORDER.ORDER_DATE.desc())
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

    public List<OrderSummaryRepositoryDTO> findAllOrderSummaries(int size, int offset) {
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
                .leftOuterJoin(COUPON).on(ORDER.FK_SHIPPING_DISCOUNT_ID.eq(COUPON.COUPON_ID))
                .orderBy(ORDER.ORDER_DATE.desc())
                .limit(size)
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
    }

    //查詢商家訂單簡介（延遲加載）
    public List<StoreOrderSummaryRepositoryDTO> findStoreOrdersByOrderId(Integer orderId) {
        return dsl.select(
                        STORE_ORDER.STORE_ORDER_ID.as("storeOrderId"),
                        USERS.ACCOUNT.as("storeName"),
                        USERS.PROFILE_PIC_URL.as("imageUrl"),
                        COUPON.CODE.as("vendorCouponCode"))
                .from(STORE_ORDER)
                .join(VENDOR).on(STORE_ORDER.FK_VENDOR_ID.eq(VENDOR.VENDOR_ID))
                .join(USERS).on(VENDOR.FK_USER_ID.eq(USERS.USER_ID))
                .leftOuterJoin(COUPON).on(STORE_ORDER.SEASONAL_DISCOUNT_ID.eq(COUPON.COUPON_ID).or(STORE_ORDER.SPECIAL_DISCOUNT_ID.eq(COUPON.COUPON_ID)))
                .where(STORE_ORDER.FK_ORDER_ID.eq(orderId))
                .fetchInto(StoreOrderSummaryRepositoryDTO.class);
    }

    public boolean isOrderBelongToCustomer(Integer userId, Integer storeOrderId) {
        return dsl.fetchExists(ORDER.join(CUSTOMER).on(ORDER.FK_CUSTOMER_ID.eq(CUSTOMER.CUSTOMER_ID))
                        .join(STORE_ORDER).on(STORE_ORDER.FK_ORDER_ID.eq(ORDER.ORDER_ID))
                .where(CUSTOMER.FK_USER_ID.eq(userId).and(STORE_ORDER.STORE_ORDER_ID.eq(storeOrderId))));
    }

    public boolean isEditOrderStatusValid(String role, Integer orderId) {
        String shipStatus = dsl.select(ORDER.SHIP_STATUS)
                .from(ORDER)
                .where(ORDER.ORDER_ID.eq(orderId))
                .fetchOneInto(String.class);
        if(role.equals(RoleType.CUSTOMER.getRole()) && shipStatus.equalsIgnoreCase(ShipStatus.DELIVERED.toString())){
            return true;
        }else{
            throw new IllegalArgumentException("Invalid role");
        }
    }

    public void updateOrderStatus(Integer orderId, String status) {
        String payStatus = dsl.select(ORDER.PAY_STATUS)
                .from(ORDER)
                .where(ORDER.ORDER_ID.eq(orderId))
                .fetchOneInto(String.class);
        if(payStatus.equalsIgnoreCase(PayStatus.PENDING.toString())){
            dsl.update(ORDER)
                    .set(ORDER.PAY_STATUS, PayStatus.PAID.toString())
                    .where(ORDER.ORDER_ID.eq(orderId))
                    .execute();
        }
        dsl.update(ORDER)
                .set(ORDER.SHIP_STATUS, status)
                .where(ORDER.ORDER_ID.eq(orderId))
                .execute();
    }

    public List<OrderDetailRepositoryDTO> findOrderDetailsByOrderId(Integer storeOrderId) {
        return dsl.select(
                        ORDER_ITEM.ORDER_ITEM_ID.as("order_item_id"),
                        ORDER_ITEM.QUANTITY.as("quantity"),
                        ORDER_ITEM.UNIT_PRICE.as("unit_price"),
                        ORDER_ITEM.TOTAL_PRICE.as("total_price"),
                        PRODUCT.IMAGE_URL.as("order_image_url"),
                        PRODUCT.NAME.as("product_name"),
                        PRODUCT_VARIANT.SIZE.as("size"),
                        PRODUCT_VARIANT.COLOR.as("color"))
                .from(ORDER_ITEM)
                .join(PRODUCT_VARIANT).on(ORDER_ITEM.FK_PRODUCT_VARIANT_ID.eq(PRODUCT_VARIANT.PRODUCT_VARIANT_ID))
                .join(PRODUCT).on(PRODUCT_VARIANT.FK_PRODUCT_ID.eq(PRODUCT.PRODUCT_ID))
                .where(ORDER_ITEM.FK_STORE_ORDER_ID.eq(storeOrderId))
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

    public List<VendorOrderResponseDTO> findStoreOrderSummariesByVendorId(Integer vendorId, int size, int offset) {
        // Reference the updated views
        Table<?> vendorOrderResponseView = table("vendor_order_response_view");
        Table<?> vendorProductVariantView = table("vendor_product_variant_view");
        Table<?> vendorUserOrderView = table("vendor_user_order_view");

        // Query for VendorOrderResponseDTO
        return dsl.select(
                        field("vorv.store_order_id").as("storeOrderId"),
                        field("vorv.total_amount").as("totalAmount"),
                        field("vorv.store_order_status").as("storeOrderStatus"),
                        field("vorv.order_date").as("orderDate"),
                        multiset(
                                select(
                                        field("vuov.product_id").as("productId"),
                                        field("vuov.product_name").as("productName"),
                                        multiset(
                                                select(
                                                        field("vpvv.product_variant_id").as("productVariantId"),
                                                        field("vpvv.color").as("color"),
                                                        field("vpvv.size").as("size"),
                                                        field("vpvv.quantity").as("quantity")
                                                )
                                                        .from(vendorProductVariantView.as("vpvv"))
                                                        .where(
                                                                field("vpvv.store_order_id").eq(field("vorv.store_order_id"))
                                                        )
                                        ).as("productVariants")
                                )
                                        .from(vendorUserOrderView.as("vuov"))
                                        .where(field("vuov.store_order_id").eq(field("vorv.store_order_id")))
                        ).as("orders")
                )
                .from(vendorOrderResponseView.as("vorv"))
                .where(field("vorv.vendor_id").eq(vendorId))
                .orderBy(field("vorv.store_order_id").desc())
                .limit(size)
                .offset(offset)
                .fetchInto(VendorOrderResponseDTO.class);
    }

}