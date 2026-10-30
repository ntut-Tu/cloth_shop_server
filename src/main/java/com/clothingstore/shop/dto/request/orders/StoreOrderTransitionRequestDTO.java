package com.clothingstore.shop.dto.request.orders;

public record StoreOrderTransitionRequestDTO(
        String targetStatus,
        String idempotencyKey,
        Integer expectedVersion,
        String reason
) {
}
