package com.clothingstore.shop.dto.response.review;

import java.math.BigDecimal;

public record ReviewSummaryResponseDTO(Integer productId, int reviewCount, BigDecimal averageRating) {
}
