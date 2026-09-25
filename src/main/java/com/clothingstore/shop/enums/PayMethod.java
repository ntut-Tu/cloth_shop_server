package com.clothingstore.shop.enums;

public enum PayMethod {
    CASH("Cash"),
    CARD("Card");

    private final String method;

    PayMethod(String method) {
        this.method = method;
    }

    public String getMethod() {
        return method;
    }
}
