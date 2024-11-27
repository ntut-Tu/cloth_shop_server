package com.clothingstore.shop.dto.others.checkout;

public class CheckoutBaseProductVariantModel {
    private int product_variant_id;
    private int quantity;

    // Getters and setters
    public int getProduct_variant_id() {
        return product_variant_id;
    }

    public void setProduct_variant_id(int product_variant_id) {
        this.product_variant_id = product_variant_id;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
