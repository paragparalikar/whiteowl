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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Builds higher-timeframe bars from 1-minute data for every symbol in the
 * repository. Merges with any existing higher-TF data (e.g. Kite downloads)
 * so that those precise values are preserved and only missing periods are filled.
 *
 * <h3>Target timeframes (from 1min)</h3>
 * 3min, 5min, 10min, 15min, 30min, 1h, daily
 *
 * <h3>Bucket alignment</h3>
 * Intraday buckets are aligned to NSE market open (09:15 IST), matching
 * Kite/Zerodha conventions (e.g. 1h candles: 09:15, 10:15, 11:15 …).
 * Daily bars use midnight IST.
 *
 * <p>Run with:
 * <pre>
 *   mvn -pl whiteowl-workbench exec:java \
 *       -Dexec.mainClass="com.whiteowl.workbench.tools.TimeframeAggregator"
 * </pre>
 */
@Slf4j
public final class TimeframeAggregator {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final int MARKET_OPEN_MINUTES = 9 * 60 + 15; // 09:15

    private static final int BIN_HEADER = 16;
    private static final int BIN_BAR    = 32;

    /** Higher timeframes to construct from 1min data. */
    private static final Timeframe[] TARGETS = {
            Timeframe.THREE_MINUTE,
            Timeframe.FIVE_MINUTE,
            Timeframe.TEN_MINUTE,
            Timeframe.FIFTEEN_MINUTE,
            Timeframe.THIRTY_MINUTE,
            Timeframe.ONE_HOUR,
            Timeframe.DAILY
    };

    // ── Entry point ──────────────────────────────────────────────────────

    public static void main(String[] args) throws Exception {
        FileBarsRepository repo = new FileBarsRepository();
        new TimeframeAggregator().run(repo);
    }

    // ── Orchestrator ─────────────────────────────────────────────────────

    public void run(FileBarsRepository repo) throws Exception {
        Path barsBase = Paths.get(
                System.getProperty("whiteowl.home",
                        System.getProperty("user.home") + File.separator + ".whiteowl"),
                "data", "bars");

        log.info("════════════════════════════════════════════════════════════");
        log.info("  Timeframe Aggregator — START");
        log.info("════════════════════════════════════════════════════════════");

        // Discover all 1min.bin files
        List<SymbolFile> symbols = discover1MinFiles(barsBase);
        log.info("Found {} symbols with 1min data", symbols.size());

        int total = symbols.size();
        int done = 0;
        int symbolsAugmented = 0;
        long totalNewBars = 0;

        for (SymbolFile sf : symbols) {
            done++;
            long newBars = processSymbol(sf, repo);
            if (newBars > 0) {
                symbolsAugmented++;
                totalNewBars += newBars;
            }

            if (done % 200 == 0 || done == total) {
                log.info("[{}/{}] augmented={}, newBars={}",
                        done, total, symbolsAugmented, totalNewBars);
            }
        }

        log.info("════════════════════════════════════════════════════════════");
        log.info("  Aggregation COMPLETE");
        log.info("  Symbols scanned   : {}", total);
        log.info("  Symbols augmented : {}", symbolsAugmented);
        log.info("  New bars created  : {}", totalNewBars);
        log.info("════════════════════════════════════════════════════════════");
    }

    // ── Discovery ────────────────────────────────────────────────────────

