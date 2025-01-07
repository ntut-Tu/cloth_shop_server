package com.clothingstore.shop.repository;

import com.clothingstore.shop.dto.request.refund.RefundDetailResponseDTO;
import com.clothingstore.shop.dto.response.refund.RefundListSumResponse;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.clothingstore.shop.jooq.Tables.*;
import static org.jooq.impl.DSL.selectOne;

@Repository
public class RefundRepository {
    private final DSLContext dsl;

    public RefundRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Integer createRefund(Integer userId, RefundDetailResponseDTO refundDetailResponseDTO) {
        Integer refundId = dsl.insertInto(REFUND_REQUEST)
                .set(REFUND_REQUEST.FK_ORDER_ITEM_ID, refundDetailResponseDTO.getOrder_item_id())
                .set(REFUND_REQUEST.REFUND_REASON, refundDetailResponseDTO.getRefund_reason())
                .set(REFUND_REQUEST.REQUEST_TARGET, "vendor")
                .set(REFUND_REQUEST.STATUS_TYPE, "vendor_pending")
                .returning(REFUND_REQUEST.REFUND_ID)
                .fetchOne()
                .getValue(REFUND_REQUEST.REFUND_ID);
        return refundId;
    }


    public String getRefundStatus(Integer refundId) {
        return dsl.select(REFUND_REQUEST.STATUS_TYPE)
                .from(REFUND_REQUEST)
                .where(REFUND_REQUEST.REFUND_ID.eq(refundId))
                .fetchOne()
                .getValue(REFUND_REQUEST.STATUS_TYPE);
    }

    public Boolean isRefundNotClose(Integer refundId) {
        return dsl.select(REFUND_REQUEST.IS_CLOSED)
                .from(REFUND_REQUEST)
                .where(REFUND_REQUEST.REFUND_ID.eq(refundId))
                .fetchOne()
                .getValue(REFUND_REQUEST.IS_CLOSED);
    }

    public Integer updateRefund(RefundDetailResponseDTO refundDetailResponseDTO, Integer refundId, Integer userId, String role) {
        switch (role){
            case "vendor":
                if(refundDetailResponseDTO.getStatus_type().toLowerCase().matches(".*reject.*")) {
                    return dsl.update(REFUND_REQUEST)
                            .set(REFUND_REQUEST.VENDOR_RESPONSE, refundDetailResponseDTO.getVendor_response())
                            .set(REFUND_REQUEST.STATUS_TYPE, "vendor_rejected")
                            .set(REFUND_REQUEST.FK_VENDOR_ID, userId)
                            .where(REFUND_REQUEST.REFUND_ID.eq(refundId))
                            .execute();
                }else{
                    return dsl.update(REFUND_REQUEST)
                            .set(REFUND_REQUEST.VENDOR_RESPONSE, refundDetailResponseDTO.getVendor_response())
                            .set(REFUND_REQUEST.STATUS_TYPE, "vendor_approved")
                            .set(REFUND_REQUEST.FK_VENDOR_ID, userId)
                            .set(REFUND_REQUEST.IS_CLOSED, true)
                            .where(REFUND_REQUEST.REFUND_ID.eq(refundId))
                            .execute();
                }
            case "admin":
                if(refundDetailResponseDTO.getStatus_type().toLowerCase().matches(".*reject.*")){
                    return dsl.update(REFUND_REQUEST)
                            .set(REFUND_REQUEST.ADMIN_RESPONSE, refundDetailResponseDTO.getAdmin_response())
                            .set(REFUND_REQUEST.STATUS_TYPE, "admin_rejected")
                            .set(REFUND_REQUEST.FK_ADMIN_ID, userId)
                            .where(REFUND_REQUEST.REFUND_ID.eq(refundId))
                            .execute();
                }else{
                    return dsl.update(REFUND_REQUEST)
                            .set(REFUND_REQUEST.ADMIN_RESPONSE, refundDetailResponseDTO.getAdmin_response())
                            .set(REFUND_REQUEST.STATUS_TYPE, "admin_approved")
                            .set(REFUND_REQUEST.FK_ADMIN_ID, userId)
                            .set(REFUND_REQUEST.IS_CLOSED, true)
                            .where(REFUND_REQUEST.REFUND_ID.eq(refundId))
                            .execute();
                }
            case "customer":
                return dsl.update(REFUND_REQUEST)
                        .set(REFUND_REQUEST.STATUS_TYPE, "admin_pending")
                        .set(REFUND_REQUEST.REQUEST_TARGET, "admin")
                        .set(REFUND_REQUEST.REFUND_REASON, refundDetailResponseDTO.getRefund_reason())
                        .where(REFUND_REQUEST.REFUND_ID.eq(refundId))
                        .execute();
            default:
                throw new IllegalArgumentException("Invalid role");
        }
    }

