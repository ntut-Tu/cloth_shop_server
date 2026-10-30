package com.clothingstore.shop.promotion;

public record PromotionRevisionRequest(
        Integer expectedLatestVersion,
        PromotionRule rule,
        PromotionBenefit benefit
) {}
