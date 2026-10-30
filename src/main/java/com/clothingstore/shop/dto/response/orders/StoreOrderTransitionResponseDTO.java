package com.clothingstore.shop.dto.response.orders;

import java.time.OffsetDateTime;

public record StoreOrderTransitionResponseDTO(
        Integer transitionId,
        Integer storeOrderId,
        Integer actorUserId,
        String fromStatus,
        String toStatus,
        Integer version,
        String idempotencyKey,
        String reason,
        boolean replayed,
        OffsetDateTime transitionedAt
) {
}
