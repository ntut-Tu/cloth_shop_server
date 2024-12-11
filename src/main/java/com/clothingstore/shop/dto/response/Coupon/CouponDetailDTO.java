package com.clothingstore.shop.dto.response.Coupon;


import com.clothingstore.shop.dto.repository.coupon.shared.SharedCoupon;

public class CouponDetailDTO {
    private Integer coupon_id;
    private String code;
    private String type;
    private String start_date;
    private String end_date;
    private boolean is_list;
    private Integer maximum_usage_per_customer;
    private SharedCoupon discount;

    //getters and setters// getters and setters
    public Integer getCoupon_id() {
        return coupon_id;
    }

    public void setCoupon_id(Integer coupon_id) {
        this.coupon_id = coupon_id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStart_date() {
        return start_date;
    }

    public void setStart_date(String start_date) {
        this.start_date = start_date;
    }

    public String getEnd_date() {
        return end_date;
    }

    public void setEnd_date(String end_date) {
        this.end_date = end_date;
    }

    public boolean isIs_list() {
        return is_list;
    }

    public void setIs_list(boolean is_list) {
        this.is_list = is_list;
    }

    public Integer getMaximum_usage_per_customer() {
        return maximum_usage_per_customer;
    }

    public void setMaximum_usage_per_customer(Integer maximum_usage_per_customer) {
        this.maximum_usage_per_customer = maximum_usage_per_customer;
    }

    public SharedCoupon getDiscount() {
        return discount;
    }

    public void setDiscount(SharedCoupon discount) {
        this.discount = discount;
    }
}
