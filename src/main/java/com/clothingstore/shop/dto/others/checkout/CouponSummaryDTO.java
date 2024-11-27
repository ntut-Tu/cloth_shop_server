package com.clothingstore.shop.dto.others.checkout;

public class CouponSummaryDTO {
    private String discountType;
    private boolean isList;
    private String code;
    private String startDate;
    private String endDate;
    private int maxUsage;
    private DiscountDetailsDTO discountDetails;

    // Getters and setters
    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public boolean isList() {
        return isList;
    }

    public void setList(boolean isList) {
        this.isList = isList;
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

    public int getMaxUsage() {
        return maxUsage;
    }

    public void setMaxUsage(int maxUsage) {
        this.maxUsage = maxUsage;
    }

    public DiscountDetailsDTO getDiscountDetails() {
        return discountDetails;
    }

    public void setDiscountDetails(DiscountDetailsDTO discountDetails) {
        this.discountDetails = discountDetails;
    }
}
