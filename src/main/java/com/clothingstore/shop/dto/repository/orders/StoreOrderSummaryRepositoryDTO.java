package com.clothingstore.shop.dto.repository.orders;

public class StoreOrderSummaryRepositoryDTO {
    private Integer storeOrderId;
    private Integer vendorId;
    private Integer seasonalDiscountId;
    private Integer specialDiscountId;
    private Integer shippingDiscountId;
    // Getters and Setters
    public Integer getStoreOrderId() {
        return storeOrderId;
    }

    public void setStoreOrderId(Integer storeOrderId) {
        this.storeOrderId = storeOrderId;
    }

    public Integer getVendorId() {
        return vendorId;
    }

    public void setVendorId(Integer vendorId) {
        this.vendorId = vendorId;
    }

    public Integer getSeasonalDiscountId() {
        return seasonalDiscountId;
    }

    public void setSeasonalDiscountId(Integer seasonalDiscountId) {
        this.seasonalDiscountId = seasonalDiscountId;
    }

    public Integer getSpecialDiscountId() {
        return specialDiscountId;
    }

    public void setSpecialDiscountId(Integer specialDiscountId) {
        this.specialDiscountId = specialDiscountId;
    }

    public Integer getShippingDiscountId() {
        return shippingDiscountId;
    }

    public void setShippingDiscountId(Integer shippingDiscountId) {
        this.shippingDiscountId = shippingDiscountId;
    }



}

