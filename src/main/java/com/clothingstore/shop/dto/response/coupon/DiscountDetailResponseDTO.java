package com.clothingstore.shop.dto.response.coupon;


public class DiscountDetailResponseDTO {
    private Integer couponId;
    private String code;
    private String type;
    private String startDate;
    private String endDate;
    private boolean isList;
    private Integer maximumUsagePerCustomer;
    private Object discount;

    // Getters and Setters
    public Integer getCouponId() { return couponId; }
    public void setCouponId(Integer couponId) { this.couponId = couponId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public boolean isList() { return isList; }
    public void setList(boolean list) { isList = list; }

    public Integer getMaximumUsagePerCustomer() { return maximumUsagePerCustomer; }
    public void setMaximumUsagePerCustomer(Integer maximumUsagePerCustomer) {
        this.maximumUsagePerCustomer = maximumUsagePerCustomer;
    }

    public Object getDiscount() { return discount; }
    public void setDiscount(Object discount) { this.discount = discount; }
}

