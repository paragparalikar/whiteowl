package com.whiteowl.core.backtest.driver;

import com.whiteowl.core.backtest.v2.engine.AtrSeries;
import com.whiteowl.core.backtest.v2.engine.Side;
import com.whiteowl.core.backtest.v2.model.TradeRecord;
import com.whiteowl.core.backtest.v2.optimization.analysis.BucketStats;
import com.whiteowl.core.backtest.v2.optimization.analysis.EntryFeatureCollector;
import com.whiteowl.core.backtest.v2.optimization.analysis.IndicatorSeries;
import com.whiteowl.core.backtest.v2.optimization.analysis.FilterAnalysis;
import com.whiteowl.core.backtest.v2.optimization.analysis.FilterAnalyzer;
import com.whiteowl.core.backtest.v2.optimization.analysis.TradeObservation;
import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.BarsArrays;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.FileBarsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Filter-analysis driver for externally produced trade lists (AmiBroker
 * "trade list" TSV export).
 *
 * <p>Pipeline:</p>
 * <ol>
 *   <li>Read the TSV file.</li>
 *   <li>Parse each row into a {@link TradeRecord} — the trade type used by
 *       the rest of this software.</li>
 *   <li>Generate entry-time features per trade: the canonical
 *       {@link EntryFeatureCollector} set plus a multi-period trend and
 *       volatility feature pack ({@link #computeFeatureSeries}), all
 *       sampled at {@code entryBarIndex - 1} — no look-ahead.</li>
 *   <li>Run {@link FilterAnalyzer} equal-width binning over each feature —
 *       the same Phase-5 analysis used by {@code ResearchPipeline}.</li>
 *   <li>Print, per feature, the bin table and the accepted value range that
 *       can be used as an entry filter.</li>
 * </ol>
 *
 * <h3>TSV format (AmiBroker trade list)</h3>
 * <pre>
 *   Symbol  Trade  Date  Price  Ex. date  Ex. Price  % chg  Profit  % Profit
 *   Shares  Position value  Cum. Profit  # bars  Profit/bar  MAE  MFE  Scale In/Out
 * </pre>
 * Timestamps are {@code dd-MM-yyyy HH:mm:ss} (IST). A {@code 00:00:00} time
 * means "at the day boundary": entries snap to the first bar of the date,
 * exits to the last bar of the date. Other timestamps resolve to the bar
 * that contains them.
 *
 * <h3>Usage</h3>
 * <pre>
 *   mvn compile exec:java -pl whiteowl-backtest \
 *       -Dexec.mainClass=com.whiteowl.core.backtest.driver.ImportedTradeFilterAnalysisDriver \
 *       -Dexec.args="C:\Users\parag\Desktop\banknifty.tsv 5min NSE"
 * </pre>
 * Args: {@code <tsvPath> <timeframe> [exchange=NSE]} — the timeframe must
 * match the bar interval the strategy traded on (inferred from the "# bars"
 * column: e.g. a 20-minute trade spanning 5 bars → 5min).
 * Bar data must exist under {@code ~/.whiteowl/data/bars/<exchange>/<symbol>/<timeframe>.bin}
 * (standard {@link FileBarsRepository} layout).
 */
public final class ImportedTradeFilterAnalysisDriver {

    private static final Logger log =
            LoggerFactory.getLogger(ImportedTradeFilterAnalysisDriver.class);

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final int MARKET_OPEN_MINUTES = 9 * 60 + 15;
    private static final DateTimeFormatter TS_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    private static final LocalTime MIDNIGHT = LocalTime.MIDNIGHT;

    // Indicator periods — identical to OptimizationEngine.evaluateDetailed.
    private static final int ATR_PERIOD = 14;
    private static final int RSI_PERIOD = 14;
    private static final int ADX_PERIOD = 14;
    private static final int ROC_PERIOD = 14;
    private static final int VOL_PERIOD = 20;

    // Binning parameters — identical to ResearchPipeline Phase 5.
    private static final int BIN_COUNT = 50;
    private static final int MIN_TRADES_PER_BIN = 5;
    private static final double IMPROVEMENT_THRESHOLD = 0.0;

    /** One row of the AmiBroker trade list. */
    private record ParsedTrade(String symbol, Side side,
                               LocalDateTime entryTime, float entryPrice,
                               LocalDateTime exitTime, float exitPrice,
                               float profit, int shares, float positionValue) {
    }

    public static void main(String[] args) throws IOException {
        Path tsv = Path.of("C:\\Users\\parag\\Desktop\\banknifty.tsv");
        Timeframe timeframe = Timeframe.FIFTEEN_MINUTE;
        String exchange = "NSE";

        // ── Steps 1+2: read & parse the trade list ─────────────────────
        List<ParsedTrade> parsed = parseTradeList(tsv);
        System.out.printf("Parsed %d trades from %s%n", parsed.size(), tsv);
        if (parsed.isEmpty()) {
            System.out.println("No trades parsed — nothing to analyze.");
            return;
        }

        // ── Step 3: load bars, convert to TradeRecord, generate
        //           entry-time features ───────────────────────────────
        FileBarsRepository repo = new FileBarsRepository();
        Map<String, BarsArrays> barsBySymbol = loadBars(repo, parsed,
                exchange, timeframe);
        Map<String, float[][]> seriesBySymbol = new LinkedHashMap<>();
        Map<String, Map<String, float[]>> extraSeriesBySymbol =
                new LinkedHashMap<>();
        for (Map.Entry<String, BarsArrays> e : barsBySymbol.entrySet()) {
            seriesBySymbol.put(e.getKey(), EntryFeatureCollector.computeSeries(
                    e.getValue(), ATR_PERIOD, RSI_PERIOD, ADX_PERIOD,
                    ROC_PERIOD, VOL_PERIOD));
            extraSeriesBySymbol.put(e.getKey(),
                    computeFeatureSeries(e.getValue()));
        }

        List<TradeRecord> trades = new ArrayList<>();
        List<TradeObservation> observations = new ArrayList<>();
        int skippedNoBars = 0, skippedUnmapped = 0;
        int positionId = 0;
        for (ParsedTrade p : parsed) {
            String scripId = scripId(exchange, p.symbol());
            BarsArrays bars = barsBySymbol.get(scripId);
            if (bars == null || bars.size() == 0) {
                skippedNoBars++;
                continue;
            }
            int entryBar = entryBarIndex(bars.timestamp(), p.entryTime());
            int exitBar = exitBarIndex(bars.timestamp(), p.exitTime());
            if (entryBar < 0 || exitBar < entryBar) {
                skippedUnmapped++;
                continue;
            }
            TradeRecord t = toRecord(++positionId, scripId, p, bars,
                    entryBar, exitBar);
            trades.add(t);
            observations.add(toObservation(t, seriesBySymbol.get(scripId),
                    extraSeriesBySymbol.get(scripId)));
        }
        System.out.printf("Mapped %d trades to %s bars"
                        + " (%d skipped: %d no-bar-data, %d unmapped"
                        + " timestamps)%n", trades.size(), timeframe.getLabel(),
                skippedNoBars + skippedUnmapped, skippedNoBars,
                skippedUnmapped);
        if (observations.isEmpty()) {
            System.out.printf("No trades could be mapped to %s bars — check"
                            + " that bar data covers the trade dates.%n",
                    timeframe.getLabel());
            return;
        }
        observations.sort(Comparator.comparingLong(
                TradeObservation::exitTimestamp));

        // ── Step 4: binning analysis ──────────────────────────────────
        List<FilterAnalyzer.NamedFeature> features =
                new ArrayList<>(FilterAnalyzer.DEFAULT_FEATURES);
        features.add(new FilterAnalyzer.NamedFeature("entryAtr",
                TradeObservation::entryAtr));
        for (String name : extraSeriesBySymbol.values().iterator().next()
                .keySet()) {
            features.add(new FilterAnalyzer.NamedFeature(name, t -> {
                Float v = t.extras().get(name);
                return v == null ? Double.NaN : v;
            }));
        }
        features.add(new FilterAnalyzer.NamedFeature("minutesFromOpen",
                TradeObservation::minutesFromOpen));
        features.add(new FilterAnalyzer.NamedFeature("dayOfWeek",
                TradeObservation::dayOfWeek));
        features.add(new FilterAnalyzer.NamedFeature("weekOfMonth",
                TradeObservation::weekOfMonth));
        features.add(new FilterAnalyzer.NamedFeature("monthOfYear",
                TradeObservation::monthOfYear));

        FilterAnalyzer analyzer = new FilterAnalyzer(BIN_COUNT,
                MIN_TRADES_PER_BIN, IMPROVEMENT_THRESHOLD);
        List<FilterAnalysis> analyses = analyzer.analyze(observations, features);

        // ── Step 5: report usable filters ─────────────────────────────
        printReport(observations, analyses);
    }

    // ── Step 2: TSV parsing ──────────────────────────────────────────────

    private static List<ParsedTrade> parseTradeList(Path file)
            throws IOException {
        List<ParsedTrade> out = new ArrayList<>();
        int malformed = 0;
        for (String line : Files.readAllLines(file)) {
            if (line.isBlank()) continue;
            String[] c = line.split("\t", -1);
            if (c.length < 11 || "Symbol".equalsIgnoreCase(c[0].trim())) {
                continue; // header or short line
            }
            Side side = parseSide(c[1]);
            if (side == null) {
                malformed++;
                continue;
            }
            try {
                out.add(new ParsedTrade(
                        c[0].trim(),
                        side,
                        LocalDateTime.parse(c[2].trim(), TS_FORMAT),
                        num(c[3]),
                        LocalDateTime.parse(c[4].trim(), TS_FORMAT),
                        num(c[5]),
                        num(c[7]),
                        (int) num(c[9]),
                        num(c[10])));
            } catch (RuntimeException ex) {
                malformed++;
                log.debug("Skipping malformed line: {}", line, ex);
            }
        }
        if (malformed > 0) {
            log.warn("Skipped {} malformed/unsupported trade rows", malformed);
        }
        return out;
    }

    private static Side parseSide(String tradeColumn) {
        String s = tradeColumn.trim();
        if (s.startsWith("Long")) return Side.LONG;
        if (s.startsWith("Short")) return Side.SHORT;
        return null;
    }

    private static float num(String s) {
        return Float.parseFloat(s.replace("%", "").replace(",", "").trim());
    }

    // ── Step 3: bar lookup & feature generation ──────────────────────────

    private static Map<String, BarsArrays> loadBars(FileBarsRepository repo,
            List<ParsedTrade> trades, String exchange, Timeframe tf) {
        Map<String, BarsArrays> out = new LinkedHashMap<>();
        for (ParsedTrade p : trades) {
            String scripId = scripId(exchange, p.symbol());
            if (out.containsKey(scripId)) continue;
            try {
                Bars bars = repo.load(scripId, tf);
                System.out.printf("Loaded %d %s-bars for %s%n", bars.size(),
                        tf.getLabel(), scripId);
                out.put(scripId, bars.arrays());
            } catch (IOException e) {
                log.warn("Failed to load {} bars for {}: {}", tf.getLabel(),
                        scripId, e.getMessage());
            }
        }
        return out;
    }

    private static String scripId(String exchange, String symbol) {
        return symbol.contains(":") ? symbol : exchange + ":" + symbol;
    }

    private static Timeframe parseTimeframe(String s) {
        for (Timeframe tf : Timeframe.values()) {
            if (tf.getLabel().equalsIgnoreCase(s)
                    || tf.getCode().equalsIgnoreCase(s)) {
                return tf;
            }
        }
        throw new IllegalArgumentException("Unknown timeframe '" + s
                + "' — valid labels: 1min,2min,3min,5min,10min,15min,30min,"
                + "1h,2h,3h,daily,weekly,monthly");
    }

    /** Entry timestamp → entry bar. 00:00:00 = entry at the day's open. */
    private static int entryBarIndex(long[] ts, LocalDateTime entry) {
        if (entry.toLocalTime().equals(MIDNIGHT)) {
            return firstBarOnDate(ts, entry.toLocalDate());
        }
        return containingBar(ts, epochMilli(entry));
    }

    /** Exit timestamp → exit bar. 00:00:00 = exit at the day's close. */
    private static int exitBarIndex(long[] ts, LocalDateTime exit) {
        if (exit.toLocalTime().equals(MIDNIGHT)) {
            return lastBarOnDate(ts, exit.toLocalDate());
        }
        return containingBar(ts, epochMilli(exit));
    }

    /** Index of the bar whose interval contains {@code t}, same-date only. */
    private static int containingBar(long[] ts, long t) {
        int i = Arrays.binarySearch(ts, t);
        int idx = i >= 0 ? i : (-i - 1) - 1;
        return idx >= 0 && sameDate(ts[idx], t) ? idx : -1;
    }

    private static int firstBarOnDate(long[] ts, LocalDate date) {
        long dayStart = epochMilli(date.atStartOfDay());
        int i = Arrays.binarySearch(ts, dayStart);
        int idx = i >= 0 ? i : -i - 1;
        return idx < ts.length && sameDate(ts[idx], dayStart) ? idx : -1;
    }

    private static int lastBarOnDate(long[] ts, LocalDate date) {
        long nextDay = epochMilli(date.plusDays(1).atStartOfDay());
        int i = Arrays.binarySearch(ts, nextDay);
        int idx = (i >= 0 ? i : -i - 1) - 1;
        return idx >= 0 && sameDate(ts[idx], epochMilli(date.atStartOfDay()))
                ? idx : -1;
    }

    private static boolean sameDate(long epochA, long epochB) {
        return Instant.ofEpochMilli(epochA).atZone(IST).toLocalDate()
                .equals(Instant.ofEpochMilli(epochB).atZone(IST).toLocalDate());
    }

    private static long epochMilli(LocalDateTime dt) {
        return dt.atZone(IST).toInstant().toEpochMilli();
    }

    private static TradeRecord toRecord(int positionId, String scripId,
                                        ParsedTrade p, BarsArrays bars,
                                        int entryBar, int exitBar) {
        float entryValue = p.entryPrice() * p.shares();
        return TradeRecord.builder()
                .positionId(positionId)
                .scripId(scripId)
                .side(p.side())
                .entryPrice(p.entryPrice())
                .exitPrice(p.exitPrice())
                .entryTimestamp(bars.timestamp()[entryBar])
                .exitTimestamp(bars.timestamp()[exitBar])
                .quantity(p.shares())
                .grossPnl(p.profit())
                .totalCost(0f)
                .netPnl(p.profit())
                .netPnlPercent(entryValue > 0
                        ? p.profit() / entryValue * 100f : 0f)
                .entryBarIndex(entryBar)
                .exitBarIndex(exitBar)
                .holdingBars(exitBar - entryBar + 1)
                .build();
    }

    /**
     * Multi-period trend and volatility feature series, complementing the
     * canonical {@link EntryFeatureCollector} set. Periods deliberately
     * bracket the defaults (14/20) so binning can reveal which lookback
     * actually discriminates. Every entry is a full-length series aligned
     * to the bar index.
     */
    private static Map<String, float[]> computeFeatureSeries(BarsArrays bars) {
        Map<String, float[]> m = new LinkedHashMap<>();
        float[] c = bars.close();
        float[] ma20 = IndicatorSeries.sma(c, 20);
        float[] ma50 = IndicatorSeries.sma(c, 50);

        // trend
        m.put("adx7", IndicatorSeries.adx(bars, 7));
        m.put("adx21", IndicatorSeries.adx(bars, 21));
        m.put("adx28", IndicatorSeries.adx(bars, 28));
        m.put("rsi7", IndicatorSeries.rsi(c, 7));
        m.put("rsi21", IndicatorSeries.rsi(c, 21));
        m.put("roc5", IndicatorSeries.roc(c, 5));
        m.put("roc10", IndicatorSeries.roc(c, 10));
        m.put("roc20", IndicatorSeries.roc(c, 20));
        m.put("closeVsSma20", pctDiff(c, ma20));
        m.put("closeVsSma50", pctDiff(c, ma50));
        m.put("smaSlope20x5", IndicatorSeries.roc(ma20, 5));

        // volatility
        m.put("atrOverClose14", ratio(AtrSeries.compute(bars, 14), c));
        m.put("atrOverMa10", ratio(AtrSeries.compute(bars, 10), ma20));
        m.put("atrOverMa20", ratio(AtrSeries.compute(bars, 20), ma20));
        m.put("volStdDevOverMa10",
                ratio(IndicatorSeries.stdDev(c, 10),
                        IndicatorSeries.sma(c, 10)));
        m.put("volStdDevOverMa50",
                ratio(IndicatorSeries.stdDev(c, 50), ma50));
        return m;
    }

    /** Elementwise {@code (a - b) / b * 100}; NaN where b is NaN or 0. */
    private static float[] pctDiff(float[] a, float[] b) {
        float[] out = new float[a.length];
        for (int i = 0; i < a.length; i++) {
            out[i] = !Float.isNaN(b[i]) && b[i] != 0
                    ? (a[i] - b[i]) / b[i] * 100f : Float.NaN;
        }
        return out;
    }

    /** Elementwise {@code a / b}; NaN where b is NaN or 0. */
    private static float[] ratio(float[] a, float[] b) {
        float[] out = new float[a.length];
        for (int i = 0; i < a.length; i++) {
            out[i] = !Float.isNaN(b[i]) && b[i] != 0
                    ? a[i] / b[i] : Float.NaN;
        }
        return out;
    }

    private static float at(float[] s, int i) {
        return i >= 0 && i < s.length ? s[i] : Float.NaN;
    }

    private static TradeObservation toObservation(TradeRecord t, float[][] s,
            Map<String, float[]> extraSeries) {
        int signalBar = Math.max(0, t.getEntryBarIndex() - 1);
        float atr = s[5][signalBar];
        Map<String, Float> extras = new HashMap<>();
        for (Map.Entry<String, float[]> e : extraSeries.entrySet()) {
            extras.put(e.getKey(), at(e.getValue(), signalBar));
        }

        Instant entry = Instant.ofEpochMilli(t.getEntryTimestamp());
        LocalTime tod = entry.atZone(IST).toLocalTime();
        LocalDate date = entry.atZone(IST).toLocalDate();
        return new TradeObservation(
                t.getPositionId(), t.getScripId(), t.getSide(),
                t.getEntryBarIndex(), t.getExitBarIndex(),
                t.getEntryTimestamp(), t.getExitTimestamp(), t.getHoldingBars(),
                t.getNetPnl(), t.getNetPnlPercent(), Float.NaN,
                s[0][signalBar], s[1][signalBar], s[2][signalBar],
                s[3][signalBar], s[4][signalBar], atr,
                tod.get(ChronoField.MINUTE_OF_DAY) - MARKET_OPEN_MINUTES,
                date.getDayOfWeek().getValue(),
                (date.getDayOfMonth() - 1) / 7 + 1,
                date.getMonthValue(),
                extras);
    }

    // ── Step 5: report ───────────────────────────────────────────────────

    private static void printReport(List<TradeObservation> obs,
                                    List<FilterAnalysis> analyses) {
        analyses.sort(Comparator.comparingDouble(o -> o.improvement()));
        analyses.reversed();
        BucketStats baseline = BucketStats.of(Double.NaN, Double.NaN, "ALL",
                obs);
        System.out.println();
        System.out.println("════════════════ BASELINE (all trades) ════════════════");
        printBucket(baseline, "  ALL");
        System.out.println();

        for (FilterAnalysis fa : analyses) {
            System.out.printf("═══ %s ═══%n", fa.feature());
            for (BucketStats b : fa.bins()) {
                if (b.tradeCount() == 0) continue;
                boolean inRegion = fa.accepted()
                        && b.lowerBound() >= fa.regionLower() - 1e-9
                        && b.upperBound() <= fa.regionUpper() + 1e-9;
                printBucket(b, inRegion ? "*" : " ");
            }
            System.out.printf("  → %s — %s%n%n",
                    fa.accepted() ? "ACCEPTED" : "rejected", fa.reason());
        }

        System.out.println("════════════════ SUGGESTED FILTERS ═══════════════════");
        boolean any = false;
        for (FilterAnalysis fa : analyses) {
            if (!fa.accepted()) continue;
            any = true;
            System.out.printf("  keep trades where %-16s ∈ [%.4g, %.4g)"
                            + "   (Sortino %+.3f over baseline %.3f)%n",
                    fa.feature(), fa.regionLower(), fa.regionUpper(),
                    fa.improvement(), baseline.sortino());
        }
        if (!any) {
            System.out.println("  none — no feature showed a stable,"
                    + " improving region over baseline.");
        }
    }

    private static void printBucket(BucketStats b, String marker) {
        String range = b.label() != null ? b.label()
                : String.format("[%10.4g, %10.4g)", b.lowerBound(),
                        b.upperBound());
        System.out.printf("%s %-24s n=%-4d win=%5.1f%%  avg=%+8.3f%%"
                        + "  avgR=%+7.2f  sortino=%+8.3f  pf=%6.2f%n",
                marker, range, b.tradeCount(), b.winRate(),
                b.avgPnlPercent(), b.avgR(), b.sortino(), b.profitFactor());
    }

}
