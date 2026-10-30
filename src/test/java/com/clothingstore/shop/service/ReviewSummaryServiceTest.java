package com.clothingstore.shop.service;

import com.clothingstore.shop.dto.repository.review.ReviewRatingStats;
import com.clothingstore.shop.dto.response.review.ReviewSummaryResponseDTO;
import com.clothingstore.shop.repository.ReviewRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReviewSummaryServiceTest {
    private final ReviewRepository repository = mock(ReviewRepository.class);
    private final ReviewService service = new ReviewService(mock(JwtService.class), repository,
            mock(AuthService.class));

    @Test
    void returnsZeroAverageWhenProductHasNoReviews() {
        when(repository.getReviewRatingStats(8)).thenReturn(new ReviewRatingStats(0, BigDecimal.ZERO));

        assertEquals(new ReviewSummaryResponseDTO(8, 0, new BigDecimal("0.00")),
                service.getReviewSummary(8));
    }

    @Test
    void roundsAverageRatingToTwoDecimalPlaces() {
        when(repository.getReviewRatingStats(8))
                .thenReturn(new ReviewRatingStats(3, new BigDecimal("3.666666")));

        assertEquals(new ReviewSummaryResponseDTO(8, 3, new BigDecimal("3.67")),
                service.getReviewSummary(8));
    }

    @Test
    void rejectsInvalidProductIdentifiersBeforeQuerying() {
        assertThrows(IllegalArgumentException.class, () -> service.getReviewSummary(0));
        assertThrows(IllegalArgumentException.class, () -> service.getReviewSummary(null));
        verifyNoInteractions(repository);
    }
}
