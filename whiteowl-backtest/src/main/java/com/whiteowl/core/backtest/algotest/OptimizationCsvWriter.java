package com.whiteowl.core.backtest.algotest;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedSet;

/**
 * Writes optimization rows to CSV. Columns are the union of all row keys in
 * first-seen order so that any input variable or result field is preserved.
 */
public final class OptimizationCsvWriter {

    private OptimizationCsvWriter() {
    }

    /**
     * Incremental writer: columns are fixed by the first row written, and each
     * subsequent row is appended immediately — so partial results survive a
     * crash or interruption mid-sweep.
     */
    public static class RowWriter implements Closeable {
        private final BufferedWriter writer;
        private List<String> columns;

        public RowWriter(Path path) throws IOException {
            this(path, new OpenOption[0]);
        }

        private RowWriter(Path path, OpenOption... options) throws IOException {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            this.writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, options);
        }

        /**
         * Append mode for restarts: rows go under the file's existing header
         * ({@code existingColumns}) rather than rewriting it. Keys not in the
         * header are dropped, so the resumed run must emit the same schema.
         */
        public static RowWriter append(Path path, List<String> existingColumns) throws IOException {
            RowWriter w = new RowWriter(path,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            w.columns = List.copyOf(existingColumns);
            return w;
        }

        public synchronized void writeRow(LinkedHashMap<String, String> row) {
            try {
                if (columns == null) {
                    columns = List.copyOf(row.keySet());
                    writer.write(csvLine(columns));
                    writer.newLine();
                }
                writer.write(csvLine(columns.stream().map(row::get).toList()));
                writer.newLine();
                writer.flush();
            } catch (IOException e) {
                throw new UncheckedIOException("Failed writing CSV row", e);
            }
        }

        @Override
        public void close() throws IOException {
            writer.close();
        }
    }

    /**
     * Reads a CSV written by this class back into rows keyed by the header.
     * Handles quoted cells containing commas, quotes and newlines.
     */
    public static List<LinkedHashMap<String, String>> read(Path path) throws IOException {
        List<List<String>> records = parse(Files.readString(path, StandardCharsets.UTF_8));
        if (records.isEmpty()) {
            return List.of();
        }
        List<String> header = records.get(0);
        List<LinkedHashMap<String, String>> rows = new ArrayList<>(records.size() - 1);
        for (int i = 1; i < records.size(); i++) {
            List<String> cells = records.get(i);
            LinkedHashMap<String, String> row = new LinkedHashMap<>();
            for (int c = 0; c < header.size(); c++) {
                row.put(header.get(c), c < cells.size() ? cells.get(c) : "");
            }
            rows.add(row);
        }
        return rows;
    }

    private static List<List<String>> parse(String csv) {
        List<List<String>> records = new ArrayList<>();
        List<String> record = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < csv.length(); i++) {
            char ch = csv.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < csv.length() && csv.charAt(i + 1) == '"') {
                        cell.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cell.append(ch);
                }
            } else if (ch == '"') {
                inQuotes = true;
            } else if (ch == ',') {
                record.add(cell.toString());
                cell.setLength(0);
            } else if (ch == '\n' || ch == '\r') {
                if (ch == '\r' && i + 1 < csv.length() && csv.charAt(i + 1) == '\n') {
                    i++;
                }
                record.add(cell.toString());
                cell.setLength(0);
                records.add(record);
                record = new ArrayList<>();
            } else {
                cell.append(ch);
            }
        }
        if (cell.length() > 0 || !record.isEmpty()) {
            record.add(cell.toString());
            records.add(record);
        }
        return records;
    }

    public static void write(Path path, List<LinkedHashMap<String, String>> rows) throws IOException {
        SequencedSet<String> columns = new LinkedHashSet<>();
        for (LinkedHashMap<String, String> row : rows) {
            columns.addAll(row.keySet());
        }
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write(csvLine(columns.stream().toList()));
            writer.newLine();
            for (LinkedHashMap<String, String> row : rows) {
                writer.write(csvLine(columns.stream().map(row::get).toList()));
                writer.newLine();
            }
        }
    }

    private static String csvLine(List<String> cells) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(cells.get(i)));
        }
        return sb.toString();
    }

    private static String escape(String cell) {
        if (cell == null) {
            return "";
        }
        if (cell.contains(",") || cell.contains("\"") || cell.contains("\n") || cell.contains("\r")) {
            return '"' + cell.replace("\"", "\"\"") + '"';
        }
        return cell;
    }
}
