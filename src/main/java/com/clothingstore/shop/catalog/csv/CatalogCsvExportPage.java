package com.clothingstore.shop.catalog.csv;

public record CatalogCsvExportPage(byte[] content, Integer nextOffset) {
    public CatalogCsvExportPage {
        content = content.clone();
    }

    @Override
    public byte[] content() {
        return content.clone();
    }
}
