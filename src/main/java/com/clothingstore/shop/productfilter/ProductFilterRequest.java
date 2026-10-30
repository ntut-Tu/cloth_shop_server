package com.clothingstore.shop.productfilter;

import java.util.List;

public record ProductFilterRequest(
        String category,
        List<FilterCriterion> filters,
        String sort,
        Integer page,
        Integer pageSize
) { }
