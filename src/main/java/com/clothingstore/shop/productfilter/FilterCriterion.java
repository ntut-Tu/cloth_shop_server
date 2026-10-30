package com.clothingstore.shop.productfilter;

import java.util.List;

public record FilterCriterion(String key, FilterOperator operator, List<String> values) { }
