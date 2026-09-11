package com.whiteowl.workbench.tools;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.FileBarsRepository;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Imports well-structured Nifty CSV data from {@code ~/.whiteowl/download/nifty data}
 * into the WhiteOwl binary bar repository.
 *
 * <p>File naming convention: {@code {SYMBOL}_{timeframe}.csv}
 * <br>Timeframes: {@code minute, 5minute, 15minute, 60minute, day}
 * <br>CSV header: {@code date,open,high,low,close,volume}
 * <br>Date format: {@code yyyy-MM-dd HH:mm:ss}
 *
 * <p>Deduplication: uses repository's built-in duplicate-skipping in
 * {@code append} and {@code prepend} to merge with any pre-existing data.
 *
 * <p>Run with:
 * <pre>
 *   mvn -pl whiteowl-workbench exec:java \
 *       -Dexec.mainClass="com.whiteowl.workbench.tools.NiftyDownloadImporter"
 * </pre>
 */
@Slf4j
public final class NiftyDownloadImporter {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String EXCHANGE_PREFIX = "NSE:";

    private static final Map<String, Timeframe> TIMEFRAME_MAP = Map.of(
            "minute", Timeframe.ONE_MINUTE,
            "5minute", Timeframe.FIVE_MINUTE,
            "15minute", Timeframe.FIFTEEN_MINUTE,
            "60minute", Timeframe.ONE_HOUR,
            "day", Timeframe.DAILY
    );

    // ── Entry point ──────────────────────────────────────────────────────

    public static void main(String[] args) throws Exception {
        Path dataDir = Paths.get(
                System.getProperty("user.home") + File.separator + ".whiteowl",
                "download", "nifty data");

        if (!Files.isDirectory(dataDir)) {
            log.error("Data directory not found: {}", dataDir);
            System.exit(1);
        }

        FileBarsRepository repo = new FileBarsRepository();
        new NiftyDownloadImporter().run(dataDir, repo);
    }

    // ── Orchestrator ─────────────────────────────────────────────────────

    public void run(Path dataDir, FileBarsRepository repo) throws Exception {
        log.info("════════════════════════════════════════════════════════════");
        log.info("  Nifty Download Import — START");
        log.info("  Data dir : {}", dataDir);
        log.info("════════════════════════════════════════════════════════════");

        // Discover CSV files
        List<Path> csvFiles = discoverCsvFiles(dataDir);
        log.info("Found {} CSV files", csvFiles.size());

        // Parse filenames into (symbol, timeframe) groups
        Map<String, List<FileEntry>> grouped = groupFiles(csvFiles);
        log.info("Identified {} unique symbols", grouped.size());

        // Import
        int totalFiles = csvFiles.size();
        int filesProcessed = 0;
        long totalBarsNew = 0;

        List<String> sortedSymbols = new ArrayList<>(grouped.keySet());
        Collections.sort(sortedSymbols);

        for (String symbol : sortedSymbols) {
            List<FileEntry> entries = grouped.get(symbol);
            for (FileEntry entry : entries) {
                long newBars = importFile(entry, repo);
                totalBarsNew += newBars;
                filesProcessed++;

                if (filesProcessed % 25 == 0 || filesProcessed == totalFiles) {
                    log.info("[{}/{}] {} {} — {} new bars  (cumulative: {} new bars)",
                            filesProcessed, totalFiles,
                            entry.scripId, entry.timeframe.getLabel(),
                            newBars, totalBarsNew);
                }
            }
        }

        log.info("════════════════════════════════════════════════════════════");
        log.info("  Import COMPLETE");
        log.info("  Files processed  : {}", filesProcessed);
        log.info("  Total new bars   : {}", totalBarsNew);
        log.info("════════════════════════════════════════════════════════════");
    }

    // ── File discovery ───────────────────────────────────────────────────

