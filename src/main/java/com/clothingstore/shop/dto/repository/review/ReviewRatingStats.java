package com.clothingstore.shop.dto.repository.review;

import java.math.BigDecimal;

public record ReviewRatingStats(int count, BigDecimal averageRating) {
}
