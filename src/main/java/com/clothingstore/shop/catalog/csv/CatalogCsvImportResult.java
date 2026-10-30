package com.clothingstore.shop.catalog.csv;

import java.util.List;

public record CatalogCsvImportResult(int total, int succeeded, int failed, List<RowResult> rows) {
    public record RowResult(int row, String operation, boolean success, Integer productId,
                            Integer variantId, String message) {
    }
}
