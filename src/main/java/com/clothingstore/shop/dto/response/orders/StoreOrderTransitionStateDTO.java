package com.clothingstore.shop.dto.response.orders;

import java.util.List;

public record StoreOrderTransitionStateDTO(
        Integer storeOrderId,
        String status,
        Integer version,
        List<String> allowedTargets
) {
}
