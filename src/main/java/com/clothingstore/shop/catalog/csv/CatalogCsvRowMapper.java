package com.clothingstore.shop.catalog.csv;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class CatalogCsvRowMapper {
    public CatalogCsvRow map(List<String> columns) {
        if (columns.size() != CatalogCsvTemplate.COLUMNS.size()) {
            throw new IllegalArgumentException("Expected " + CatalogCsvTemplate.COLUMNS.size() + " columns");
        }
        CatalogCsvRow.Operation operation;
        try {
            operation = CatalogCsvRow.Operation.valueOf(required(columns.get(0), "operation").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("operation must be ADD, EDIT, or DELETE");
        }
        Integer productId = number(columns.get(1), "product_id", false);
        Integer variantId = number(columns.get(2), "variant_id", false);
        String name = text(columns.get(3), "name", 50);
        String description = text(columns.get(4), "description", Integer.MAX_VALUE);
        String imageUrl = text(columns.get(5), "image_url", 500);
        String category = text(columns.get(6), "category", 50);
        Boolean listed = bool(columns.get(7));
        String color = text(columns.get(8), "color", Integer.MAX_VALUE);
        String size = text(columns.get(9), "size", 10);
        Integer price = number(columns.get(10), "price", true);
        Integer stock = number(columns.get(11), "stock", true);
        CatalogCsvRow row = new CatalogCsvRow(operation, productId, variantId, name, description,
                imageUrl, category, listed, color, size, price, stock);
        validate(row);
        return row;
    }

    private void validate(CatalogCsvRow row) {
        switch (row.operation()) {
            case ADD -> {
                if (row.variantId() != null) {
                    throw new IllegalArgumentException("ADD requires an empty variant_id");
                }
                if (row.color() == null || row.size() == null || row.price() == null || row.stock() == null) {
                    throw new IllegalArgumentException("ADD requires color, size, price, and stock");
                }
                if (row.productId() == null) {
                    if (row.name() == null || row.category() == null) {
                        throw new IllegalArgumentException("A new product requires name and category");
                    }
                } else if (row.name() != null || row.description() != null || row.imageUrl() != null
                        || row.category() != null || row.listed() != null) {
                    throw new IllegalArgumentException("ADD to an existing product accepts variant fields only");
                }
            }
            case EDIT -> {
                if (row.productId() == null) {
                    throw new IllegalArgumentException("EDIT requires product_id");
                }
                boolean productFields = row.name() != null || row.description() != null || row.imageUrl() != null
                        || row.category() != null || row.listed() != null;
                boolean variantFields = row.color() != null || row.size() != null || row.price() != null || row.stock() != null;
                if (!productFields && !variantFields) {
                    throw new IllegalArgumentException("EDIT requires at least one changed field");
                }
                if (variantFields && row.variantId() == null) {
                    throw new IllegalArgumentException("Variant changes require variant_id");
                }
            }
            case DELETE -> {
                if (row.productId() == null) {
                    throw new IllegalArgumentException("DELETE requires product_id");
                }
                if (row.name() != null || row.description() != null || row.imageUrl() != null
                        || row.category() != null || row.listed() != null || row.color() != null
                        || row.size() != null || row.price() != null || row.stock() != null) {
                    throw new IllegalArgumentException("DELETE accepts identifiers only");
                }
            }
        }
    }

    private String required(String value, String field) {
        String result = value.trim();
        if (result.isEmpty()) throw new IllegalArgumentException(field + " is required");
        return result;
    }

    private String text(String value, String field, int maxLength) {
        String result = CatalogCsvFormulaProtection.unescape(value);
        if (result.isBlank()) return null;
        if (result.length() > maxLength) throw new IllegalArgumentException(field + " is too long");
        return result;
    }

    private Integer number(String value, String field, boolean allowZero) {
        String result = value.trim();
        if (result.isEmpty()) return null;
        try {
            int parsed = Integer.parseInt(result);
            if (parsed < 0 || (!allowZero && parsed == 0)) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(field + " must be " + (allowZero ? "a nonnegative" : "a positive") + " integer");
        }
    }

    private Boolean bool(String value) {
        String result = value.trim();
        if (result.isEmpty()) return null;
        if (result.equalsIgnoreCase("true")) return true;
        if (result.equalsIgnoreCase("false")) return false;
        throw new IllegalArgumentException("is_list must be true or false");
    }
}
