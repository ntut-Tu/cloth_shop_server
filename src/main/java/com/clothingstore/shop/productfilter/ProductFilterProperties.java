package com.clothingstore.shop.productfilter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "catalog.filters")
public class ProductFilterProperties {
    private List<Entry> common = new ArrayList<>();
    private Map<String, List<Entry>> categories = new HashMap<>();

    public List<Entry> getCommon() { return common; }
    public void setCommon(List<Entry> common) { this.common = common; }
    public Map<String, List<Entry>> getCategories() { return categories; }
    public void setCategories(Map<String, List<Entry>> categories) { this.categories = categories; }

    public static class Entry {
        private String key;
        private FilterControl control;
        private FilterValueType valueType;
        private FilterSource source;
        private List<FilterOperator> operators = new ArrayList<>();
        private List<String> options = new ArrayList<>();

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public FilterControl getControl() { return control; }
        public void setControl(FilterControl control) { this.control = control; }
        public FilterValueType getValueType() { return valueType; }
        public void setValueType(FilterValueType valueType) { this.valueType = valueType; }
        public FilterSource getSource() { return source; }
        public void setSource(FilterSource source) { this.source = source; }
        public List<FilterOperator> getOperators() { return operators; }
        public void setOperators(List<FilterOperator> operators) { this.operators = operators; }
        public List<String> getOptions() { return options; }
        public void setOptions(List<String> options) { this.options = options; }
    }
}
