package com.clothingstore.shop.productfilter;

import org.jooq.Condition;
import org.jooq.SortField;

import java.util.List;

public record ProductFilterPlan(
        Condition condition,
        Condition matchedVariantCondition,
        List<SortField<?>> sortFields,
        int pageSize,
        int offset
) { }
