package com.clothingstore.shop.enums;

public enum PayStatus {
    PENDING("Pending"),
    PAID("Paid"),
    CANCELLED("Cancelled");
    private final String type;

    PayStatus(String type) {
        this.type = type;
    }
}
