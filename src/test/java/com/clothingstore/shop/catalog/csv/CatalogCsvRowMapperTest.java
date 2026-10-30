package com.clothingstore.shop.catalog.csv;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CatalogCsvRowMapperTest {
    private final CatalogCsvRowMapper mapper = new CatalogCsvRowMapper();

    @Test
    void acceptsNewProductAndExistingProductVariant() {
        CatalogCsvRow product = mapper.map(row("ADD", "", "", "Shirt", "", "", "Tops", "true", "blue", "M", "200", "4"));
        assertEquals(CatalogCsvRow.Operation.ADD, product.operation());
        assertEquals("Shirt", product.name());
        CatalogCsvRow variant = mapper.map(row("ADD", "12", "", "", "", "", "", "", "red", "L", "300", "2"));
        assertEquals(12, variant.productId());
    }

    @Test
    void rejectsAmbiguousAndInvalidMutations() {
        assertThrows(IllegalArgumentException.class,
                () -> mapper.map(row("ADD", "12", "", "Ignored", "", "", "", "", "red", "L", "300", "2")));
        assertThrows(IllegalArgumentException.class,
                () -> mapper.map(row("EDIT", "12", "", "", "", "", "", "", "red", "", "", "")));
        assertThrows(IllegalArgumentException.class,
                () -> mapper.map(row("DELETE", "12", "", "Unexpected", "", "", "", "", "", "", "", "")));
        assertThrows(IllegalArgumentException.class,
                () -> mapper.map(row("ADD", "", "", "Shirt", "", "", "Tops", "true", "blue", "M", "-1", "4")));
    }

    @Test
    void handlesSpreadsheetFormulaEscapes() {
        CatalogCsvRow safe = mapper.map(row("ADD", "", "", "'=SUM(1,1)", "", "", "Tops", "true", "blue", "M", "200", "4"));
        assertEquals("=SUM(1,1)", safe.name());
        assertThrows(IllegalArgumentException.class,
                () -> mapper.map(row("ADD", "", "", "=SUM(1,1)", "", "", "Tops", "true", "blue", "M", "200", "4")));
    }

    @Test
    void preservesWhitespaceInTextFieldsAndEscapedFormulas() {
        CatalogCsvRow row = mapper.map(row("ADD", "", "", "  Shirt  ", "  soft cotton  ", "", "Cloth",
                "true", "Blue", "M", "200", "4"));
        assertEquals("  Shirt  ", row.name());
        assertEquals("  soft cotton  ", row.description());

        String original = "  =SUM(1,1)  ";
        String protectedValue = CatalogCsvFormulaProtection.escape(original);
        assertEquals(original, CatalogCsvFormulaProtection.unescape(protectedValue));
        assertThrows(IllegalArgumentException.class, () -> CatalogCsvFormulaProtection.unescape(original));
    }

    private List<String> row(String... values) { return Arrays.asList(values); }
}
