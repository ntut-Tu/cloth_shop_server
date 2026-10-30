package com.clothingstore.shop.catalog.csv;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Parses RFC 4180 style quoted fields, including escaped quotes and embedded newlines. */
@Component
public class CatalogCsvCodec {
    public List<List<String>> parse(String csv, int maxRows) {
        if (csv == null || csv.isEmpty()) {
            throw new IllegalArgumentException("CSV is empty");
        }
        if (csv.charAt(0) == '\uFEFF') {
            csv = csv.substring(1);
        }
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        boolean afterQuote = false;
        boolean atStart = true;
        for (int i = 0; i < csv.length(); i++) {
            char ch = csv.charAt(i);
            if (quoted) {
                if (ch == '"') {
                    if (i + 1 < csv.length() && csv.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        quoted = false;
                        afterQuote = true;
                    }
                } else {
                    field.append(ch);
                }
            } else if (ch == '"') {
                if (!atStart || afterQuote) {
                    throw new IllegalArgumentException("Unexpected quote in CSV field");
                }
                quoted = true;
                atStart = false;
            } else if (ch == ',') {
                row.add(field.toString());
                field.setLength(0);
                atStart = true;
                afterQuote = false;
            } else if (ch == '\n' || ch == '\r') {
                row.add(field.toString());
                addRow(rows, row, maxRows);
                row = new ArrayList<>();
                field.setLength(0);
                atStart = true;
                afterQuote = false;
                if (ch == '\r' && i + 1 < csv.length() && csv.charAt(i + 1) == '\n') {
                    i++;
                }
            } else {
                if (afterQuote) {
                    throw new IllegalArgumentException("Unexpected data after quoted CSV field");
                }
                field.append(ch);
                atStart = false;
            }
        }
        if (quoted) {
            throw new IllegalArgumentException("Unclosed quoted CSV field");
        }
        if (!row.isEmpty() || field.length() > 0 || !atStart) {
            row.add(field.toString());
            addRow(rows, row, maxRows);
        }
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("CSV is empty");
        }
        return rows;
    }

    private void addRow(List<List<String>> rows, List<String> row, int maxRows) {
        if (rows.size() >= maxRows) {
            throw new IllegalArgumentException("CSV exceeds the configured row limit");
        }
        rows.add(List.copyOf(row));
    }

    public String encode(List<String> values) {
        List<String> fields = new ArrayList<>(values.size());
        for (String value : values) {
            String safe = value == null ? "" : value;
            if (safe.contains(",") || safe.contains("\n") || safe.contains("\r") || safe.contains("\"")) {
                safe = "\"" + safe.replace("\"", "\"\"") + "\"";
            }
            fields.add(safe);
        }
        return String.join(",", fields) + "\r\n";
    }
}
