package com.clothingstore.shop.dto.repository.coupon;

import com.clothingstore.shop.dto.repository.coupon.shared.SharedCoupon;

public class SpecialDiscountDTO extends SharedCoupon {
    private Integer special_discount_id;
    private Integer buy_quantity;
    private Integer get_quantity;
    private Integer buy_variant_id;
    private Integer get_variant_id;

    //getters and setters
    public Integer getSpecial_discount_id() {
        return special_discount_id;
    }

    public void setSpecial_discount_id(Integer special_discount_id) {
        this.special_discount_id = special_discount_id;
    }

    public Integer getBuy_quantity() {
        return buy_quantity;
    }

    public void setBuy_quantity(Integer buy_quantity) {
        this.buy_quantity = buy_quantity;
    }

    public Integer getGet_quantity() {
        return get_quantity;
    }

    public void setGet_quantity(Integer get_quantity) {
        this.get_quantity = get_quantity;
    }

    public Integer getBuy_variant_id() {
        return buy_variant_id;
    }

    public void setBuy_variant_id(Integer buy_variant_id) {
        this.buy_variant_id = buy_variant_id;
    }

    public Integer getGet_variant_id() {
        return get_variant_id;
    }

    public void setGet_variant_id(Integer get_variant_id) {
        this.get_variant_id = get_variant_id;
    }
}
