package com.clothingstore.shop.dto.response.checkout;

public class PlaceOrderResponseDTO {
    private String orderId;
    private String status; // e.g., "confirmed", "pending", "failed"
    private Integer totalAmount;
    private Integer discountAmount;
    private Integer finalAmount;
    private String estimatedDeliveryDate;

    // Getters and setters
}