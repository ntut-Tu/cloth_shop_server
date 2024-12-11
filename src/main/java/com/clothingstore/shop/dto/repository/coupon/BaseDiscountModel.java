package com.clothingstore.shop.dto.repository.coupon;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME, // 使用类型标识符
        include = JsonTypeInfo.As.PROPERTY, // 类型信息作为 JSON 的属性
        property = "type" // JSON 中的属性名，标识类型
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = StandardDiscountDTO.class, name = "Standard"),
        @JsonSubTypes.Type(value = SpecialDiscountDTO.class, name = "Special")
})
public class BaseDiscountModel {
    // 公共字段（可以扩展）
}