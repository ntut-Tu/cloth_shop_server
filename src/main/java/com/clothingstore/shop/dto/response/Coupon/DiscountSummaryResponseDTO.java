package com.clothingstore.shop.dto.response.Coupon;

public class DiscountSummaryResponseDTO {
    private Integer discountId; // 唯一标识符
    private String discountType; // 优惠类型 (e.g., "Seasonal", "Shipping", "Special")
    private String code; // 优惠代码
    private String startDate; // 开始日期
    private String endDate; // 结束日期
    private boolean isActive; // 当前优惠是否生效

    // Getters and Setters
    public Integer getDiscountId() {
        return discountId;
    }

    public void setDiscountId(Integer discountId) {
        this.discountId = discountId;
    }

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}

