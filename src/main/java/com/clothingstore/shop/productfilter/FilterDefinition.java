package com.clothingstore.shop.productfilter;

import java.util.List;

public record FilterDefinition(
        String key,
        FilterControl control,
        FilterValueType valueType,
        FilterSource source,
        List<FilterOperator> operators,
        List<String> options
) {
    public FilterDefinition {
        operators = List.copyOf(operators);
        options = options == null ? List.of() : List.copyOf(options);
    }
}
