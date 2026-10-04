package com.whiteowl.core.backtest.algotest;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            this.writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
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
