package com.clothingstore.shop.enums;

public enum ShipStatus {
    PENDING("Pending"),
    SHIPPED("Shipped"),
    DELIVERED("Delivered");

    private final String status;

    ShipStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
