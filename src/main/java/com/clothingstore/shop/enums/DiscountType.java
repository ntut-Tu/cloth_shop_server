package com.clothingstore.shop.enums;

public enum DiscountType {
    PERCENTAGE("Percentage"),
    SPECIAL("Special"),
    FIXED("Fixed");

    private final String type;

    DiscountType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