    private List<SymbolFile> discover1MinFiles(Path barsBase) throws IOException {
        List<SymbolFile> result = new ArrayList<>();
        if (!Files.isDirectory(barsBase)) return result;

        // Walk: barsBase / {exchange} / {symbol} / 1min.bin
        try (Stream<Path> walk = Files.walk(barsBase, 3)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> "1min.bin".equals(p.getFileName().toString()))
                    .forEach(p -> {
                        Path symDir = p.getParent();
                        Path exDir  = symDir.getParent();
                        if (exDir == null || !exDir.getParent().equals(barsBase)) return;
                        String scripId = exDir.getFileName() + ":" + symDir.getFileName();
                        result.add(new SymbolFile(scripId, p));
                    });
        }
        Collections.sort(result, (a, b) -> a.scripId.compareTo(b.scripId));
        return result;
    }

    // ── Per-symbol processing ────────────────────────────────────────────

    private long processSymbol(SymbolFile sf, FileBarsRepository repo) {
        try {
            Bars oneMin = loadNoMmap(sf.scripId, Timeframe.ONE_MINUTE, sf.path);
            if (oneMin.size() < 2) return 0;

            long newBars = 0;

            for (Timeframe target : TARGETS) {
                long added = aggregateAndMerge(sf.scripId, oneMin, target, repo);
                newBars += added;
            }

            if (newBars > 0) {
                log.info("  {} — +{} bars across {} timeframes",
                        sf.scripId, newBars, TARGETS.length);
            }
            return newBars;

        } catch (IOException e) {
            log.error("Failed to process {}: {}", sf.scripId, e.getMessage());
            return 0;
        }
    }

    /**
     * Aggregate 1min bars into the target timeframe, then merge into the repo.
     * Returns the number of new bars actually added.
     */
    private long aggregateAndMerge(String scripId, Bars oneMin, Timeframe target,
                                    FileBarsRepository repo) throws IOException {
        // Aggregate
        TreeMap<Long, Agg> agg = aggregate(oneMin, target);
        if (agg.isEmpty()) return 0;

        // Build Bars
        Bars bars = new Bars(scripId, target, agg.size());
        for (Map.Entry<Long, Agg> e : agg.entrySet()) {
            Agg a = e.getValue();
            bars.append(e.getKey(), a.open, a.high, a.low, a.close, a.volume);
        }

        // Merge with existing
        if (!repo.exists(scripId, target)) {
            repo.save(scripId, target, bars);
            return bars.size();
        }

        int before = repo.countBars(scripId, target);
        repo.prepend(scripId, target, bars, 0, bars.size());
        repo.append(scripId, target, bars, 0, bars.size());
        int after = repo.countBars(scripId, target);
        return after - before;
    }

    // ── Aggregation engine ───────────────────────────────────────────────

    private TreeMap<Long, Agg> aggregate(Bars oneMin, Timeframe target) {
        TreeMap<Long, Agg> result = new TreeMap<>();
        boolean isDaily = target == Timeframe.DAILY;
        int tfMinutes = (int) (target.getSeconds() / 60);

        for (int i = 0; i < oneMin.size(); i++) {
            long ts = oneMin.getTimestamp(i);
            float o = oneMin.getOpen(i);
            float h = oneMin.getHigh(i);
            float l = oneMin.getLow(i);
            float c = oneMin.getClose(i);
            long  v = oneMin.getVolume(i);

            long bucket = isDaily ? dailyBucket(ts) : intradayBucket(ts, tfMinutes);

            Agg a = result.get(bucket);
            if (a == null) {
                result.put(bucket, new Agg(o, h, l, c, v));
            } else {
                if (h > a.high) a.high = h;
                if (l < a.low)  a.low  = l;
                a.close  = c;
                a.volume += v;
            }
        }
        return result;
    }

    /**
     * Compute intraday bucket timestamp, aligned to market open (09:15 IST).
     * E.g. for 1h: 09:15, 10:15, 11:15, 12:15, 13:15, 14:15.
     */
    private long intradayBucket(long epochMillis, int tfMinutes) {
        LocalDateTime dt = Instant.ofEpochMilli(epochMillis).atZone(IST).toLocalDateTime();
        LocalDate date = dt.toLocalDate();
        int minuteOfDay = dt.getHour() * 60 + dt.getMinute();

        int sinceOpen = minuteOfDay - MARKET_OPEN_MINUTES;
        if (sinceOpen < 0) sinceOpen = 0; // pre-market → first bucket

        int bucketMinutes = (sinceOpen / tfMinutes) * tfMinutes;
        int bucketMinuteOfDay = MARKET_OPEN_MINUTES + bucketMinutes;

        LocalDateTime bucketDt = date.atTime(bucketMinuteOfDay / 60, bucketMinuteOfDay % 60);
        return bucketDt.atZone(IST).toInstant().toEpochMilli();
    }

    /** Daily bucket = midnight IST of the bar's date. */
    private long dailyBucket(long epochMillis) {
        LocalDate date = Instant.ofEpochMilli(epochMillis).atZone(IST).toLocalDate();
        return date.atStartOfDay(IST).toInstant().toEpochMilli();
    }

    // ── Binary reader (no mmap) ──────────────────────────────────────────

    private Bars loadNoMmap(String scripId, Timeframe tf, Path path) throws IOException {
        long fileSize = Files.size(path);
        if (fileSize <= BIN_HEADER) return new Bars(scripId, tf, 0);

        ByteBuffer buf = ByteBuffer.allocate((int) fileSize).order(ByteOrder.LITTLE_ENDIAN);
        try (FileChannel ch = FileChannel.open(path, StandardOpenOption.READ)) {
            while (buf.hasRemaining()) ch.read(buf);
        }
        buf.flip();

        int count = (int) buf.getLong(8);
        Bars bars = new Bars(scripId, tf, count);
        for (int i = 0; i < count; i++) {
            int off = BIN_HEADER + i * BIN_BAR;
            bars.append(buf.getLong(off), buf.getFloat(off + 8), buf.getFloat(off + 12),
                    buf.getFloat(off + 16), buf.getFloat(off + 20), buf.getLong(off + 24));
        }
        return bars;
    }

    // ── Inner types ──────────────────────────────────────────────────────

    private static final class SymbolFile {
        final String scripId;
        final Path path;
        SymbolFile(String scripId, Path path) {
            this.scripId = scripId;
            this.path = path;
        }
    }

    /** Mutable aggregation record for a single higher-TF bar. */
    private static final class Agg {
        final float open;
        float high, low, close;
        long volume;

        Agg(float open, float high, float low, float close, long volume) {
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
            this.volume = volume;
        }
    }
}
