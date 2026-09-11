package com.whiteowl.workbench.tools;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.FileBarsRepository;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Standalone tool that imports Nifty/BankNifty intraday CSV data into the WhiteOwl
 * binary bar repository ({@link FileBarsRepository}).
 *
 * <p>Data directory: {@code ~/.whiteowl/nifty-banknifty-intraday-data-main}
 *
 * <p>The tool handles multiple CSV column formats (7, 8, or 9 columns),
 * variable directory structures across years, header-line detection,
 * symbol normalization, timestamp deduplication, and options-data exclusion.
 *
 * <p>Run with:
 * <pre>
 *   mvn -pl whiteowl-workbench exec:java \
 *       -Dexec.mainClass="com.whiteowl.workbench.tools.IntradayDataImporter"
 * </pre>
 */
@Slf4j
public final class IntradayDataImporter {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final Set<String> HEADER_TOKENS = Set.of(
            "ticker", "symbol", "date", "time", "open", "high", "low", "close");

    private static final Map<String, String> SYMBOL_ALIASES = Map.of(
            "^NSEI", "NIFTY",
            "S&P CNX NIFTY (Index)", "NIFTY"
    );

    // ── Entry point ──────────────────────────────────────────────────────

    public static void main(String[] args) throws Exception {
        Path dataDir = Paths.get(
                System.getProperty("user.home") + File.separator + ".whiteowl",
                "nifty-banknifty-intraday-data-main");

        if (!Files.isDirectory(dataDir)) {
            log.error("Data directory not found: {}", dataDir);
            System.exit(1);
        }

        FileBarsRepository repo = new FileBarsRepository();
        new IntradayDataImporter().run(dataDir, repo);
    }

    // ── Orchestrator ─────────────────────────────────────────────────────

    public void run(Path dataDir, FileBarsRepository repo) throws Exception {
        log.info("════════════════════════════════════════════════════════════");
        log.info("  Intraday Data Import — START");
        log.info("  Data dir : {}", dataDir);
        log.info("════════════════════════════════════════════════════════════");

        // Phase 1 — discover .txt files (skip options dirs)
        log.info("[Phase 1] Discovering files …");
        List<Path> allFiles = discoverFiles(dataDir);
        log.info("[Phase 1] Found {} .txt files", allFiles.size());

        // Phase 2 — group files by symbol (read first data line of each)
        log.info("[Phase 2] Grouping files by symbol …");
        Map<String, List<Path>> filesBySymbol = groupBySymbol(allFiles);
        log.info("[Phase 2] Found {} unique symbols across {} files",
                filesBySymbol.size(), allFiles.size());

        // Phase 3 — import symbol by symbol
        log.info("[Phase 3] Importing bars …");
        List<String> sortedSymbols = new ArrayList<>(filesBySymbol.keySet());
        Collections.sort(sortedSymbols);

        int totalSymbols = sortedSymbols.size();
        int processed = 0;
        long totalBars = 0;
        long totalFilesRead = 0;

        for (String symbol : sortedSymbols) {
            List<Path> files = filesBySymbol.get(symbol);
            long bars = importSymbol(symbol, files, repo);
            totalBars += bars;
            totalFilesRead += files.size();
            processed++;

            log.info("[{}/{}] {} — {} bars from {} files  (cumulative: {} bars)",
                    processed, totalSymbols, "NSE:" + symbol, bars, files.size(), totalBars);
        }

        log.info("════════════════════════════════════════════════════════════");
        log.info("  Import COMPLETE");
        log.info("  Symbols imported : {}", totalSymbols);
        log.info("  Total bars       : {}", totalBars);
        log.info("  Files read       : {}", totalFilesRead);
        log.info("════════════════════════════════════════════════════════════");
    }

    // ── Phase 1: file discovery ──────────────────────────────────────────

