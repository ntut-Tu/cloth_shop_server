package com.clothingstore.shop.dto.response.checkout;

public class SubmitOrderResponseDTO {
    private Integer orderId;
    private String status; // e.g., "confirmed", "pending", "failed"
    private Integer totalAmount;
    private Integer discountAmount;
    private Integer finalAmount;
    private String estimatedDeliveryDate;

    public SubmitOrderResponseDTO() {}
    public SubmitOrderResponseDTO(Integer orderId, String status, Integer totalAmount, Integer discountAmount, Integer finalAmount, String estimatedDeliveryDate) {
        this.orderId = orderId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
        this.estimatedDeliveryDate = estimatedDeliveryDate;
    }

    // Getters and setters
    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Integer totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Integer getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Integer discountAmount) {
        this.discountAmount = discountAmount;
    }

    public Integer getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(Integer finalAmount) {
        this.finalAmount = finalAmount;
    }

    public String getEstimatedDeliveryDate() {
        return estimatedDeliveryDate;
    }

    public void setEstimatedDeliveryDate(String estimatedDeliveryDate) {
        this.estimatedDeliveryDate = estimatedDeliveryDate;
    }
}