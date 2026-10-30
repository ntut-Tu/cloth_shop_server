package com.clothingstore.shop.promotion;

public class PromotionNotApplicableException extends RuntimeException {
    public PromotionNotApplicableException(String message, Throwable cause) {
        super(message, cause);
    }
}
