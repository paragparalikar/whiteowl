package com.whiteowl.workbench.tools;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.FileBarsRepository;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Scans all bar data in the WhiteOwl repository for integrity issues introduced
 * during the three-source merge, then fixes them in place.
 *
 * <h3>Checks performed</h3>
 * <ol>
 *   <li><b>Out-of-order timestamps</b> — bars not in strictly ascending order</li>
 *   <li><b>Duplicate timestamps</b> — two or more bars at the same millisecond</li>
 *   <li><b>High &lt; Low</b> — OHLC constraint violation</li>
 *   <li><b>Non-positive prices</b> — O/H/L/C ≤ 0</li>
 *   <li><b>Weekend bars</b> — Saturday/Sunday (IST) bars for intraday timeframes</li>
 * </ol>
 *
 * <h3>Fix strategy</h3>
 * <ul>
 *   <li>All bars are funnelled into a {@code TreeMap} keyed by timestamp
 *       — this sorts and deduplicates in one pass.</li>
 *   <li>When two bars collide on the same timestamp, the one with higher volume
 *       wins; if volumes are equal the later source wins (last-write-wins).</li>
 *   <li>Bars with non-positive prices or H &lt; L are dropped.</li>
 *   <li>Weekend bars (intraday only) are dropped.</li>
 *   <li>The cleaned data is saved back only if it differs from the original.</li>
 * </ul>
 *
 * <p>Run with:
 * <pre>
 *   mvn -pl whiteowl-workbench exec:java \
 *       -Dexec.mainClass="com.whiteowl.workbench.tools.BarDataValidator"
 * </pre>
 */
@Slf4j
public final class BarDataValidator {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    /** Map from Timeframe.label → Timeframe enum. */
    private static final Map<String, Timeframe> LABEL_TO_TF = buildLabelMap();

    private static Map<String, Timeframe> buildLabelMap() {
        Map<String, Timeframe> m = new LinkedHashMap<>();
        for (Timeframe tf : Timeframe.values()) {
            m.put(tf.getLabel(), tf);
        }
        return m;
    }

    // ── Counters ─────────────────────────────────────────────────────────

    private static final int HEADER_SIZE = 16;
    private static final int BAR_SIZE = 32;

    private int filesScanned;
    private int filesFixed;
    private int filesFailed;
    private long duplicatesRemoved;
    private long outOfOrderFixed;
    private long hlViolationsRemoved;
    private long nonPositiveRemoved;
    private long weekendBarsRemoved;
    private long totalBarsBeforeFix;
    private long totalBarsAfterFix;

    // ── Entry point ──────────────────────────────────────────────────────

    public static void main(String[] args) throws Exception {
        FileBarsRepository repo = new FileBarsRepository();
        new BarDataValidator().run(repo);
    }

    // ── Orchestrator ─────────────────────────────────────────────────────

    public void run(FileBarsRepository repo) throws Exception {
        Path baseDir = Paths.get(
                System.getProperty("whiteowl.home",
                        System.getProperty("user.home") + File.separator + ".whiteowl"),
                "data", "bars");

        log.info("════════════════════════════════════════════════════════════");
        log.info("  Bar Data Validator — START");
        log.info("  Base dir : {}", baseDir);
        log.info("════════════════════════════════════════════════════════════");

        // Discover all .bin files → (scripId, timeframe)
        List<BarFile> barFiles = discoverBarFiles(baseDir);
        log.info("Found {} bar files to validate", barFiles.size());

        int total = barFiles.size();
        int progressStep = Math.max(1, total / 20); // ~5% steps

        for (int i = 0; i < total; i++) {
            BarFile bf = barFiles.get(i);
            validateAndFix(bf, repo);

            if ((i + 1) % progressStep == 0 || i + 1 == total) {
                log.info("[{}/{}] scanned={}, fixed={}, dupes={}, oo={}, hl={}, np={}, wknd={}",
                        i + 1, total, filesScanned, filesFixed,
                        duplicatesRemoved, outOfOrderFixed,
                        hlViolationsRemoved, nonPositiveRemoved, weekendBarsRemoved);
            }
        }

        log.info("════════════════════════════════════════════════════════════");
        log.info("  Validation COMPLETE");
        log.info("  Files scanned       : {}", filesScanned);
        log.info("  Files fixed         : {}", filesFixed);
        log.info("  Duplicates removed  : {}", duplicatesRemoved);
        log.info("  Out-of-order fixed  : {}", outOfOrderFixed);
        log.info("  H<L violations      : {}", hlViolationsRemoved);
        log.info("  Non-positive prices : {}", nonPositiveRemoved);
        log.info("  Weekend bars        : {}", weekendBarsRemoved);
        log.info("  Files failed (lock) : {}", filesFailed);
        log.info("  Bars before fix     : {}", totalBarsBeforeFix);
        log.info("  Bars after fix      : {}", totalBarsAfterFix);
        log.info("  Bars removed        : {}", totalBarsBeforeFix - totalBarsAfterFix);
        log.info("════════════════════════════════════════════════════════════");
    }

