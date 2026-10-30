package com.clothingstore.shop.catalog.csv;

import com.clothingstore.shop.dto.response.ApiResponseDTO;
import com.clothingstore.shop.utils.TokenUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@RestController
@RequestMapping("/api/products/csv")
public class CatalogCsvController {
    private static final MediaType CSV = MediaType.parseMediaType("text/csv;charset=UTF-8");
    private final CatalogCsvService service;
    private final CatalogCsvProperties properties;

    public CatalogCsvController(CatalogCsvService service, CatalogCsvProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> template(HttpServletRequest request) {
        int vendorId = service.requireVendor(TokenUtils.extractTokenFromCookies(request));
        return download(service.template(vendorId), "product-import-template.csv");
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(HttpServletRequest request,
                                         @RequestParam(defaultValue = "0") int offset) {
        int vendorId = service.requireVendor(TokenUtils.extractTokenFromCookies(request));
        CatalogCsvExportPage page = service.export(vendorId, offset);
        ResponseEntity.BodyBuilder response = ResponseEntity.ok().contentType(CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"products.csv\"");
        if (page.nextOffset() != null) {
            response.header("X-Next-Offset", page.nextOffset().toString());
        }
        return response.body(page.content());
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseDTO<CatalogCsvImportResult>> importCsv(
            HttpServletRequest request, @RequestParam("file") MultipartFile file) {
        int vendorId = service.requireVendor(TokenUtils.extractTokenFromCookies(request));
        if (file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV is empty");
        if (file.getSize() > properties.getMaxFileBytes()) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "CSV exceeds the configured file limit");
        }
        try {
            CatalogCsvImportResult result = service.importCsv(vendorId, file.getBytes());
            return ResponseEntity.ok(new ApiResponseDTO<>(result.failed() == 0,
                    "CSV import processed", result));
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read CSV", exception);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    private ResponseEntity<byte[]> download(byte[] content, String filename) {
        return ResponseEntity.ok().contentType(CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(content);
    }
}
