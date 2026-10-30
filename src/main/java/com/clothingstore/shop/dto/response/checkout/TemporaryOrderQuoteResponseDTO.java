package com.clothingstore.shop.dto.response.checkout;

public record TemporaryOrderQuoteResponseDTO(
        String orderId,
        Integer subtotal,
        Integer shippingFee,
        Integer storeDiscountAmount,
        Integer totalAmount,
        String expiresAt) {
}
