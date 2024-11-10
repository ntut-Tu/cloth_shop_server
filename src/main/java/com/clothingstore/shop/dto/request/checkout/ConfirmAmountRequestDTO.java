package com.clothingstore.shop.dto.request.checkout;

import com.clothingstore.shop.dto.response.checkout.others.CartItemDTO;

import java.util.List;

public class ConfirmAmountRequestDTO {
    private List<CartItemDTO> cartItems;
    private String shippingDiscountCode;
    private String seasonalDiscountCode;
    private String specialDiscountCode;

    // Getters and setters
}


