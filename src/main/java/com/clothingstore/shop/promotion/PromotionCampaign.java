package com.clothingstore.shop.promotion;

import java.time.OffsetDateTime;

public record PromotionCampaign(
        long id,
        String code,
        String status,
        int latestVersion,
        Integer publishedVersion,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        Integer maxUses,
        Integer maxUsesPerCustomer,
        int useCount
) {}
