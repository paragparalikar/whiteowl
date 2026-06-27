package com.whiteowl.core.backtest.v2.feature;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
public final class CsvFeatureConsumer implements FeatureConsumer {

    private static final char SEPARATOR = ',';
    private static final String HEADER_PHASE = "phase";
    private static final String HEADER_SCRIP_ID = "scrip_id";
    private static final String HEADER_POSITION_ID = "position_id";
    private static final String HEADER_BAR_INDEX = "bar_index";
    private static final String HEADER_TIMESTAMP = "timestamp";

    private final BufferedWriter writer;
    private boolean headerWritten;

    public CsvFeatureConsumer(Path outputPath) throws IOException {
        Files.createDirectories(outputPath.getParent());
        this.writer = Files.newBufferedWriter(outputPath);
    }

    @Override
    public void accept(FeatureVector vector) {
        try {
            writeHeaderIfNeeded(vector);
            writeRow(vector);
        } catch (IOException e) {
            log.error("Failed to write feature row: {}", e.getMessage(), e);
        }
    }

    @Override
    public void flush() {
        try {
            writer.flush();
        } catch (IOException e) {
            log.error("Failed to flush CSV writer: {}", e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        try {
            writer.close();
        } catch (IOException e) {
            log.error("Failed to close CSV writer: {}", e.getMessage(), e);
        }
    }

    private void writeHeaderIfNeeded(FeatureVector vector) throws IOException {
        if (headerWritten) {
            return;
        }
        StringBuilder header = new StringBuilder();
        header.append(HEADER_PHASE).append(SEPARATOR)
                .append(HEADER_SCRIP_ID).append(SEPARATOR)
                .append(HEADER_POSITION_ID).append(SEPARATOR)
                .append(HEADER_BAR_INDEX).append(SEPARATOR)
                .append(HEADER_TIMESTAMP);
        for (String name : vector.getFeatureNames()) {
            header.append(SEPARATOR).append(name);
        }
        writer.write(header.toString());
        writer.newLine();
        headerWritten = true;
    }

    private void writeRow(FeatureVector vector) throws IOException {
        StringBuilder row = new StringBuilder();
        row.append(vector.getPhase()).append(SEPARATOR)
                .append(vector.getScripId()).append(SEPARATOR)
                .append(vector.getPositionId()).append(SEPARATOR)
                .append(vector.getBarIndex()).append(SEPARATOR)
                .append(vector.getTimestamp());
        List<Float> values = vector.getFeatureValues();
        for (Float value : values) {
            row.append(SEPARATOR).append(value);
        }
        writer.write(row.toString());
        writer.newLine();
    }

}