    private List<Path> discoverCsvFiles(Path dir) throws IOException {
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.csv")) {
            for (Path p : stream) {
                if (Files.isRegularFile(p)) files.add(p);
            }
        }
        Collections.sort(files);
        return files;
    }

    // ── Filename parsing ─────────────────────────────────────────────────

    private Map<String, List<FileEntry>> groupFiles(List<Path> files) {
        Map<String, List<FileEntry>> map = new LinkedHashMap<>();
        int skipped = 0;

        for (Path file : files) {
            String name = file.getFileName().toString();
            // Strip .csv
            String base = name.substring(0, name.length() - 4);

            // Find last underscore
            int lastUnderscore = base.lastIndexOf('_');
            if (lastUnderscore < 0) {
                skipped++;
                continue;
            }

            String symbol = base.substring(0, lastUnderscore);
            String tfKey = base.substring(lastUnderscore + 1);
            Timeframe tf = TIMEFRAME_MAP.get(tfKey);

            if (tf == null) {
                log.warn("Unknown timeframe '{}' in file: {}", tfKey, name);
                skipped++;
                continue;
            }

            String scripId = EXCHANGE_PREFIX + symbol;
            FileEntry entry = new FileEntry(file, scripId, tf);
            map.computeIfAbsent(symbol, k -> new ArrayList<>()).add(entry);
        }

        if (skipped > 0) {
            log.warn("Skipped {} files with unrecognized naming", skipped);
        }
        return map;
    }

    // ── Per-file import ──────────────────────────────────────────────────

    private long importFile(FileEntry entry, FileBarsRepository repo) throws IOException {
        Bars bars = parseCsv(entry.path, entry.scripId, entry.timeframe);
        if (bars.size() == 0) return 0;

        boolean exists = repo.exists(entry.scripId, entry.timeframe);

        if (!exists) {
            repo.save(entry.scripId, entry.timeframe, bars);
            return bars.size();
        }

        // Merge with existing data: prepend older bars, append newer bars.
        // Repository methods internally skip duplicates.
        int before = repo.countBars(entry.scripId, entry.timeframe);

        repo.prepend(entry.scripId, entry.timeframe, bars, 0, bars.size());
        repo.append(entry.scripId, entry.timeframe, bars, 0, bars.size());

        int after = repo.countBars(entry.scripId, entry.timeframe);
        return after - before;
    }

    // ── CSV parsing ──────────────────────────────────────────────────────

    private Bars parseCsv(Path file, String scripId, Timeframe timeframe) throws IOException {
        List<long[]> timestamps = new ArrayList<>();
        List<float[]> ohlc = new ArrayList<>();
        List<Long> volumes = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            boolean headerSeen = false;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Skip header
                if (!headerSeen && line.startsWith("date,")) {
                    headerSeen = true;
                    continue;
                }

                String[] parts = line.split(",", 7);
                if (parts.length < 6) continue;

                try {
                    String dateStr = parts[0].trim();
                    float open = Float.parseFloat(parts[1].trim());
                    float high = Float.parseFloat(parts[2].trim());
                    float low = Float.parseFloat(parts[3].trim());
                    float close = Float.parseFloat(parts[4].trim());
                    long volume = Long.parseLong(parts[5].trim());

                    long ts = LocalDateTime.parse(dateStr, TS_FMT)
                            .atZone(IST)
                            .toInstant()
                            .toEpochMilli();

                    timestamps.add(new long[]{ts});
                    ohlc.add(new float[]{open, high, low, close});
                    volumes.add(volume);
                } catch (Exception e) {
                    // skip malformed line
                }
            }
        }

        Bars bars = new Bars(scripId, timeframe, timestamps.size());
        for (int i = 0; i < timestamps.size(); i++) {
            float[] v = ohlc.get(i);
            bars.append(timestamps.get(i)[0], v[0], v[1], v[2], v[3], volumes.get(i));
        }
        return bars;
    }

    // ── Inner types ──────────────────────────────────────────────────────

    private static final class FileEntry {
        final Path path;
        final String scripId;
        final Timeframe timeframe;

        FileEntry(Path path, String scripId, Timeframe timeframe) {
            this.path = path;
            this.scripId = scripId;
            this.timeframe = timeframe;
        }
    }
}
