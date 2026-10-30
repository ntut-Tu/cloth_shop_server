package com.clothingstore.shop.catalog.csv;

/** Makes text values safe to open in spreadsheet software while preserving CSV round trips. */
public final class CatalogCsvFormulaProtection {
    private CatalogCsvFormulaProtection() {
    }

    public static String escape(String value) {
        if (value == null || value.isEmpty()) return value;
        String trimmed = value.stripLeading();
        if (trimmed.isEmpty()) return value;
        char first = trimmed.charAt(0);
        return first == '\'' || dangerous(first) ? "'" + value : value;
    }

    public static String unescape(String value) {
        if (value == null || value.isEmpty()) return value;
        if (value.charAt(0) == '\'' && value.length() > 1) {
            String original = value.substring(1);
            String trimmedOriginal = original.stripLeading();
            if (!trimmedOriginal.isEmpty() &&
                    (trimmedOriginal.charAt(0) == '\'' || dangerous(trimmedOriginal.charAt(0)))) {
                return original;
            }
        }
        String trimmed = value.stripLeading();
        if (!trimmed.isEmpty() && dangerous(trimmed.charAt(0))) {
            throw new IllegalArgumentException("Spreadsheet formulas must be prefixed with an apostrophe");
        }
        return value;
    }

    private static boolean dangerous(char value) {
        return value == '=' || value == '+' || value == '-' || value == '@';
    }
}
