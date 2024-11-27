package com.clothingstore.shop.dto.others.checkout;

public class CartItemDTO {
    private Integer productVariantId;
    private Integer quantity;

    //Getters and setters
    public Integer getProductVariantId() {
        return productVariantId;
    }

    public void setProductVariantId(Integer productVariantId) {
        this.productVariantId = productVariantId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
