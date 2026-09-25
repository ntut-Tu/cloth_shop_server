package com.clothingstore.shop.dto.repository.coupon;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = StandardDiscountModel.class, name = "standard"),

        @JsonSubTypes.Type(value = SpecialDiscountModel.class, name = "special")
})
public class BaseDiscountModel {
}