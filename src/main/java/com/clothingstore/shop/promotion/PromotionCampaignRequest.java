package com.clothingstore.shop.promotion;

import java.time.OffsetDateTime;

public record PromotionCampaignRequest(
        String code,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        Integer maxUses,
        Integer maxUsesPerCustomer,
        PromotionRule rule,
        PromotionBenefit benefit
) {}
