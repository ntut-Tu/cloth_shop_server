package com.clothingstore.shop.catalog.csv;

import org.junit.jupiter.api.Test;
import org.jooq.exception.DataAccessException;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogCsvProcessorTest {
    private final CatalogCsvCodec codec = new CatalogCsvCodec();
    private final CatalogCsvRowMapper mapper = new CatalogCsvRowMapper();

    @Test
    void reportsValidationFailureAndContinuesWithFollowingRows() throws Exception {
        CatalogCsvMutationRepository repository = mock(CatalogCsvMutationRepository.class);
        when(repository.apply(eq(7), any())).thenReturn(new CatalogCsvMutationRepository.MutationResult(20, 30));
        CatalogCsvProcessor processor = processor(repository);
        String content = header() + "EDIT,,,,,,,,,,,\n"
                + "ADD,,,Shirt,,,Tops,true,blue,M,200,4\n";

        CatalogCsvImportResult result = processor.process(request(content));

        assertEquals(2, result.total());
        assertEquals(1, result.succeeded());
        assertEquals(1, result.failed());
        assertEquals(2, result.rows().get(0).row());
        assertEquals(3, result.rows().get(1).row());
        verify(repository).apply(eq(7), any());
    }

    @Test
    void rejectsMismatchedTemplateBeforeMutation() throws Exception {
        CatalogCsvMutationRepository repository = mock(CatalogCsvMutationRepository.class);
        assertThrows(IllegalArgumentException.class,
                () -> processor(repository).process(request("wrong,header\nADD,stuff\n")));
    }

    @Test
    void enforcesByteLimitBeforeParsing() throws Exception {
        CatalogCsvMutationRepository repository = mock(CatalogCsvMutationRepository.class);
        CatalogCsvProperties properties = properties();
        properties.setMaxFileBytes(5);
        CatalogCsvProcessor processor = new CatalogCsvProcessor(properties, template(), codec, mapper, repository);
        assertThrows(IllegalArgumentException.class, () -> processor.process(request(header())));
    }

    @Test
    void reportsConstraintFailureWithoutStoppingLaterRows() throws Exception {
        CatalogCsvMutationRepository repository = mock(CatalogCsvMutationRepository.class);
        when(repository.apply(eq(7), any()))
                .thenThrow(new DataAccessException("foreign key constraint"))
                .thenReturn(new CatalogCsvMutationRepository.MutationResult(21, 31));
        String content = header() + "DELETE,10,,,,,,,,,,\n"
                + "ADD,,,Shirt,,,Tops,true,blue,M,200,4\n";

        CatalogCsvImportResult result = processor(repository).process(request(content));

        assertEquals(2, result.total());
        assertEquals(1, result.failed());
        assertEquals("Database constraints prevent this change", result.rows().get(0).message());
        assertEquals(21, result.rows().get(1).productId());
    }

    private CatalogCsvProcessor processor(CatalogCsvMutationRepository repository) throws Exception {
        return new CatalogCsvProcessor(properties(), template(), codec, mapper, repository);
    }

    private CatalogCsvProperties properties() {
        CatalogCsvProperties properties = new CatalogCsvProperties();
        properties.setMaxRows(10);
        properties.setMaxFileBytes(10_000);
        properties.setTemplate(new org.springframework.core.io.ByteArrayResource(header().getBytes(StandardCharsets.UTF_8)));
        return properties;
    }

    private CatalogCsvTemplate template() throws Exception { return new CatalogCsvTemplate(properties(), codec); }
    private CatalogCsvImportRequest request(String csv) { return new CatalogCsvImportRequest(7, csv.getBytes(StandardCharsets.UTF_8)); }
    private String header() { return String.join(",", CatalogCsvTemplate.COLUMNS) + "\n"; }
}
