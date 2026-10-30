package com.clothingstore.shop.catalog.csv;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Immutable template and schema shared by every import request. */
@Component
public class CatalogCsvTemplate {
    public static final List<String> COLUMNS = List.of(
            "operation", "product_id", "variant_id", "name", "description", "image_url",
            "category", "is_list", "color", "size", "price", "stock");

    private final byte[] bytes;
    private final List<String> columns;

    public CatalogCsvTemplate(CatalogCsvProperties properties, CatalogCsvCodec codec) throws IOException {
        if (properties.getTemplate() == null) {
            throw new IllegalStateException("catalog.csv.template must reference a CSV resource");
        }
        try (InputStream input = properties.getTemplate().getInputStream()) {
            bytes = input.readAllBytes();
        }
        List<List<String>> parsed = codec.parse(new String(bytes, StandardCharsets.UTF_8), 2);
        if (parsed.size() != 1 || !parsed.get(0).equals(COLUMNS)) {
            throw new IllegalStateException("The catalog CSV template must contain the supported header only");
        }
        columns = List.copyOf(parsed.get(0));
    }

    public byte[] bytes() { return bytes.clone(); }
    public List<String> columns() { return columns; }
}
