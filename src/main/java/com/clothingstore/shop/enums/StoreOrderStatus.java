package com.clothingstore.shop.enums;

public enum StoreOrderStatus {
    PENDING("Pending"),
    SHIPPED("Shipped"),
    CANCELLED("Cancelled");

    private final String status;

    StoreOrderStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
