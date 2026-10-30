package com.clothingstore.shop.catalog.csv;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CatalogCsvControllerTest {
    @Test
    void exportExposesContinuationOffsetForNextImportablePage() {
        CatalogCsvService service = mock(CatalogCsvService.class);
        CatalogCsvProperties properties = new CatalogCsvProperties();
        byte[] content = "header\r\nrow\r\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        when(service.requireVendor("jwt")).thenReturn(7);
        when(service.export(7, 0)).thenReturn(new CatalogCsvExportPage(content, 2));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("token", "jwt"));

        var response = new CatalogCsvController(service, properties).export(request, 0);

        assertEquals("2", response.getHeaders().getFirst("X-Next-Offset"));
        assertArrayEquals(content, response.getBody());
    }
}