    // ── File discovery ───────────────────────────────────────────────────

    private List<BarFile> discoverBarFiles(Path baseDir) throws IOException {
        List<BarFile> result = new ArrayList<>();

        if (!Files.isDirectory(baseDir)) {
            log.error("Base directory not found: {}", baseDir);
            return result;
        }

        // Walk: baseDir / {exchange} / {symbol} / {label}.bin
        try (Stream<Path> walk = Files.walk(baseDir, 3)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".bin"))
                    .forEach(p -> {
                        String label = p.getFileName().toString().replace(".bin", "");
                        Timeframe tf = LABEL_TO_TF.get(label);
                        if (tf == null) return; // skip unrecognized

                        // Reconstruct scripId from path segments: .../NSE/NIFTY 50/1min.bin
                        Path symbolDir = p.getParent();
                        Path exchangeDir = symbolDir.getParent();
                        if (exchangeDir == null || !exchangeDir.getParent().equals(baseDir)) return;

                        String exchange = exchangeDir.getFileName().toString();
                        String symbol = symbolDir.getFileName().toString();
                        String scripId = exchange + ":" + symbol;

                        result.add(new BarFile(scripId, tf, p));
                    });
        }

        result.sort((a, b) -> {
            int cmp = a.scripId.compareTo(b.scripId);
            return cmp != 0 ? cmp : a.timeframe.compareTo(b.timeframe);
        });
        return result;
    }

    // ── Per-file validation & fix ────────────────────────────────────────

    /**
     * Read bars directly using heap ByteBuffer (not mmap) to avoid
     * Windows file lock issues when we later overwrite the same file.
     */
    private Bars loadBarsNoMmap(BarFile bf) throws IOException {
        Path path = bf.path;
        long fileSize = Files.size(path);
        if (fileSize <= HEADER_SIZE) return new Bars(bf.scripId, bf.timeframe, 0);

        ByteBuffer buffer = ByteBuffer.allocate((int) fileSize).order(ByteOrder.LITTLE_ENDIAN);
        try (FileChannel ch = FileChannel.open(path, StandardOpenOption.READ)) {
            while (buffer.hasRemaining()) ch.read(buffer);
        }
        buffer.flip();

        int barCount = (int) buffer.getLong(8); // header offset 8 = bar count
        Bars bars = new Bars(bf.scripId, bf.timeframe, barCount);
        for (int i = 0; i < barCount; i++) {
            int off = HEADER_SIZE + i * BAR_SIZE;
            bars.append(
                    buffer.getLong(off),        // timestamp
                    buffer.getFloat(off + 8),   // open
                    buffer.getFloat(off + 12),  // high
                    buffer.getFloat(off + 16),  // low
                    buffer.getFloat(off + 20),  // close
                    buffer.getLong(off + 24));   // volume
        }
        return bars;
    }

    private void validateAndFix(BarFile bf, FileBarsRepository repo) {
        filesScanned++;

        try {
            Bars bars = loadBarsNoMmap(bf);
            int originalSize = bars.size();
            totalBarsBeforeFix += originalSize;

            if (originalSize == 0) {
                totalBarsAfterFix += 0;
                return;
            }

            // ── Detection pass ──────────────────────────────────────
            int dupes = 0;
            int outOfOrder = 0;
            int hlViolations = 0;
            int nonPositive = 0;
            int weekend = 0;

            boolean isIntraday = bf.timeframe != Timeframe.DAILY
                    && bf.timeframe != Timeframe.WEEKLY
                    && bf.timeframe != Timeframe.MONTHLY;

            for (int i = 0; i < originalSize; i++) {
                long ts = bars.getTimestamp(i);
                float o = bars.getOpen(i);
                float h = bars.getHigh(i);
                float l = bars.getLow(i);
                float c = bars.getClose(i);

                // Duplicate / out-of-order
                if (i > 0) {
                    long prev = bars.getTimestamp(i - 1);
                    if (ts == prev) dupes++;
                    if (ts < prev) outOfOrder++;
                }

                // OHLC checks
                if (o <= 0 || h <= 0 || l <= 0 || c <= 0) nonPositive++;
                if (h < l) hlViolations++;

                // Weekend
                if (isIntraday) {
                    DayOfWeek dow = Instant.ofEpochMilli(ts)
                            .atZone(IST).getDayOfWeek();
                    if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) weekend++;
                }
            }

            boolean needsFix = (dupes + outOfOrder + hlViolations + nonPositive + weekend) > 0;

            if (!needsFix) {
                totalBarsAfterFix += originalSize;
                return;
            }

            // ── Log issues ──────────────────────────────────────────
            log.warn("ISSUES in {} {} ({}bars): dupes={}, oo={}, hl={}, np={}, wknd={}",
                    bf.scripId, bf.timeframe.getLabel(), originalSize,
                    dupes, outOfOrder, hlViolations, nonPositive, weekend);

            // ── Fix pass — funnel through TreeMap ────────────────────
            TreeMap<Long, OhlcvRecord> clean = new TreeMap<>();
            int droppedHl = 0;
            int droppedNp = 0;
            int droppedWknd = 0;
            int droppedDupe = 0;

            for (int i = 0; i < originalSize; i++) {
                long ts = bars.getTimestamp(i);
                float o = bars.getOpen(i);
                float h = bars.getHigh(i);
                float l = bars.getLow(i);
                float c = bars.getClose(i);
                long v = bars.getVolume(i);

                // Drop non-positive
                if (o <= 0 || h <= 0 || l <= 0 || c <= 0) {
                    droppedNp++;
                    continue;
                }

                // Drop H < L
                if (h < l) {
                    droppedHl++;
                    continue;
                }

                // Drop weekend bars for intraday
                if (isIntraday) {
                    DayOfWeek dow = Instant.ofEpochMilli(ts).atZone(IST).getDayOfWeek();
                    if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                        droppedWknd++;
                        continue;
                    }
                }

                // TreeMap insert: deduplicates by timestamp
                OhlcvRecord existing = clean.get(ts);
                if (existing != null) {
                    droppedDupe++;
                    // Keep the bar with higher volume (more data)
                    if (v > existing.volume) {
                        clean.put(ts, new OhlcvRecord(o, h, l, c, v));
                    }
                    // else keep existing
                } else {
                    clean.put(ts, new OhlcvRecord(o, h, l, c, v));
                }
            }

            // ── Rebuild and save ─────────────────────────────────────
            Bars fixed = new Bars(bf.scripId, bf.timeframe, clean.size());
            for (Map.Entry<Long, OhlcvRecord> e : clean.entrySet()) {
                OhlcvRecord r = e.getValue();
                fixed.append(e.getKey(), r.open, r.high, r.low, r.close, r.volume);
            }

            repo.save(bf.scripId, bf.timeframe, fixed);

            log.info("  FIXED {} {} : {} → {} bars (dropped: dupes={}, hl={}, np={}, wknd={})",
                    bf.scripId, bf.timeframe.getLabel(),
                    originalSize, fixed.size(),
                    droppedDupe, droppedHl, droppedNp, droppedWknd);

            duplicatesRemoved += droppedDupe;
            outOfOrderFixed += outOfOrder;
            hlViolationsRemoved += droppedHl;
            nonPositiveRemoved += droppedNp;
            weekendBarsRemoved += droppedWknd;
            totalBarsAfterFix += fixed.size();
            filesFixed++;

        } catch (IOException e) {
            log.error("Failed to process {} {}: {}", bf.scripId, bf.timeframe.getLabel(), e.getMessage());
            filesFailed++;
            // Don't count as lost — file is unchanged on disk
        }
    }

    // ── Inner types ──────────────────────────────────────────────────────

    private static final class BarFile {
        final String scripId;
        final Timeframe timeframe;
        final Path path;

        BarFile(String scripId, Timeframe timeframe, Path path) {
            this.scripId = scripId;
            this.timeframe = timeframe;
            this.path = path;
        }
    }

    private static final class OhlcvRecord {
        final float open, high, low, close;
        final long volume;

        OhlcvRecord(float open, float high, float low, float close, long volume) {
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
            this.volume = volume;
        }
    }
}
