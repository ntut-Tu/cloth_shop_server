package com.clothingstore.shop.promotion;

public record PromotionVersion(long campaignId, int version, PromotionRule rule, PromotionBenefit benefit) {}
