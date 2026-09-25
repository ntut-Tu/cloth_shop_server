package com.clothingstore.shop.dto.repository.orders;

public class OrderSummeryDetailModel {
    private String creditCardLastFour;
    private String paymentMethod;
    private String shippingAddress;
    private String shippingMethod;
    // Getters and Setters
    public String getCreditCardLastFour() {
        return creditCardLastFour;
    }

    public void setCreditCardLastFour(String creditCardLastFour) {
        this.creditCardLastFour = creditCardLastFour;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getShippingMethod() {
        return shippingMethod;
    }

    public void setShippingMethod(String shippingMethod) {
        this.shippingMethod = shippingMethod;
    }
}
//creditCardLastFour?: number;
//paymentMethod: string;
//shippingAddress?: string;
//shippingMethod: string;