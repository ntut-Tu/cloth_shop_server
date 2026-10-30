package com.clothingstore.shop.catalog.csv;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class CatalogCsvImportHandler {
    private final ObjectProvider<CatalogCsvProcessor> processors;

    public CatalogCsvImportHandler(ObjectProvider<CatalogCsvProcessor> processors) {
        this.processors = processors;
    }

    public CatalogCsvImportResult process(CatalogCsvImportRequest request) {
        return processors.getObject().process(request);
    }
}
