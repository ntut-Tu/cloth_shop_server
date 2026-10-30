package com.clothingstore.shop.catalog.csv;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CatalogCsvIntegrationFlowTest {
    @Test
    void gatewayRoutesThroughConfiguredChannelToHandler() {
        CatalogCsvImportHandler handler = mock(CatalogCsvImportHandler.class);
        CatalogCsvImportResult expected = new CatalogCsvImportResult(0, 0, 0, List.of());
        when(handler.process(any())).thenReturn(expected);
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(CatalogCsvImportHandler.class, () -> handler);
            context.register(CatalogCsvConfiguration.class);
            context.refresh();
            CatalogCsvGateway gateway = context.getBean(CatalogCsvGateway.class);
            assertEquals(expected, gateway.importCsv(new CatalogCsvImportRequest(7, new byte[0])));
        }
    }
}
