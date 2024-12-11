package com.clothingstore.shop.dto.repository.coupon;

import com.clothingstore.shop.dto.repository.coupon.shared.SharedCoupon;
import com.clothingstore.shop.enums.CouponType;

import java.math.BigDecimal;

public class StandardDiscountDTO extends SharedCoupon {
    private Integer standard_discount_id;
    private String discount_type;
    private BigDecimal ratio;
    private Integer discount_amount;
    private Integer minimum_spend;

    //getters and setters
    public Integer getStandard_discount_id() {
        return standard_discount_id;
    }

    public void setStandard_discount_id(Integer standard_discount_id) {
        this.standard_discount_id = standard_discount_id;
    }

    public String getDiscount_type() {
        return discount_type;
    }

    public void setDiscount_type(String discount_type) {
        this.discount_type = discount_type;
    }

    public BigDecimal getRatio() {
        return ratio;
    }

    public void setRatio(BigDecimal ratio) {
        this.ratio = ratio;
    }

    public Integer getDiscount_amount() {
        return discount_amount;
    }

    public void setDiscount_amount(Integer discount_amount) {
        this.discount_amount = discount_amount;
    }

    public Integer getMinimum_spend() {
        return minimum_spend;
    }

    public void setMinimum_spend(Integer minimum_spend) {
        this.minimum_spend = minimum_spend;
    }
}
