package com.whiteowl.workbench.tools;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.FileBarsRepository;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * One-shot importer that loads <b>both</b> offline Nifty/BankNifty datasets into
 * the WhiteOwl binary bar repository, merges them with any pre-existing data
 * (e.g. Kite-downloaded bars), and runs a validation pass at the end.
 *
 * <h3>Data sources (in import order)</h3>
 * <ol>
 *   <li><b>Intraday .txt dataset</b> — {@code ~/.whiteowl/nifty-banknifty-intraday-data-main}
 *       <br>2008–2023, 1-minute bars, nested year/month/day directories, CSV columns:
 *       Symbol,Date,Time,Open,High,Low,Close[,Volume[,OI]]</li>
 *   <li><b>Nifty download CSVs</b> — {@code ~/.whiteowl/download/nifty data}
 *       <br>2015–2026, multi-timeframe, flat directory, filename:
 *       {@code {SYMBOL}_{timeframe}.csv}, header: date,open,high,low,close,volume</li>
 * </ol>
 *
 * <h3>What this tool handles</h3>
 * <ul>
 *   <li>Maps legacy CSV symbols to <b>Kite-recognized</b> names
 *       (e.g. NIFTY→NIFTY 50, BANKNIFTY→NIFTY BANK, CNX-IT→NIFTY IT)</li>
 *   <li>Deduplicates bars by timestamp (TreeMap within a source, prepend/append across sources)</li>
 *   <li>Filters out non-positive prices, H&lt;L violations, and weekend bars at parse time</li>
 *   <li>Runs a full verification scan at the end to confirm zero issues remain</li>
 * </ul>
 *
 * <p>Run with:
 * <pre>
 *   mvn -pl whiteowl-workbench exec:java \
 *       -Dexec.mainClass="com.whiteowl.workbench.tools.UnifiedDataImporter"
 * </pre>
 */
@Slf4j
public final class UnifiedDataImporter {

    // ── Constants ────────────────────────────────────────────────────────

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final String EXCHANGE = "NSE";
    private static final String SCRIP_PREFIX = EXCHANGE + ":";

    private static final DateTimeFormatter TXT_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter CSV_TS_FMT   = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Set<String> HEADER_TOKENS = Set.of(
            "ticker", "symbol", "date", "time", "open", "high", "low", "close");

    /** Raw CSV symbol → Kite-recognized symbol. */
    private static final Map<String, String> SYMBOL_MAP = buildSymbolMap();

    /** Filename timeframe suffix → Timeframe enum (for nifty download CSVs). */
    private static final Map<String, Timeframe> TF_SUFFIX_MAP = Map.of(
            "minute",   Timeframe.ONE_MINUTE,
            "5minute",  Timeframe.FIVE_MINUTE,
            "15minute", Timeframe.FIFTEEN_MINUTE,
            "60minute", Timeframe.ONE_HOUR,
            "day",      Timeframe.DAILY);

    /** Timeframe.label → Timeframe (for the validation pass). */
    private static final Map<String, Timeframe> LABEL_TO_TF = buildLabelMap();

    private static final int BIN_HEADER = 16;
    private static final int BIN_BAR    = 32;

    // ── Symbol mapping ───────────────────────────────────────────────────

    private static Map<String, String> buildSymbolMap() {
        Map<String, String> m = new HashMap<>();
        // Aliases found in the .txt dataset
        m.put("^NSEI",                   "NIFTY 50");
        m.put("S&P CNX NIFTY (Index)",   "NIFTY 50");
        // Index renames (old short name → Kite name)
        m.put("NIFTY",                   "NIFTY 50");
        m.put("NIFTY-I",                 "NIFTY 50");
        m.put("BANKNIFTY",               "NIFTY BANK");
        m.put("BANKNIFTY-I",             "NIFTY BANK");
        m.put("INDIAVIX",                "INDIA VIX");
        // CNX → NIFTY rebrand
        m.put("CNX-IT",                  "NIFTY IT");
        m.put("CNXIT",                   "NIFTY IT");
        m.put("CNX-MIDCAP",              "NIFTY MIDCAP 50");
        m.put("CNX-NIFTY-JUNIOR",        "NIFTY NEXT 50");
        m.put("CNX100",                  "NIFTY 100");
        m.put("CNX500",                  "NIFTY 500");
        m.put("CNXENERGY",               "NIFTY ENERGY");
        m.put("CNXFMCG",                 "NIFTY FMCG");
        // Spacing / formatting
        m.put("NIFTY-MIDCAP50",          "NIFTY MIDCAP 50");
        m.put("NIFTYAUTO",               "NIFTY AUTO");
        m.put("NIFTYFINSERVICE",          "NIFTY FIN SERVICE");
        m.put("NIFTYINFRA",              "NIFTY INFRA");
        m.put("NIFTYMETAL",              "NIFTY METAL");
        return Collections.unmodifiableMap(m);
    }

