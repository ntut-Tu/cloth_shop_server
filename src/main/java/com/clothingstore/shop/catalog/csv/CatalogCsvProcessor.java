package com.clothingstore.shop.catalog.csv;

import org.jooq.exception.DataAccessException;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Per-import state is isolated in a fresh prototype instance. */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CatalogCsvProcessor {
    private final CatalogCsvProperties properties;
    private final CatalogCsvTemplate template;
    private final CatalogCsvCodec codec;
    private final CatalogCsvRowMapper mapper;
    private final CatalogCsvMutationRepository repository;
    private final List<CatalogCsvImportResult.RowResult> results = new ArrayList<>();

    public CatalogCsvProcessor(CatalogCsvProperties properties, CatalogCsvTemplate template, CatalogCsvCodec codec,
                               CatalogCsvRowMapper mapper, CatalogCsvMutationRepository repository) {
        this.properties = properties;
        this.template = template;
        this.codec = codec;
        this.mapper = mapper;
        this.repository = repository;
    }

    public CatalogCsvImportResult process(CatalogCsvImportRequest request) {
        if (request.content() == null || request.content().length == 0) {
            throw new IllegalArgumentException("CSV is empty");
        }
        if (request.content().length > properties.getMaxFileBytes()) {
            throw new IllegalArgumentException("CSV exceeds the configured file limit");
        }
        String content;
        try {
            content = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(request.content())).toString();
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException("CSV must use UTF-8", exception);
        }
        List<List<String>> rows = codec.parse(content, properties.getMaxRows() + 1);
        if (!rows.get(0).equals(template.columns())) {
            throw new IllegalArgumentException("CSV header does not match the current template");
        }
        for (int i = 1; i < rows.size(); i++) {
            List<String> columns = rows.get(i);
            if (columns.stream().allMatch(String::isBlank)) continue;
            String operation = columns.isEmpty() ? "" : columns.get(0).trim();
            try {
                CatalogCsvRow row = mapper.map(columns);
                CatalogCsvMutationRepository.MutationResult mutation = repository.apply(request.vendorId(), row);
                results.add(new CatalogCsvImportResult.RowResult(i + 1, row.operation().name(), true,
                        mutation.productId(), mutation.variantId(), "Applied"));
            } catch (IllegalArgumentException exception) {
                results.add(new CatalogCsvImportResult.RowResult(i + 1, operation, false,
                        null, null, exception.getMessage()));
            } catch (DataAccessException | org.springframework.dao.DataAccessException exception) {
                results.add(new CatalogCsvImportResult.RowResult(i + 1, operation, false,
                        null, null, "Database constraints prevent this change"));
            }
        }
        int succeeded = (int) results.stream().filter(CatalogCsvImportResult.RowResult::success).count();
        return new CatalogCsvImportResult(results.size(), succeeded, results.size() - succeeded, List.copyOf(results));
    }
}
