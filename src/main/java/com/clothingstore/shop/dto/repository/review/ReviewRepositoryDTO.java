package com.clothingstore.shop.dto.repository.review;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class ReviewRepositoryDTO {
    Integer reviewId;
    BigDecimal rating;
    String comment;
    OffsetDateTime reviewDate;
    Integer fk_customerId;
    Integer fk_productId;

    // Getters and Setters
    public Integer getReviewId() {
        return reviewId;
    }

    public void setReviewId(Integer reviewId) {
        this.reviewId = reviewId;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public OffsetDateTime getReviewDate() {
        return reviewDate;
    }

    public void setReviewDate(OffsetDateTime reviewDate) {
        this.reviewDate = reviewDate;
    }

    public Integer getFk_customerId() {
        return fk_customerId;
    }

    public void setFk_customerId(Integer fk_customerId) {
        this.fk_customerId = fk_customerId;
    }

    public Integer getFk_productId() {
        return fk_productId;
    }

    public void setFk_productId(Integer fk_productId) {
        this.fk_productId = fk_productId;
    }
}
