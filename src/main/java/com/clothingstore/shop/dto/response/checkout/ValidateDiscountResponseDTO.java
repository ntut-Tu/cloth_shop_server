package com.clothingstore.shop.dto.response.checkout;

import com.clothingstore.shop.dto.response.checkout.others.DiscountDetailsDTO;

public class ValidateDiscountResponseDTO {
    private String discountCode;
    private String type; // "shipping", "seasonal", "special"
    private boolean expired;
    private boolean usedUp;
    private DiscountDetailsDTO discountDetails;

    // Getters and setters
}
