package com.clothingstore.shop.catalog.csv;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CatalogCsvCodecTest {
    private final CatalogCsvCodec codec = new CatalogCsvCodec();

    @Test
    void parsesQuotedCommasNewlinesEscapedQuotesAndBom() {
        String csv = "\uFEFFname,description\r\n\"Shirt, blue\",\"line 1\nline \"\"2\"\"\"\r\n";
        assertEquals(List.of(List.of("name", "description"),
                        List.of("Shirt, blue", "line 1\nline \"2\"")), codec.parse(csv, 2));
    }

    @Test
    void rejectsMalformedQuotesAndOversizedFiles() {
        assertThrows(IllegalArgumentException.class, () -> codec.parse("a,b\n\"broken", 5));
        assertThrows(IllegalArgumentException.class, () -> codec.parse("a,b\nc,d", 1));
    }

    @Test
    void encodesValuesForRoundTrip() {
        List<String> values = List.of("ADD", "A, B", "said \"hi\"", "two\nlines");
        assertEquals(values, codec.parse(codec.encode(values), 1).get(0));
    }
}
