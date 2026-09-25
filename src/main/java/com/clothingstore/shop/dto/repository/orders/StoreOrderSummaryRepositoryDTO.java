package com.clothingstore.shop.dto.repository.orders;

public class StoreOrderSummaryRepositoryDTO {
    private Integer storeOrderId;
    private String storeName;
    private String imageUrl;
    private String vendorCouponCode;

    // Getters and Setters
    public Integer getStoreOrderId() {
        return storeOrderId;
    }

    public void setStoreOrderId(Integer storeOrderId) {
        this.storeOrderId = storeOrderId;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getVendorCouponCode() {
        return vendorCouponCode;
    }

    public void setVendorCouponCode(String vendorCouponCode) {
        this.vendorCouponCode = vendorCouponCode;
    }

}

