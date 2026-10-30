package com.clothingstore.shop.productfilter;

import java.util.List;

public record ValidatedFilterRequest(
        String category,
        List<ValidatedCriterion> criteria,
        String sort,
        int pageSize,
        int offset
) { }