    public Boolean isRequestExist(Integer orderItemId) {
        try{
            return dsl.fetchExists(selectOne()
                    .from(REFUND_REQUEST)
                    .where(REFUND_REQUEST.FK_ORDER_ITEM_ID.eq(orderItemId)));
        }catch (Exception e){
            throw  e;
        }
    }

    public RefundDetailResponseDTO fetchRefundDetailsByRefundId(Integer refundId) {
        try {
            return dsl.select(
                    REFUND_REQUEST.REFUND_ID,
                    REFUND_REQUEST.FK_ORDER_ITEM_ID,
                    REFUND_REQUEST.REFUND_REASON,
                    REFUND_REQUEST.REQUEST_TARGET,
                    REFUND_REQUEST.STATUS_TYPE,
                    REFUND_REQUEST.VENDOR_RESPONSE,
                    REFUND_REQUEST.ADMIN_RESPONSE,
                    REFUND_REQUEST.IS_CLOSED,
                    REFUND_REQUEST.CREATED_AT,
                    REFUND_REQUEST.UPDATED_AT
            )
                    .from(REFUND_REQUEST)
                    .where(REFUND_REQUEST.REFUND_ID.eq(refundId))
                    .fetchOne()
                    .into(RefundDetailResponseDTO.class);
        }catch (Exception e){
            throw e;
        }
    }

    public RefundDetailResponseDTO fetchRefundDetailsByOrderItem(Integer orderItemId) {
        try {
            return dsl.select(
                    REFUND_REQUEST.REFUND_ID,
                    REFUND_REQUEST.FK_ORDER_ITEM_ID,
                    REFUND_REQUEST.REFUND_REASON,
                    REFUND_REQUEST.REQUEST_TARGET,
                    REFUND_REQUEST.STATUS_TYPE,
                    REFUND_REQUEST.VENDOR_RESPONSE,
                    REFUND_REQUEST.ADMIN_RESPONSE,
                    REFUND_REQUEST.IS_CLOSED,
                    REFUND_REQUEST.CREATED_AT,
                    REFUND_REQUEST.UPDATED_AT
            )
                    .from(REFUND_REQUEST)
                    .where(REFUND_REQUEST.FK_ORDER_ITEM_ID.eq(orderItemId))
                    .fetchOne()
                    .into(RefundDetailResponseDTO.class);
        }catch (Exception e){
            throw e;
        }
    }

