package com.clothingstore.shop.dto.request.checkout;

import com.clothingstore.shop.dto.others.checkout.CheckoutBaseOrderModel;

public class SubmitOrderRequestDTO extends CheckoutBaseOrderModel {
    private String payment_method;
    private String credit_card_last_four;
    private String delivery_type;
    private String pickup_store;
    private String shipping_address;

    // Getters and setters
    public String getPayment_method() {
        return payment_method;
    }

    public void setPayment_method(String payment_method) {
        this.payment_method = payment_method;
    }

    public String getCredit_card_last_four() {
        return credit_card_last_four;
    }

    public void setCredit_card_last_four(String credit_card_last_four) {
        this.credit_card_last_four = credit_card_last_four;
    }

    public String getDelivery_type() {
        return delivery_type;
    }

    public void setDelivery_type(String delivery_type) {
        this.delivery_type = delivery_type;
    }

    public String getPickup_store() {
        return pickup_store;
    }

    public void setPickup_store(String pickup_store) {
        this.pickup_store = pickup_store;
    }

    public String getShipping_address() {
        return shipping_address;
    }

    public void setShipping_address(String shipping_address) {
        this.shipping_address = shipping_address;
    }
}
/*
export interface CheckoutBaseOrderModel {
  order_date?: string;
  shipping_discount_code?: string;
  store_orders: CheckoutBaseStoreOrderModel[];
}
export interface CheckoutBaseStoreOrderModel {
  tempDiscountCode?: string;
  store_id: number;
  special_discount_code?: string;
  seasonal_discount_code?: string;
  product_variants: CheckoutBaseProductVariantModel[];
}
export interface CheckoutBaseProductVariantModel {
  product_variant_id: number;
  quantity: number;
}
export interface SubmitOrderModel extends CheckoutBaseOrderModel{
  payment_method: string,
  credit_card_last_four : string,
  delivery_type : string,
  pickup_store : string,
  shipping_address : string,
}
 */