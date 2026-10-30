package com.clothingstore.shop.productfilter;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class ProductFilterValidator {
    private static final Set<String> SORTS = Set.of("newest", "sold", "rating", "price_asc", "price_desc");
    private final ProductFilterRegistry registry;

    public ProductFilterValidator(ProductFilterRegistry registry) {
        this.registry = registry;
    }

    public ValidatedFilterRequest validate(ProductFilterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Filter request is required");
        }
        String category = registry.category(request.category());
        int page = request.page() == null ? 1 : request.page();
        int pageSize = request.pageSize() == null ? 30 : request.pageSize();
        if (page < 1 || pageSize < 1 || pageSize > 100 || ((long) (page - 1) * pageSize) > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid pagination; page must be positive and pageSize must be 1–100");
        }
        String sort = request.sort() == null ? "newest" : request.sort();
        if (!SORTS.contains(sort)) {
            throw new IllegalArgumentException("Unsupported sort: " + sort);
        }
        List<FilterCriterion> filters = request.filters() == null ? List.of() : request.filters();
        if (filters.size() > 20) {
            throw new IllegalArgumentException("At most 20 filters are allowed");
        }
        Set<String> usedKeys = new HashSet<>();
        List<ValidatedCriterion> validated = new ArrayList<>();
        for (FilterCriterion filter : filters) {
            if (filter == null || filter.key() == null || !usedKeys.add(filter.key())) {
                throw new IllegalArgumentException("Each filter key must occur once");
            }
            FilterDefinition definition = registry.resolve(category, filter.key());
            if (filter.operator() == null || !definition.operators().contains(filter.operator())) {
                throw new IllegalArgumentException("Unsupported operator for filter: " + filter.key());
            }
            List<String> raw = filter.values();
            int required = filter.operator() == FilterOperator.BETWEEN ? 2 : 1;
            if (raw == null || raw.size() < required ||
                    (filter.operator() != FilterOperator.IN && raw.size() != required) ||
                    (filter.operator() == FilterOperator.IN && raw.size() > 20)) {
                throw new IllegalArgumentException("Invalid value count for filter: " + filter.key());
            }
            List<Object> values = new ArrayList<>();
            for (String value : raw) {
                values.add(parse(definition, value));
            }
            if (filter.operator() == FilterOperator.BETWEEN &&
                    compare(values.get(0), values.get(1)) > 0) {
                throw new IllegalArgumentException("Range lower bound exceeds upper bound: " + filter.key());
            }
            validated.add(new ValidatedCriterion(definition, filter.operator(), List.copyOf(values)));
        }
        return new ValidatedFilterRequest(category, List.copyOf(validated), sort, pageSize, (page - 1) * pageSize);
    }

    private static Object parse(FilterDefinition definition, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Filter value must not be blank: " + definition.key());
        }
        String trimmed = value.trim();
        try {
            return switch (definition.valueType()) {
                case BOOLEAN -> {
                    if (!trimmed.equalsIgnoreCase("true") && !trimmed.equalsIgnoreCase("false")) {
                        throw new IllegalArgumentException("Expected a boolean: " + definition.key());
                    }
                    yield Boolean.parseBoolean(trimmed);
                }
                case INTEGER -> {
                    int number = Integer.parseInt(trimmed);
                    if (number < 0) {
                        throw new IllegalArgumentException("Negative values are not allowed: " + definition.key());
                    }
                    yield number;
                }
                case DECIMAL -> {
                    BigDecimal number = new BigDecimal(trimmed);
                    if (number.compareTo(BigDecimal.ZERO) < 0 || number.compareTo(new BigDecimal("5")) > 0) {
                        throw new IllegalArgumentException("Rating must be between 0 and 5");
                    }
                    yield number;
                }
                case TEXT -> {
                    if (trimmed.length() > 200) {
                        throw new IllegalArgumentException("Filter value is too long: " + definition.key());
                    }
                    if (!definition.options().isEmpty()) {
                        yield definition.options().stream()
                                .filter(option -> option.equalsIgnoreCase(trimmed))
                                .findFirst()
                                .orElseThrow(() -> new IllegalArgumentException("Unsupported option for filter: " + definition.key()));
                    }
                    yield trimmed;
                }
            };
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid numeric value for filter: " + definition.key(), ex);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static int compare(Object lower, Object upper) {
        return ((Comparable) lower).compareTo(upper);
    }
}
