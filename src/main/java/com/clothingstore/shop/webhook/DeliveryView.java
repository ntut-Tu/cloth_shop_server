package com.clothingstore.shop.webhook;

import java.time.OffsetDateTime;

public record DeliveryView(long id, long eventId, String eventType, String status,
                           int attempts, Integer responseStatus, String lastError,
                           OffsetDateTime nextAttemptAt, OffsetDateTime deliveredAt) { }
