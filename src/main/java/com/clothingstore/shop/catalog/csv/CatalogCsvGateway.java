package com.clothingstore.shop.catalog.csv;

import org.springframework.integration.annotation.MessagingGateway;

@MessagingGateway(defaultRequestChannel = CatalogCsvChannels.IMPORT)
public interface CatalogCsvGateway {
    CatalogCsvImportResult importCsv(CatalogCsvImportRequest request);
}
