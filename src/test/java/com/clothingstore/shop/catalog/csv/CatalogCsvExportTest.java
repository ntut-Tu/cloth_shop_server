package com.clothingstore.shop.catalog.csv;

import com.clothingstore.shop.repository.AuthRepository;
import com.clothingstore.shop.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CatalogCsvExportTest {
    private final CatalogCsvCodec codec = new CatalogCsvCodec();
    private final CatalogCsvMutationRepository repository = mock(CatalogCsvMutationRepository.class);
    private final CatalogCsvTemplate template = mock(CatalogCsvTemplate.class);
    private final CatalogCsvProperties properties = new CatalogCsvProperties();

    @Test
    void exportsAllProductsAsImportablePages() {
        properties.setMaxRows(2);
        when(template.columns()).thenReturn(CatalogCsvTemplate.COLUMNS);
        when(repository.export(7, 0, 3)).thenReturn(List.of(row(1), row(2), row(3)));
        when(repository.export(7, 2, 3)).thenReturn(List.of(row(3)));

        CatalogCsvExportPage first = service().export(7, 0);
        CatalogCsvExportPage second = service().export(7, first.nextOffset());

        assertEquals(2, first.nextOffset());
        assertNull(second.nextOffset());
        assertEquals(3, parse(first).size());
        assertEquals(2, parse(second).size());
        assertEquals(CatalogCsvTemplate.COLUMNS, parse(first).get(0));
        assertEquals(CatalogCsvTemplate.COLUMNS, parse(second).get(0));
    }

    @Test
    void byteLimitedExportContinuesAtNextUnwrittenRow() {
        properties.setMaxRows(10);
        when(template.columns()).thenReturn(CatalogCsvTemplate.COLUMNS);
        int headerSize = codec.encode(CatalogCsvTemplate.COLUMNS).getBytes(StandardCharsets.UTF_8).length;
        int firstSize = codec.encode(List.of("EDIT", "1", "1", "Shirt 1", "", "", "Cloth", "true",
                "Blue", "M", "100", "2")).getBytes(StandardCharsets.UTF_8).length;
        properties.setMaxFileBytes(headerSize + firstSize);
        when(repository.export(7, 0, 11)).thenReturn(List.of(row(1), row(2)));

        CatalogCsvExportPage page = service().export(7, 0);

        assertEquals(1, page.nextOffset());
        assertEquals(headerSize + firstSize, page.content().length);
        assertEquals(2, parse(page).size());
    }

    @Test
    void rejectsSingleRowThatCannotFitConfiguredLimit() {
        when(template.columns()).thenReturn(CatalogCsvTemplate.COLUMNS);
        properties.setMaxFileBytes(codec.encode(CatalogCsvTemplate.COLUMNS)
                .getBytes(StandardCharsets.UTF_8).length + 1);
        when(repository.export(7, 0, 1001)).thenReturn(List.of(row(1)));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service().export(7, 0));

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, error.getStatusCode());
    }

    @Test
    void exportedTextAndFormulaEscapesImportWithoutChangingWhitespace() {
        when(template.columns()).thenReturn(CatalogCsvTemplate.COLUMNS);
        CatalogCsvRow original = new CatalogCsvRow(CatalogCsvRow.Operation.EDIT, 1, 1,
                "  Shirt  ", "  =SUM(1,1)  ", null, "Cloth", true, "Blue", "M", 100, 2);
        when(repository.export(7, 0, 1001)).thenReturn(List.of(original));

        CatalogCsvRow imported = new CatalogCsvRowMapper().map(parse(service().export(7, 0)).get(1));

        assertEquals(original.name(), imported.name());
        assertEquals(original.description(), imported.description());
    }

    private CatalogCsvService service() {
        return new CatalogCsvService(mock(JwtService.class), mock(AuthRepository.class),
                mock(CatalogCsvGateway.class), repository, template, codec, properties);
    }

    private List<List<String>> parse(CatalogCsvExportPage page) {
        return codec.parse(new String(page.content(), StandardCharsets.UTF_8), 3);
    }

    private CatalogCsvRow row(int id) {
        return new CatalogCsvRow(CatalogCsvRow.Operation.EDIT, id, id, "Shirt " + id,
                null, null, "Cloth", true, "Blue", "M", 100, 2);
    }
}