    private static Map<String, Timeframe> buildLabelMap() {
        Map<String, Timeframe> m = new LinkedHashMap<>();
        for (Timeframe tf : Timeframe.values()) m.put(tf.getLabel(), tf);
        return m;
    }

    /** Resolve a raw symbol to its Kite-recognized name (identity if not mapped). */
    private static String kiteSymbol(String raw) {
        return SYMBOL_MAP.getOrDefault(raw, raw);
    }

    // ── Entry point ──────────────────────────────────────────────────────

    public static void main(String[] args) throws Exception {
        Path whiteowlHome = Paths.get(System.getProperty("whiteowl.home",
                System.getProperty("user.home") + File.separator + ".whiteowl"));

        Path intradayDir   = whiteowlHome.resolve("nifty-banknifty-intraday-data-main");
        Path niftyDlDir    = whiteowlHome.resolve("download").resolve("nifty data");

        FileBarsRepository repo = new FileBarsRepository();
        new UnifiedDataImporter().run(intradayDir, niftyDlDir, repo);
    }

    // ── Orchestrator ─────────────────────────────────────────────────────

    public void run(Path intradayDir, Path niftyDlDir, FileBarsRepository repo) throws Exception {
        log.info("════════════════════════════════════════════════════════════");
        log.info("  Unified Data Importer — START");
        log.info("════════════════════════════════════════════════════════════");

        // Phase 1 — Intraday .txt files (2008–2023, 1-minute only)
        if (Files.isDirectory(intradayDir)) {
            importIntradayTxtFiles(intradayDir, repo);
        } else {
            log.warn("Phase 1 SKIPPED — directory not found: {}", intradayDir);
        }

        // Phase 2 — Nifty download CSVs (2015–2026, multi-timeframe)
        if (Files.isDirectory(niftyDlDir)) {
            importNiftyDownloadCsvs(niftyDlDir, repo);
        } else {
            log.warn("Phase 2 SKIPPED — directory not found: {}", niftyDlDir);
        }

        // Phase 3 — Build higher timeframes from 1min data
        new TimeframeAggregator().run(repo);

        // Phase 4 — Validation & fix
        Path barsBase = Paths.get(System.getProperty("whiteowl.home",
                System.getProperty("user.home") + File.separator + ".whiteowl"),
                "data", "bars");
        validateAndFix(barsBase, repo);

        log.info("════════════════════════════════════════════════════════════");
        log.info("  ALL DONE");
        log.info("════════════════════════════════════════════════════════════");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PHASE 1 — Intraday .txt files
    // ═══════════════════════════════════════════════════════════════════════

    private void importIntradayTxtFiles(Path dataDir, FileBarsRepository repo) throws Exception {
        log.info("────────────────────────────────────────────────────────────");
        log.info("Phase 1 — Importing intraday .txt files from {}", dataDir);
        log.info("────────────────────────────────────────────────────────────");

        // 1a. Discover .txt files, skip options directories
        List<Path> txtFiles;
        try (Stream<Path> walk = Files.walk(dataDir)) {
            txtFiles = walk
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".txt"))
                    .filter(p -> {
                        for (Path c : p) { if (c.toString().startsWith("NSE_OPT")) return false; }
                        return true;
                    })
                    .collect(java.util.stream.Collectors.toList());
        }
        log.info("  Found {} .txt files (options excluded)", txtFiles.size());

        // 1b. Group files by Kite-symbol (read first data line of each file)
        Map<String, List<Path>> bySymbol = new HashMap<>();
        int skipped = 0;
        for (Path f : txtFiles) {
            String raw = readFirstSymbol(f);
            if (raw == null) { skipped++; continue; }
            String symbol = kiteSymbol(raw);
            bySymbol.computeIfAbsent(symbol, k -> new ArrayList<>()).add(f);
        }
        if (skipped > 0) log.warn("  Skipped {} unreadable files", skipped);
        log.info("  Grouped into {} symbols", bySymbol.size());

        // 1c. Import symbol by symbol
        List<String> symbols = new ArrayList<>(bySymbol.keySet());
        Collections.sort(symbols);
        int total = symbols.size();
        int done = 0;
        long cumBars = 0;

