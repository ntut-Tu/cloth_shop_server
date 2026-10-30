package com.clothingstore.shop.productfilter;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class ProductFilterRegistry {
    private static final Set<String> CATEGORIES = Set.of("cloth", "pants", "shoes", "accessories");
    private final Map<String, List<FilterDefinition>> definitions;

    public ProductFilterRegistry(ProductFilterProperties properties) {
        if (properties.getCommon() == null || properties.getCategories() == null ||
                !properties.getCategories().keySet().stream().map(s -> s.toLowerCase(Locale.ROOT)).collect(java.util.stream.Collectors.toSet()).equals(CATEGORIES)) {
            throw new IllegalStateException("Filter definitions must cover every product category");
        }
        Map<String, List<FilterDefinition>> built = new LinkedHashMap<>();
        List<FilterDefinition> common = definitionsFor(properties.getCommon());
        built.put("all", common);
        properties.getCategories().forEach((category, entries) -> {
            String normalized = category.toLowerCase(Locale.ROOT);
            LinkedHashMap<String, FilterDefinition> fields = new LinkedHashMap<>();
            common.forEach(definition -> fields.put(definition.key(), definition));
            definitionsFor(entries).forEach(definition -> {
                if (fields.putIfAbsent(definition.key(), definition) != null) {
                    throw new IllegalStateException("Duplicate filter key: " + definition.key());
                }
            });
            built.put(normalized, List.copyOf(fields.values()));
        });
        definitions = Map.copyOf(built);
    }

    public String category(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Category is required");
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (!definitions.containsKey(normalized)) {
            throw new IllegalArgumentException("Unknown product category: " + raw);
        }
        return normalized;
    }

    public List<FilterDefinition> forCategory(String rawCategory) {
        return definitions.get(category(rawCategory));
    }

    public FilterDefinition resolve(String rawCategory, String key) {
        if (key == null) {
            throw new IllegalArgumentException("Filter key is required");
        }
        return forCategory(rawCategory).stream()
                .filter(definition -> definition.key().equals(key))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Filter is not available for this category: " + key));
    }

    private static List<FilterDefinition> definitionsFor(List<ProductFilterProperties.Entry> entries) {
        if (entries == null) {
            throw new IllegalStateException("Filter definitions are missing");
        }
        List<FilterDefinition> definitions = new ArrayList<>();
        for (ProductFilterProperties.Entry entry : entries) {
            if (entry == null || entry.getKey() == null || !entry.getKey().matches("[a-z][A-Za-z0-9]{0,39}") ||
                    entry.getControl() == null || entry.getValueType() == null || entry.getSource() == null ||
                    entry.getOperators() == null || entry.getOperators().isEmpty()) {
                throw new IllegalStateException("Invalid filter definition");
            }
            FilterDefinition definition = new FilterDefinition(entry.getKey(), entry.getControl(),
                    entry.getValueType(), entry.getSource(), entry.getOperators(), entry.getOptions());
            validateDefinition(definition);
            definitions.add(definition);
        }
        if (definitions.stream().map(FilterDefinition::key).distinct().count() != definitions.size()) {
            throw new IllegalStateException("Duplicate filter key");
        }
        return List.copyOf(definitions);
    }

    private static void validateDefinition(FilterDefinition definition) {
        boolean valid = switch (definition.source()) {
            case NAME -> definition.control() == FilterControl.TEXT && definition.valueType() == FilterValueType.TEXT;
            case RATE -> definition.control() == FilterControl.OPERATOR_TEXT && definition.valueType() == FilterValueType.DECIMAL;
            case PRICE -> definition.control() == FilterControl.OPERATOR_TEXT && definition.valueType() == FilterValueType.INTEGER;
            case IN_STOCK -> definition.control() == FilterControl.CHECKBOX && definition.valueType() == FilterValueType.BOOLEAN;
            case COLOR -> definition.control() == FilterControl.SELECT && definition.valueType() == FilterValueType.TEXT && !definition.options().isEmpty();
            case SIZE -> definition.control() == FilterControl.DROPDOWN && definition.valueType() == FilterValueType.TEXT && !definition.options().isEmpty();
            case CUSTOM -> definition.control() == FilterControl.CUSTOM_FIELD && definition.valueType() == FilterValueType.TEXT;
        };
        if (!valid) {
            throw new IllegalStateException("Incompatible filter definition: " + definition.key());
        }
        Set<FilterOperator> allowed = switch (definition.control()) {
            case TEXT, CUSTOM_FIELD -> Set.of(FilterOperator.EQ, FilterOperator.CONTAINS);
            case SELECT -> Set.of(FilterOperator.IN);
            case DROPDOWN, CHECKBOX -> Set.of(FilterOperator.EQ);
            case OPERATOR_TEXT -> Set.of(FilterOperator.EQ, FilterOperator.GTE, FilterOperator.LTE, FilterOperator.BETWEEN);
        };
        if (!allowed.containsAll(definition.operators())) {
            throw new IllegalStateException("Invalid operators for filter: " + definition.key());
        }
    }
}
