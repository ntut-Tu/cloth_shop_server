package com.clothingstore.shop.webhook;

import java.time.OffsetDateTime;
import java.util.Set;

public record SubscriptionView(long id, String endpointUrl, Set<String> events,
                               boolean active, OffsetDateTime createdAt) { }
