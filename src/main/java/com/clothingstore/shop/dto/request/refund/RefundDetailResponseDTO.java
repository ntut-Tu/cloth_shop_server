package com.clothingstore.shop.dto.request.refund;

public class RefundDetailResponseDTO {
    private Integer order_item_id;
    private String request_target;
    private String status_type;
    private Boolean is_closed;
    private String refund_reason;
    private String vendor_response;
    private String admin_response;
    private String updated_at;

    // getters and setters
    public Integer getOrder_item_id() {
        return order_item_id;
    }

    public void setOrder_item_id(Integer order_item_id) {
        this.order_item_id = order_item_id;
    }

    public String getRequest_target() {
        return request_target;
    }

    public void setRequest_target(String request_target) {
        this.request_target = request_target;
    }

    public String getStatus_type() {
        return status_type;
    }

    public void setStatus_type(String status_type) {
        this.status_type = status_type;
    }

    public Boolean getIs_closed() {
        return is_closed;
    }

    public void setIs_closed(Boolean is_closed) {
        this.is_closed = is_closed;
    }

    public String getRefund_reason() {
        return refund_reason;
    }

    public void setRefund_reason(String refund_reason) {
        this.refund_reason = refund_reason;
    }

    public String getVendor_response() {
        return vendor_response;
    }

    public void setVendor_response(String vendor_response) {
        this.vendor_response = vendor_response;
    }

    public String getAdmin_response() {
        return admin_response;
    }

    public void setAdmin_response(String admin_response) {
        this.admin_response = admin_response;
    }

    public String getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(String updated_at) {
        this.updated_at = updated_at;
    }


}
