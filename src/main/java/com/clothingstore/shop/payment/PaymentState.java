package com.clothingstore.shop.payment;

public enum PaymentState {
    REQUIRES_PAYMENT,
    PROCESSING,
    CAPTURED,
    SETTLED,
    FAILED,
    CANCELLED
}