    private List<Path> discoverFiles(Path dataDir) throws IOException {
        List<Path> result = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(dataDir)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".txt"))
                    .filter(p -> !isOptionsPath(p))
                    .forEach(result::add);
        }
        return result;
    }

    private boolean isOptionsPath(Path path) {
        for (Path component : path) {
            if (component.toString().startsWith("NSE_OPT")) {
                return true;
            }
        }
        return false;
    }

    // ── Phase 2: group by symbol ─────────────────────────────────────────

    private Map<String, List<Path>> groupBySymbol(List<Path> files) {
        Map<String, List<Path>> map = new HashMap<>();
        int skipped = 0;

        for (Path file : files) {
            try {
                String symbol = extractSymbol(file);
                if (symbol == null) {
                    skipped++;
                    continue;
                }
                symbol = normalizeSymbol(symbol);
                map.computeIfAbsent(symbol, k -> new ArrayList<>()).add(file);
            } catch (Exception e) {
                skipped++;
                if (skipped <= 5) {
                    log.warn("Cannot read symbol from {}: {}", file.getFileName(), e.getMessage());
                }
            }
        }

        if (skipped > 0) {
            log.warn("Skipped {} files (unreadable or empty)", skipped);
        }
        return map;
    }

    private String extractSymbol(Path file) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",", 3);
                if (parts.length < 2) continue;
                String first = parts[0].trim();
                if (HEADER_TOKENS.contains(first.toLowerCase())) continue;
                return first;
            }
        }
        return null; // empty file
    }

    private String normalizeSymbol(String raw) {
        String mapped = SYMBOL_ALIASES.get(raw);
        return mapped != null ? mapped : raw;
    }

    // ── Phase 3: per-symbol import ───────────────────────────────────────

    private long importSymbol(String symbol, List<Path> files, FileBarsRepository repo)
            throws IOException {

        String scripId = "NSE:" + symbol;

        // Collect bars in a sorted map (deduplicates by timestamp)
        TreeMap<Long, BarRecord> barMap = new TreeMap<>();

        for (Path file : files) {
            parseFile(file, symbol, barMap);
        }

        if (barMap.isEmpty()) {
            return 0;
        }

        // Build Bars object
        Bars bars = new Bars(scripId, Timeframe.ONE_MINUTE, barMap.size());
        for (Map.Entry<Long, BarRecord> entry : barMap.entrySet()) {
            BarRecord r = entry.getValue();
            bars.append(entry.getKey(), r.open, r.high, r.low, r.close, r.volume);
        }

        // Persist
        repo.save(scripId, Timeframe.ONE_MINUTE, bars);
        return barMap.size();
    }

    // ── CSV parsing ──────────────────────────────────────────────────────

    private void parseFile(Path file, String expectedSymbol, TreeMap<Long, BarRecord> barMap) {
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(",");
                if (parts.length < 7) continue;

                String rawSymbol = parts[0].trim();
                if (HEADER_TOKENS.contains(rawSymbol.toLowerCase())) continue;

                String symbol = normalizeSymbol(rawSymbol);
                if (!symbol.equals(expectedSymbol)) continue;

                try {
                    String dateStr = parts[1].trim();
                    String timeStr = parts[2].trim();
                    float open = Float.parseFloat(parts[3].trim());
                    float high = Float.parseFloat(parts[4].trim());
                    float low = Float.parseFloat(parts[5].trim());
                    float close = Float.parseFloat(parts[6].trim());

                    long volume = 0;
                    if (parts.length >= 8) {
                        String volStr = parts[7].trim();
                        if (!volStr.isEmpty()) {
                            volume = Long.parseLong(volStr);
                        }
                    }

                    long ts = toEpochMillis(dateStr, timeStr);
                    barMap.put(ts, new BarRecord(open, high, low, close, volume));
                } catch (NumberFormatException | java.time.DateTimeException e) {
                    // silently skip malformed lines
                }
            }
        } catch (IOException e) {
            log.warn("Error reading {}: {}", file, e.getMessage());
        }
    }

    private long toEpochMillis(String dateStr, String timeStr) {
        LocalDate date = LocalDate.parse(dateStr, DATE_FMT);
        String[] tp = timeStr.split(":");
        LocalTime time = LocalTime.of(Integer.parseInt(tp[0]), Integer.parseInt(tp[1]));
        return LocalDateTime.of(date, time).atZone(IST).toInstant().toEpochMilli();
    }

    // ── Tiny record for OHLCV ────────────────────────────────────────────

    private static final class BarRecord {
        final float open, high, low, close;
        final long volume;

        BarRecord(float open, float high, float low, float close, long volume) {
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
            this.volume = volume;
        }
    }
}
