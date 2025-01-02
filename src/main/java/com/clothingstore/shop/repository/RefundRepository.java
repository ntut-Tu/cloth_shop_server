package com.clothingstore.shop.repository;

import com.clothingstore.shop.dto.request.refund.RefundDetailResponseDTO;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

import static com.clothingstore.shop.jooq.Tables.*;

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

    public Integer updateRefund(RefundDetailResponseDTO refundDetailResponseDTO, Integer refundId, String role) {
        switch (role){
            case "vendor":
                if(refundDetailResponseDTO.getVendor_response().equals("rejected")) {
                    return dsl.update(REFUND_REQUEST)
                            .set(REFUND_REQUEST.VENDOR_RESPONSE, refundDetailResponseDTO.getVendor_response())
                            .set(REFUND_REQUEST.STATUS_TYPE, "vendor_rejected")
                            .where(REFUND_REQUEST.REFUND_ID.eq(refundId))
                            .execute();
                }else{
                    return dsl.update(REFUND_REQUEST)
                            .set(REFUND_REQUEST.VENDOR_RESPONSE, refundDetailResponseDTO.getVendor_response())
                            .set(REFUND_REQUEST.STATUS_TYPE, "vendor_approved")
                            .where(REFUND_REQUEST.REFUND_ID.eq(refundId))
                            .execute();
                }
            case "admin":
                if(refundDetailResponseDTO.getAdmin_response().equals("rejected")){
                    return dsl.update(REFUND_REQUEST)
                            .set(REFUND_REQUEST.ADMIN_RESPONSE, refundDetailResponseDTO.getAdmin_response())
                            .set(REFUND_REQUEST.STATUS_TYPE, "admin_rejected")
                            .where(REFUND_REQUEST.REFUND_ID.eq(refundId))
                            .execute();
                }else{
                    return dsl.update(REFUND_REQUEST)
                            .set(REFUND_REQUEST.ADMIN_RESPONSE, refundDetailResponseDTO.getAdmin_response())
                            .set(REFUND_REQUEST.STATUS_TYPE, "admin_approved")
                            .set(REFUND_REQUEST.IS_CLOSED, true)
                            .where(REFUND_REQUEST.REFUND_ID.eq(refundId))
                            .execute();
                }
            case "customer":
                return dsl.update(REFUND_REQUEST)
                        .set(REFUND_REQUEST.STATUS_TYPE, "admin_pending")
                        .where(REFUND_REQUEST.REFUND_ID.eq(refundId))
                        .execute();
            default:
                throw new IllegalArgumentException("Invalid role");
        }
    }
}
