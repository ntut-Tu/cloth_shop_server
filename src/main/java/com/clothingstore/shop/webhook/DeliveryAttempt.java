package com.clothingstore.shop.webhook;

import java.util.UUID;

public record DeliveryAttempt(long id, long eventId, String eventType, String payload,
                              String endpointUrl, String secret, int attempts, UUID leaseToken) { }
