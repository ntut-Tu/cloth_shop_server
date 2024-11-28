package com.clothingstore.shop.enums;

import java.util.ResourceBundle;

public enum CouponType {
    SPECIAL_DISCOUNT("special"),
    SHIPPING_DISCOUNT("shipping"),
    SEASONAL_DISCOUNT("seasonal");

    private final String type;

    CouponType(String type) {
        this.type = type;
    }

    public String getTranslation(String key) {
        ResourceBundle bundle = ResourceBundle.getBundle("messages_" + type);
        return bundle.getString(key);
    }
}
