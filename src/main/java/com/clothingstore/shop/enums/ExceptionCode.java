package com.clothingstore.shop.enums;

public enum ExceptionCode {
    PRODUCT_NOT_FOUND(1001, "Product not found"),
    DISCOUNT_NOT_AVAILABLE(1002, "Discount not available"),
    INVALID_COUPON_CODE(1003, "Invalid coupon code"),
    OUT_OF_STOCK(1004, "Out of stock"),
    FAILED_TO_QUERY(1005, "Failed to query");

    private final int code;
    private final String message;

    ExceptionCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