        for (String symbol : symbols) {
            List<Path> files = bySymbol.get(symbol);
            String scripId = SCRIP_PREFIX + symbol;
            Timeframe tf = Timeframe.ONE_MINUTE;

            // Collect into TreeMap (sorted + dedup)
            TreeMap<Long, float[]> bars = new TreeMap<>();
            for (Path f : files) parseTxtFile(f, symbol, bars);

            if (bars.isEmpty()) { done++; continue; }

            // Build Bars and merge into repo
            Bars b = toBars(scripId, tf, bars);
            mergeIntoRepo(repo, scripId, tf, b);
            cumBars += b.size();
            done++;

            if (done % 50 == 0 || done == total) {
                log.info("  Phase 1 [{}/{}] {} — {} bars  (cumulative {})",
                        done, total, scripId, b.size(), cumBars);
            }
        }
        log.info("Phase 1 DONE — {} symbols, {} bars from {} files", total, cumBars, txtFiles.size());
    }

    /** Read the first non-header, non-empty line and return the symbol field. */
    private String readFirstSymbol(Path file) {
        try (BufferedReader r = Files.newBufferedReader(file)) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String first = line.split(",", 3)[0].trim();
                if (HEADER_TOKENS.contains(first.toLowerCase())) continue;
                return first;
            }
        } catch (IOException ignored) {}
        return null;
    }

    /** Parse a .txt CSV file (Symbol,Date,Time,O,H,L,C[,V[,OI]]) into the bar map. */
    private void parseTxtFile(Path file, String expectedSymbol, TreeMap<Long, float[]> bars) {
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] p = line.split(",");
                if (p.length < 7) continue;

                String raw = p[0].trim();
                if (HEADER_TOKENS.contains(raw.toLowerCase())) continue;
                if (!kiteSymbol(raw).equals(expectedSymbol)) continue;

                try {
                    LocalDate date = LocalDate.parse(p[1].trim(), TXT_DATE_FMT);
                    String[] tp = p[2].trim().split(":");
                    LocalTime time = LocalTime.of(Integer.parseInt(tp[0]), Integer.parseInt(tp[1]));
                    long ts = LocalDateTime.of(date, time).atZone(IST).toInstant().toEpochMilli();

                    float o = Float.parseFloat(p[3].trim());
                    float h = Float.parseFloat(p[4].trim());
                    float l = Float.parseFloat(p[5].trim());
                    float c = Float.parseFloat(p[6].trim());
                    long  v = p.length >= 8 && !p[7].trim().isEmpty() ? Long.parseLong(p[7].trim()) : 0;

                    if (!isValidBar(ts, o, h, l, c, true)) continue;
                    bars.put(ts, new float[]{o, h, l, c, v});
                } catch (Exception ignored) {}
            }
        } catch (IOException e) {
            log.warn("  Error reading {}: {}", file.getFileName(), e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PHASE 2 — Nifty download CSVs
    // ═══════════════════════════════════════════════════════════════════════

    private void importNiftyDownloadCsvs(Path dataDir, FileBarsRepository repo) throws Exception {
        log.info("────────────────────────────────────────────────────────────");
        log.info("Phase 2 — Importing nifty download CSVs from {}", dataDir);
        log.info("────────────────────────────────────────────────────────────");

        List<Path> csvFiles = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dataDir, "*.csv")) {
            for (Path p : ds) if (Files.isRegularFile(p)) csvFiles.add(p);
        }
        Collections.sort(csvFiles);
        log.info("  Found {} CSV files", csvFiles.size());

        int total = csvFiles.size();
        int done = 0;
        long cumBars = 0;

        for (Path file : csvFiles) {
            String name = file.getFileName().toString();
            String base = name.substring(0, name.length() - 4); // strip .csv
            int lastUs = base.lastIndexOf('_');
            if (lastUs < 0) { done++; continue; }

            String symbol = base.substring(0, lastUs);
            String tfKey  = base.substring(lastUs + 1);
            Timeframe tf  = TF_SUFFIX_MAP.get(tfKey);
            if (tf == null) { done++; continue; }

            String scripId = SCRIP_PREFIX + symbol;
            boolean isIntraday = tf != Timeframe.DAILY && tf != Timeframe.WEEKLY && tf != Timeframe.MONTHLY;

            TreeMap<Long, float[]> bars = new TreeMap<>();
            try (BufferedReader reader = Files.newBufferedReader(file)) {
                String line;
                boolean headerSeen = false;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    if (!headerSeen && line.startsWith("date,")) { headerSeen = true; continue; }

                    String[] p = line.split(",", 7);
                    if (p.length < 6) continue;
                    try {
                        long  ts = LocalDateTime.parse(p[0].trim(), CSV_TS_FMT)
                                .atZone(IST).toInstant().toEpochMilli();
                        float o  = Float.parseFloat(p[1].trim());
                        float h  = Float.parseFloat(p[2].trim());
                        float l  = Float.parseFloat(p[3].trim());
                        float c  = Float.parseFloat(p[4].trim());
                        long  v  = Long.parseLong(p[5].trim());

                        if (!isValidBar(ts, o, h, l, c, isIntraday)) continue;
                        bars.put(ts, new float[]{o, h, l, c, v});
                    } catch (Exception ignored) {}
                }
            }

            if (bars.isEmpty()) { done++; continue; }

            Bars b = toBars(scripId, tf, bars);
            mergeIntoRepo(repo, scripId, tf, b);
            cumBars += b.size();
            done++;

            if (done % 25 == 0 || done == total) {
                log.info("  Phase 2 [{}/{}] {} {} — {} bars  (cumulative {})",
                        done, total, scripId, tf.getLabel(), b.size(), cumBars);
            }
        }
        log.info("Phase 2 DONE — {} files, {} bars", total, cumBars);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PHASE 3 — Validation & fix
    // ═══════════════════════════════════════════════════════════════════════

    private void validateAndFix(Path barsBase, FileBarsRepository repo) throws Exception {
        log.info("────────────────────────────────────────────────────────────");
        log.info("Phase 3 — Validation pass");
        log.info("────────────────────────────────────────────────────────────");

        if (!Files.isDirectory(barsBase)) {
            log.warn("  Bars directory not found: {}", barsBase);
            return;
        }

        // Discover all .bin files
        List<BinFile> binFiles = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(barsBase, 3)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".bin"))
                    .forEach(p -> {
                        String label = p.getFileName().toString().replace(".bin", "");
                        Timeframe tf = LABEL_TO_TF.get(label);
                        if (tf == null) return;
                        Path symDir = p.getParent();
                        Path exDir  = symDir.getParent();
                        if (exDir == null || !exDir.getParent().equals(barsBase)) return;
                        String scripId = exDir.getFileName() + ":" + symDir.getFileName();
                        binFiles.add(new BinFile(scripId, tf, p));
                    });
        }
        binFiles.sort((a, b) -> {
            int c = a.scripId.compareTo(b.scripId);
            return c != 0 ? c : a.tf.compareTo(b.tf);
        });

        int scanned = 0, fixed = 0;
        long dupes = 0, hlBad = 0, npBad = 0, wknd = 0, totalBefore = 0, totalAfter = 0;

        for (BinFile bf : binFiles) {
            scanned++;
            Bars bars = loadNoMmap(bf);
            int n = bars.size();
            totalBefore += n;
            if (n == 0) { continue; }

            boolean isIntraday = bf.tf != Timeframe.DAILY && bf.tf != Timeframe.WEEKLY
                    && bf.tf != Timeframe.MONTHLY;

            // Detect issues
            boolean dirty = false;
            for (int i = 0; i < n; i++) {
                long ts = bars.getTimestamp(i);
                float o = bars.getOpen(i), h = bars.getHigh(i), l = bars.getLow(i), c = bars.getClose(i);
                if (i > 0 && ts <= bars.getTimestamp(i - 1)) { dirty = true; break; }
                if (o <= 0 || h <= 0 || l <= 0 || c <= 0)     { dirty = true; break; }
                if (h < l)                                      { dirty = true; break; }
                if (isIntraday) {
                    DayOfWeek dow = Instant.ofEpochMilli(ts).atZone(IST).getDayOfWeek();
                    if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) { dirty = true; break; }
                }
            }

            if (!dirty) { totalAfter += n; continue; }

            // Fix: funnel through TreeMap
            TreeMap<Long, float[]> clean = new TreeMap<>();
            int dD = 0, dH = 0, dN = 0, dW = 0;
            for (int i = 0; i < n; i++) {
                long ts = bars.getTimestamp(i);
                float o = bars.getOpen(i), h = bars.getHigh(i), l = bars.getLow(i), c = bars.getClose(i);
                long  v = bars.getVolume(i);
                if (o <= 0 || h <= 0 || l <= 0 || c <= 0) { dN++; continue; }
                if (h < l) { dH++; continue; }
                if (isIntraday) {
                    DayOfWeek dow = Instant.ofEpochMilli(ts).atZone(IST).getDayOfWeek();
                    if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) { dW++; continue; }
                }
                if (clean.containsKey(ts)) { dD++; if (v <= clean.get(ts)[4]) continue; }
                clean.put(ts, new float[]{o, h, l, c, v});
            }

            Bars fb = toBarsFromMap(bf.scripId, bf.tf, clean);
            repo.save(bf.scripId, bf.tf, fb);
            fixed++;
            dupes += dD; hlBad += dH; npBad += dN; wknd += dW;
            totalAfter += fb.size();

            log.info("  FIX {} {} : {} → {} (dupes={}, hl={}, np={}, wknd={})",
                    bf.scripId, bf.tf.getLabel(), n, fb.size(), dD, dH, dN, dW);
        }

        log.info("Phase 3 DONE — scanned={}, fixed={}", scanned, fixed);
        log.info("  dupes={}, hl={}, np={}, wknd={}", dupes, hlBad, npBad, wknd);
        log.info("  bars: {} → {} (removed {})", totalBefore, totalAfter, totalBefore - totalAfter);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Shared helpers
    // ═══════════════════════════════════════════════════════════════════════

    /** Returns false for bars with non-positive prices, H < L, or weekend timestamps (intraday). */
    private static boolean isValidBar(long ts, float o, float h, float l, float c, boolean isIntraday) {
        if (o <= 0 || h <= 0 || l <= 0 || c <= 0) return false;
        if (h < l) return false;
        if (isIntraday) {
            DayOfWeek dow = Instant.ofEpochMilli(ts).atZone(IST).getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) return false;
        }
        return true;
    }

    /** Convert a sorted TreeMap of bars into a Bars object. */
    private static Bars toBars(String scripId, Timeframe tf, TreeMap<Long, float[]> map) {
        Bars bars = new Bars(scripId, tf, map.size());
        for (Map.Entry<Long, float[]> e : map.entrySet()) {
            float[] v = e.getValue();
            bars.append(e.getKey(), v[0], v[1], v[2], v[3], (long) v[4]);
        }
        return bars;
    }

    /** Same as toBars but with a different name for clarity in the validation pass. */
    private static Bars toBarsFromMap(String scripId, Timeframe tf, TreeMap<Long, float[]> map) {
        return toBars(scripId, tf, map);
    }

    /**
     * Merge bars into the repository: if data already exists for this
     * (scripId, timeframe), prepend older bars and append newer bars.
     * The repository's built-in duplicate-skipping handles overlaps.
     * If no data exists, simply saves.
     */
    private static void mergeIntoRepo(FileBarsRepository repo, String scripId,
                                       Timeframe tf, Bars bars) throws IOException {
        if (!repo.exists(scripId, tf)) {
            repo.save(scripId, tf, bars);
        } else {
            repo.prepend(scripId, tf, bars, 0, bars.size());
            repo.append(scripId, tf, bars, 0, bars.size());
        }
    }

    /** Read a .bin file without memory-mapping (avoids Windows file lock). */
    private static Bars loadNoMmap(BinFile bf) throws IOException {
        long fileSize = Files.size(bf.path);
        if (fileSize <= BIN_HEADER) return new Bars(bf.scripId, bf.tf, 0);
        ByteBuffer buf = ByteBuffer.allocate((int) fileSize).order(ByteOrder.LITTLE_ENDIAN);
        try (FileChannel ch = FileChannel.open(bf.path, StandardOpenOption.READ)) {
            while (buf.hasRemaining()) ch.read(buf);
        }
        buf.flip();
        int count = (int) buf.getLong(8);
        Bars bars = new Bars(bf.scripId, bf.tf, count);
        for (int i = 0; i < count; i++) {
            int off = BIN_HEADER + i * BIN_BAR;
            bars.append(buf.getLong(off), buf.getFloat(off + 8), buf.getFloat(off + 12),
                    buf.getFloat(off + 16), buf.getFloat(off + 20), buf.getLong(off + 24));
        }
        return bars;
    }

    // ── Inner types ──────────────────────────────────────────────────────

    private static final class BinFile {
        final String scripId;
        final Timeframe tf;
        final Path path;
        BinFile(String scripId, Timeframe tf, Path path) {
            this.scripId = scripId; this.tf = tf; this.path = path;
        }
    }
}
