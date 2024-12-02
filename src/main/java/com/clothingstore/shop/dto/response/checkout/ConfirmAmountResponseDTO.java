package com.clothingstore.shop.dto.response.checkout;

public class ConfirmAmountResponseDTO {

    private Integer totalAmount;
    private Integer shippingFee;
    private Integer discountAmount;
    private Integer finalAmount;
    private String order_id;

    public ConfirmAmountResponseDTO() {}
    public ConfirmAmountResponseDTO(Integer totalAmount, Integer shippingFee, Integer discountAmount, Integer finalAmount, String order_id) {
        this.totalAmount = totalAmount;
        this.shippingFee = shippingFee;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
        this.order_id = order_id;
    }

    // Getters and setters
    public Integer getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Integer totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Integer getShippingFee() {
        return shippingFee;
    }

    public void setShippingFee(Integer shippingFee) {
        this.shippingFee = shippingFee;
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

    public String getOrder_id() {
        return order_id;
    }

    public void setOrder_id(String order_id) {
        this.order_id = order_id;
    }
}

/*
export interface ConfirmAmountResponseModel {
  total_amount: number;
  shipping_fee: number;
  discount_amount: number;
  final_amount: number;
}
 */