    public List<RefundListSumResponse> getRefundList(String role, Integer userId) {
    try {
        if (role.equals("customer")) {
            Integer customerId = dsl.select(CUSTOMER.CUSTOMER_ID)
                    .from(CUSTOMER)
                    .where(CUSTOMER.FK_USER_ID.eq(userId))
                    .fetchOne()
                    .getValue(CUSTOMER.CUSTOMER_ID);
            return dsl.select(
                            REFUND_REQUEST.REFUND_ID,
                            REFUND_REQUEST.FK_ORDER_ITEM_ID.as("order_item_id"),
                            REFUND_REQUEST.STATUS_TYPE.as("refund_status"),
                            REFUND_REQUEST.IS_CLOSED,
                            PRODUCT.NAME.as("item_name")
                    )
                    .from(REFUND_REQUEST)
                    .join(ORDER_ITEM).on(REFUND_REQUEST.FK_ORDER_ITEM_ID.eq(ORDER_ITEM.ORDER_ITEM_ID))
                    .join(PRODUCT_VARIANT).on(ORDER_ITEM.FK_PRODUCT_VARIANT_ID.eq(PRODUCT_VARIANT.PRODUCT_VARIANT_ID))
                    .join(PRODUCT).on(PRODUCT_VARIANT.FK_PRODUCT_ID.eq(PRODUCT.PRODUCT_ID))
                    .where(ORDER_ITEM.FK_ORDER_ID.in(
                            dsl.select(ORDER.ORDER_ID)
                                    .from(ORDER)
                                    .where(ORDER.FK_CUSTOMER_ID.eq(customerId))
                    ))
                    .fetchInto(RefundListSumResponse.class);
        } else if (role.equals("vendor")) {
            Integer vendorId = dsl.select(VENDOR.VENDOR_ID)
                    .from(VENDOR)
                    .where(VENDOR.FK_USER_ID.eq(userId))
                    .fetchOne()
                    .getValue(VENDOR.VENDOR_ID);
            return dsl.select(
                            REFUND_REQUEST.REFUND_ID,
                            REFUND_REQUEST.FK_ORDER_ITEM_ID.as("order_item_id"),
                            PRODUCT.NAME.as("item_name"),
                            REFUND_REQUEST.STATUS_TYPE.as("refund_status"),
                            REFUND_REQUEST.IS_CLOSED
                    )
                    .from(REFUND_REQUEST)
                    .join(ORDER_ITEM).on(REFUND_REQUEST.FK_ORDER_ITEM_ID.eq(ORDER_ITEM.ORDER_ITEM_ID))
                    .join(PRODUCT_VARIANT).on(ORDER_ITEM.FK_PRODUCT_VARIANT_ID.eq(PRODUCT_VARIANT.PRODUCT_VARIANT_ID))
                    .join(PRODUCT).on(PRODUCT_VARIANT.FK_PRODUCT_ID.eq(PRODUCT.PRODUCT_ID))
                    .join(STORE_ORDER).on(ORDER_ITEM.FK_STORE_ORDER_ID.eq(STORE_ORDER.STORE_ORDER_ID))
                    .where(STORE_ORDER.FK_VENDOR_ID.eq(vendorId))
                    .fetchInto(RefundListSumResponse.class);
        } else if (role.equals("admin")) {
            return dsl.select(
                            REFUND_REQUEST.REFUND_ID,
                            REFUND_REQUEST.FK_ORDER_ITEM_ID.as("order_item_id"),
                            PRODUCT.NAME.as("item_name"),
                            REFUND_REQUEST.STATUS_TYPE.as("refund_status"),
                            REFUND_REQUEST.IS_CLOSED
                    )
                    .from(REFUND_REQUEST)
                    .join(ORDER_ITEM).on(REFUND_REQUEST.FK_ORDER_ITEM_ID.eq(ORDER_ITEM.ORDER_ITEM_ID))
                    .join(PRODUCT_VARIANT).on(PRODUCT_VARIANT.PRODUCT_VARIANT_ID.eq(ORDER_ITEM.FK_PRODUCT_VARIANT_ID))
                    .join(PRODUCT).on(PRODUCT_VARIANT.FK_PRODUCT_ID.eq(PRODUCT.PRODUCT_ID))
                    .where(REFUND_REQUEST.REQUEST_TARGET.eq("admin"))
                    .fetchInto(RefundListSumResponse.class);
        } else {
            throw new IllegalArgumentException("Invalid role");
        }
    } catch (Exception e) {
        throw e;
    }
}
}
