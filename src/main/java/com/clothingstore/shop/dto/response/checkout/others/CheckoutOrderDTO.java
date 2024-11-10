package com.clothingstore.shop.dto.response.checkout.others;

import java.util.List;

public class CheckoutOrderDTO {
    private String orderDate;
    private String paymentMethod;
    private String creditCardLastFour;
    private String deliverType;
    private String pickupStore;
    private String shippingAddress;
    private DiscountsDTO discounts;
    private List<CartItemDTO> cartItems;

    // Getters and setters
}
