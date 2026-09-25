package com.clothingstore.shop.enums;

public enum ShipStatus {
    PENDING("Pending"),
    SHIPPED("Shipped"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled"),
    REFUND_REQUESTED("Refund Requested"),
    COMPLETED("Completed");

    private final String status;

    ShipStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
