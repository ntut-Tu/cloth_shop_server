package com.clothingstore.shop.dto.response.checkout.others;

import java.math.BigDecimal;

public class DiscountDetailsDTO {
    private String discountType; // "percentage" or "fixed"
    private BigDecimal ratio; // for percentage discount
    private Integer amount; // for fixed discount
    private String applyShippingMethod; // for shipping discounts only
    private Integer buyQuantity; // for special discounts only
    private Integer giftQuantity; // for special discounts only
    private Integer buyVariantId; // for special discounts only
    private Integer giftVariantId; // for special discounts only

    // Getters and setters
}
