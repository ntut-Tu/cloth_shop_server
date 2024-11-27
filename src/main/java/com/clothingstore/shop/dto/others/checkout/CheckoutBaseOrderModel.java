package com.clothingstore.shop.dto.others.checkout;

import java.util.List;

public class CheckoutBaseOrderModel {
    private String order_date;
    private String shipping_discount_code;
    private List<CheckoutBaseStoreOrderModel> store_orders;

    // Getters and setters
    public String getOrder_date() {
        return order_date;
    }

    public void setOrder_date(String order_date) {
        this.order_date = order_date;
    }

    public String getShipping_discount_code() {
        return shipping_discount_code;
    }

    public void setShipping_discount_code(String shipping_discount_code) {
        this.shipping_discount_code = shipping_discount_code;
    }

    public List<CheckoutBaseStoreOrderModel> getStore_orders() {
        return store_orders;
    }

    public void setStore_orders(List<CheckoutBaseStoreOrderModel> store_orders) {
        this.store_orders = store_orders;
    }
}