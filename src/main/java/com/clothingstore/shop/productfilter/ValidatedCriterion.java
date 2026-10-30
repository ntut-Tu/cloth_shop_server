package com.clothingstore.shop.productfilter;

import java.util.List;

public record ValidatedCriterion(
        FilterDefinition definition,
        FilterOperator operator,
        List<Object> values
) { }
