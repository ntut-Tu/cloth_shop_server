package com.clothingstore.shop.dto.others.checkout;

import java.util.List;

public class CheckoutBaseStoreOrderModel {
    private String tempDiscountCode;
    private int store_id;
    private String special_discount_code;
    private String seasonal_discount_code;
    private List<CheckoutBaseProductVariantModel> product_variants;

    // Getters and setters
    public String getTempDiscountCode() {
        return tempDiscountCode;
    }

    public void setTempDiscountCode(String tempDiscountCode) {
        this.tempDiscountCode = tempDiscountCode;
    }

    public int getStore_id() {
        return store_id;
    }

    public void setStore_id(int store_id) {
        this.store_id = store_id;
    }

    public String getSpecial_discount_code() {
        return special_discount_code;
    }

    public void setSpecial_discount_code(String special_discount_code) {
        this.special_discount_code = special_discount_code;
    }

    public String getSeasonal_discount_code() {
        return seasonal_discount_code;
    }

    public void setSeasonal_discount_code(String seasonal_discount_code) {
        this.seasonal_discount_code = seasonal_discount_code;
    }

    public List<CheckoutBaseProductVariantModel> getProduct_variants() {
        return product_variants;
    }

    public void setProduct_variants(List<CheckoutBaseProductVariantModel> product_variants) {
        this.product_variants = product_variants;
    }
}
