package com.clothingstore.shop.payment;

import java.util.UUID;

public record PaymentView(
        UUID intentId,
        int orderId,
        int amount,
        String currency,
        PaymentState status,
        UUID attemptId,
        PaymentAttemptState attemptStatus) {
}
