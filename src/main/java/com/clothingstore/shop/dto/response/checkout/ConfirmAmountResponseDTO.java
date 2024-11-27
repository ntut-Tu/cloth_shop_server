package com.clothingstore.shop.dto.response.checkout;

public class ConfirmAmountResponseDTO {

    private Integer totalAmount;
    private Integer shippingFee;
    private Integer discountAmount;
    private Integer finalAmount;

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
}

/*
export interface ConfirmAmountResponseModel {
  total_amount: number;
  shipping_fee: number;
  discount_amount: number;
  final_amount: number;
}
 */