package com.clothingstore.shop.catalog.csv;

import com.clothingstore.shop.dto.repository.users.UserRepositoryDTO;
import com.clothingstore.shop.repository.AuthRepository;
import com.clothingstore.shop.service.JwtService;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;

@Service
public class CatalogCsvService {
    private final JwtService jwt;
    private final AuthRepository auth;
    private final CatalogCsvGateway gateway;
    private final CatalogCsvMutationRepository repository;
    private final CatalogCsvTemplate template;
    private final CatalogCsvCodec codec;
    private final CatalogCsvProperties properties;

    public CatalogCsvService(JwtService jwt, AuthRepository auth, CatalogCsvGateway gateway,
                             CatalogCsvMutationRepository repository, CatalogCsvTemplate template,
                             CatalogCsvCodec codec, CatalogCsvProperties properties) {
        this.jwt = jwt;
        this.auth = auth;
        this.gateway = gateway;
        this.repository = repository;
        this.template = template;
        this.codec = codec;
        this.properties = properties;
    }

    public int requireVendor(String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vendor token is required");
        }
        Integer userId;
        String role;
        try {
            userId = jwt.extractUserId(token);
            role = jwt.extractRole(token);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid vendor token");
        }
        if (userId == null || !"vendor".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vendor access required");
        }
        UserRepositoryDTO user = auth.findByUserIDAndRole(userId, "vendor")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Vendor access required"));
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vendor account is inactive");
        }
        Integer vendorId = auth.getVendorId(userId);
        if (vendorId == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vendor access required");
        return vendorId;
    }

    public byte[] template(int vendorId) {
        return template.bytes();
    }

    public CatalogCsvImportResult importCsv(int vendorId, byte[] content) {
        try {
            return gateway.importCsv(new CatalogCsvImportRequest(vendorId, content));
        } catch (MessagingException exception) {
            Throwable cause = exception;
            while (cause != null) {
                if (cause instanceof IllegalArgumentException invalidCsv) throw invalidCsv;
                cause = cause.getCause();
            }
            throw exception;
        }
    }

    public CatalogCsvExportPage export(int vendorId, int offset) {
        if (properties.getMaxRows() < 1 || properties.getMaxFileBytes() < 1) {
            throw new IllegalStateException("CSV export limits must be positive");
        }
        if (offset < 0 || (long) offset + properties.getMaxRows() + 1 > Integer.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid CSV export offset");
        }
        ByteArrayOutputStream csv = new ByteArrayOutputStream();
        csv.writeBytes(codec.encode(template.columns()).getBytes(StandardCharsets.UTF_8));
        if (csv.size() > properties.getMaxFileBytes()) {
            throw new IllegalStateException("CSV template exceeds the configured file limit");
        }
        List<CatalogCsvRow> rows = repository.export(vendorId, offset, properties.getMaxRows() + 1);
        int written = 0;
        for (CatalogCsvRow row : rows) {
            if (written == properties.getMaxRows()) break;
            byte[] encoded = codec.encode(Arrays.asList(
                    row.operation().name(), value(row.productId()), value(row.variantId()),
                    safe(row.name()), safe(row.description()), safe(row.imageUrl()), safe(row.category()),
                    value(row.listed()), safe(row.color()), safe(row.size()), value(row.price()), value(row.stock())))
                    .getBytes(StandardCharsets.UTF_8);
            if ((long) csv.size() + encoded.length > properties.getMaxFileBytes()) {
                if (written == 0) {
                    throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                            "A product row exceeds the configured CSV file limit");
                }
                break;
            }
            csv.writeBytes(encoded);
            written++;
        }
        Integer nextOffset = written < rows.size() ? offset + written : null;
        return new CatalogCsvExportPage(csv.toByteArray(), nextOffset);
    }

    private String safe(String value) { return CatalogCsvFormulaProtection.escape(value); }
    private String value(Object value) { return value == null ? "" : value.toString(); }
}
