package com.clothingstore.shop.catalog.csv;

public record CatalogCsvImportRequest(int vendorId, byte[] content) {
}
