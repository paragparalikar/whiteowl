package com.whiteowl.workbench.surface;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * In-memory representation of an arbitrary CSV file. The first record is the
 * header row; each column is detected as numeric or categorical purely from
 * its cell contents. No assumptions are made about column names or layout.
 */
public final class CsvData {

    private final List<String> headers;
    private final List<String[]> rows;
    private final boolean[] numeric;

    private CsvData(List<String> headers, List<String[]> rows, boolean[] numeric) {
        this.headers = headers;
        this.rows = rows;
        this.numeric = numeric;
    }

    public static CsvData load(Path path) throws IOException {
        String text = Files.readString(path, StandardCharsets.UTF_8);
        List<List<String>> records = parse(text);
        if (records.isEmpty()) {
            throw new IOException("CSV file is empty: " + path);
        }
        List<String> headers = records.get(0);
        int width = headers.size();
        List<String[]> rows = new ArrayList<>();
        for (int i = 1; i < records.size(); i++) {
            List<String> record = records.get(i);
            if (isBlank(record)) {
                continue;
            }
            String[] row = new String[width];
            for (int c = 0; c < width; c++) {
                row[c] = c < record.size() ? record.get(c) : "";
            }
            rows.add(row);
        }
        boolean[] numeric = detectNumeric(headers, rows);
        return new CsvData(headers, rows, numeric);
    }

    public List<String> getHeaders() {
        return headers;
    }

    public int columnIndex(String name) {
        return headers.indexOf(name);
    }

    public int rowCount() {
        return rows.size();
    }

    public int columnCount() {
        return headers.size();
    }

    public String cell(int rowIndex, int columnIndex) {
        return rows.get(rowIndex)[columnIndex];
    }

    public boolean isNumeric(int columnIndex) {
        return numeric[columnIndex];
    }

    public double numericValue(int rowIndex, int columnIndex) {
        return parseNumber(cell(rowIndex, columnIndex));
    }

    public static double parseNumber(String value) {
        if (value == null) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    /**
     * Sorted distinct non-blank values of a column, used to offer value choices
     * for categorical and boolean filters.
     */
    public List<String> distinctValues(int columnIndex) {
        Set<String> values = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (String[] row : rows) {
            String cell = row[columnIndex];
            if (!cell.isBlank()) {
                values.add(cell);
            }
        }
        return List.copyOf(values);
    }

    private static boolean[] detectNumeric(List<String> headers, List<String[]> rows) {
        boolean[] numeric = new boolean[headers.size()];
        for (int c = 0; c < headers.size(); c++) {
            boolean allNumeric = false;
            for (String[] row : rows) {
                String cell = row[c];
                if (cell == null || cell.isBlank()) {
                    continue;
                }
                if (Double.isNaN(parseNumber(cell))) {
                    allNumeric = false;
                    break;
                }
                allNumeric = true;
            }
            numeric[c] = allNumeric;
        }
        return numeric;
    }

    private static boolean isBlank(List<String> record) {
        for (String cell : record) {
            if (!cell.isBlank()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Minimal RFC-4180 style parser supporting quoted cells, escaped quotes and
     * newlines inside quoted values.
     */
    private static List<List<String>> parse(String text) {
        List<List<String>> records = new ArrayList<>();
        List<String> record = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(ch);
                }
            } else if (ch == '"') {
                inQuotes = true;
            } else if (ch == ',') {
                record.add(field.toString());
                field.setLength(0);
            } else if (ch == '\n' || ch == '\r') {
                if (ch == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                record.add(field.toString());
                field.setLength(0);
                records.add(record);
                record = new ArrayList<>();
            } else {
                field.append(ch);
            }
        }
        if (field.length() > 0 || !record.isEmpty()) {
            record.add(field.toString());
            records.add(record);
        }
        return records;
    }

}
